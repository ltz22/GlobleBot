import com.google.gson.*;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.distance.DistanceOp;

import java.io.*;
import java.util.*;

/**
 * Parses GeoJSON data and creates JTS geometries for distance calculations.
 * Handles MultiPolygon and Polygon types.
 */
public class GeoJSONParser {

    private Map<String, Geometry> countryGeometries;
    private GeometryFactory geometryFactory;

    public GeoJSONParser() {
        this.countryGeometries = new HashMap<>();
        this.geometryFactory = new GeometryFactory();
    }

    /**
     * Load countries from GeoJSON file
     */
    public void loadFromFile(String filePath) throws IOException {
        System.out.println("Loading GeoJSON from: " + filePath);

        // Read JSON file
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        reader.close();

        // Parse FeatureCollection
        if (root.has("type") && root.get("type").getAsString().equals("FeatureCollection")) {
            JsonArray features = root.getAsJsonArray("features");

            for (JsonElement featureElement : features) {
                JsonObject feature = featureElement.getAsJsonObject();

                // Get country name from properties
                JsonObject properties = feature.getAsJsonObject("properties");
                String countryName = getCountryName(properties);

                if (countryName == null) {
                    continue;
                }

                // Get geometry
                JsonObject geometry = feature.getAsJsonObject("geometry");
                Geometry geom = parseGeometry(geometry);

                if (geom != null) {
                    countryGeometries.put(countryName, geom);
                }
            }

            System.out.println("Loaded " + countryGeometries.size() + " countries");
        }
    }

    /**
     * Extract country name from properties (tries multiple fields)
     */
    private String getCountryName(JsonObject properties) {
        // Try common property names
        String[] nameFields = {"name", "NAME", "ADMIN", "admin", "NAME_LONG", "name_long"};

        for (String field : nameFields) {
            if (properties.has(field)) {
                return properties.get(field).getAsString();
            }
        }

        return null;
    }

    /**
     * Parse GeoJSON geometry into JTS Geometry
     */
    private Geometry parseGeometry(JsonObject geometryObj) {
        String type = geometryObj.get("type").getAsString();
        JsonArray coordinates = geometryObj.getAsJsonArray("coordinates");

        try {
            switch (type) {
                case "Polygon":
                    return parsePolygon(coordinates);
                case "MultiPolygon":
                    return parseMultiPolygon(coordinates);
                default:
                    System.out.println("Unsupported geometry type: " + type);
                    return null;
            }
        } catch (Exception e) {
            System.out.println("Error parsing geometry: " + e.getMessage());
            return null;
        }
    }

    /**
     * Parse a Polygon from coordinates array
     */
    private Polygon parsePolygon(JsonArray coordinates) {
        // First array is exterior ring, rest are holes
        JsonArray exteriorRing = coordinates.get(0).getAsJsonArray();
        Coordinate[] coords = parseCoordinateArray(exteriorRing);
        LinearRing shell = geometryFactory.createLinearRing(coords);

        // Parse holes if present
        LinearRing[] holes = new LinearRing[coordinates.size() - 1];
        for (int i = 1; i < coordinates.size(); i++) {
            JsonArray hole = coordinates.get(i).getAsJsonArray();
            holes[i - 1] = geometryFactory.createLinearRing(parseCoordinateArray(hole));
        }

        return geometryFactory.createPolygon(shell, holes);
    }

    /**
     * Parse a MultiPolygon from coordinates array
     */
    private MultiPolygon parseMultiPolygon(JsonArray coordinates) {
        Polygon[] polygons = new Polygon[coordinates.size()];

        for (int i = 0; i < coordinates.size(); i++) {
            JsonArray polygonCoords = coordinates.get(i).getAsJsonArray();
            polygons[i] = parsePolygon(polygonCoords);
        }

        return geometryFactory.createMultiPolygon(polygons);
    }

    /**
     * Parse coordinate array [[lon, lat], [lon, lat], ...]
     */
    private Coordinate[] parseCoordinateArray(JsonArray coordArray) {
        Coordinate[] coords = new Coordinate[coordArray.size()];

        for (int i = 0; i < coordArray.size(); i++) {
            JsonArray point = coordArray.get(i).getAsJsonArray();
            double lon = point.get(0).getAsDouble();
            double lat = point.get(1).getAsDouble();
            coords[i] = new Coordinate(lon, lat);
        }

        return coords;
    }

    /**
     * Get geometry for a country
     */
    public Geometry getCountryGeometry(String countryName) {
        // Try exact match first
        if (countryGeometries.containsKey(countryName)) {
            return countryGeometries.get(countryName);
        }

        // Try case-insensitive match
        for (Map.Entry<String, Geometry> entry : countryGeometries.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(countryName)) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Calculate distance between two countries (in kilometers)
     * This is the closest border distance
     */
    public double calculateDistance(String country1, String country2) {
        Geometry geom1 = getCountryGeometry(country1);
        Geometry geom2 = getCountryGeometry(country2);

        if (geom1 == null || geom2 == null) {
            System.out.println("Warning: Could not find geometry for " +
                    (geom1 == null ? country1 : country2));
            return -1;
        }

        // Use JTS to calculate minimum distance
        double distance = DistanceOp.distance(geom1, geom2);

        // Convert from degrees to kilometers
        // Approximate: 1 degree ≈ 111 km at equator
        return distance * 111.0;
    }

    /**
     * Get all country names
     */
    public Set<String> getAllCountryNames() {
        return new HashSet<>(countryGeometries.keySet());
    }

    /**
     * Pre-calculate all distances and save to CSV
     * (This can take a while for 200+ countries!)
     */
    public void generateDistanceMatrix(String outputCsvPath) throws IOException {
        System.out.println("Generating distance matrix...");
        System.out.println("This may take several minutes for 200+ countries");

        BufferedWriter writer = new BufferedWriter(new FileWriter(outputCsvPath));
        writer.write("Country1,Country2,Distance\n");

        List<String> countries = new ArrayList<>(countryGeometries.keySet());
        int total = countries.size();
        int count = 0;

        for (int i = 0; i < countries.size(); i++) {
            String country1 = countries.get(i);

            for (int j = i + 1; j < countries.size(); j++) {
                String country2 = countries.get(j);

                double distance = calculateDistance(country1, country2);

                // Write both directions
                writer.write(String.format("%s,%s,%.0f\n", country1, country2, distance));
                writer.write(String.format("%s,%s,%.0f\n", country2, country1, distance));

                count++;
                if (count % 100 == 0) {
                    System.out.printf("Progress: %d/%d pairs calculated\n",
                            count, (total * (total - 1)) / 2);
                }
            }
        }

        writer.close();
        System.out.println("Distance matrix saved to: " + outputCsvPath);
        System.out.println("Total pairs: " + count);
    }
}