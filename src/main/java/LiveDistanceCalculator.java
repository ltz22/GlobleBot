import org.locationtech.jts.geom.*;

import java.io.IOException;
import java.util.*;

/**
 * Live distance calculator using GeoJSON multipolygon data.
 * Calculates distances on-the-fly instead of using pre-calculated matrix.
 */
public class LiveDistanceCalculator {

    private GeoJSONParser geoParser;
    private Map<String, Double> distanceCache;

    public LiveDistanceCalculator(String geoJsonFilePath) throws IOException {
        this.geoParser = new GeoJSONParser();
        this.distanceCache = new HashMap<>();

        System.out.println("Loading GeoJSON data...");
        geoParser.loadFromFile(geoJsonFilePath);
        System.out.println("Ready for distance calculations!");
    }

    /**
     * Get distance between two countries (with caching)
     */
    public int getDistance(String country1, String country2) {
        // Normalize names
        country1 = country1.trim();
        country2 = country2.trim();

        // Check cache (bidirectional)
        String key1 = country1 + "|" + country2;
        String key2 = country2 + "|" + country1;

        if (distanceCache.containsKey(key1)) {
            return distanceCache.get(key1).intValue();
        }
        if (distanceCache.containsKey(key2)) {
            return distanceCache.get(key2).intValue();
        }

        // Calculate distance
        double distance = geoParser.calculateDistance(country1, country2);

        if (distance < 0) {
            return -1; // Country not found
        }

        // Cache the result
        distanceCache.put(key1, distance);

        return (int) Math.round(distance);
    }

    /**
     * Find all countries within a distance range
     */
    public List<String> getCountriesInRange(String centerCountry, int minDistance, int maxDistance) {
        List<String> result = new ArrayList<>();
        Set<String> allCountries = geoParser.getAllCountryNames();

        for (String country : allCountries) {
            if (country.equalsIgnoreCase(centerCountry)) {
                continue;
            }

            int distance = getDistance(centerCountry, country);
            if (distance >= minDistance && distance <= maxDistance) {
                result.add(country);
            }
        }

        return result;
    }

    /**
     * Find N closest countries to a given country
     */
    public List<String> getClosestCountries(String centerCountry, int n, Set<String> exclude) {
        Set<String> allCountries = geoParser.getAllCountryNames();
        List<CountryDistance> distances = new ArrayList<>();

        for (String country : allCountries) {
            if (country.equalsIgnoreCase(centerCountry)) {
                continue;
            }
            if (exclude != null && exclude.contains(country.toLowerCase())) {
                continue;
            }

            int distance = getDistance(centerCountry, country);
            if (distance >= 0) {
                distances.add(new CountryDistance(country, distance));
            }
        }

        // Sort by distance
        distances.sort(Comparator.comparingInt(cd -> cd.distance));

        return distances.stream()
                .limit(n)
                .map(cd -> cd.country)
                .toList();
    }

    /**
     * Triangulate: find countries matching multiple distance constraints
     */
    public List<String> triangulate(Map<String, Integer> referencePoints, Set<String> exclude) {
        Set<String> allCountries = geoParser.getAllCountryNames();
        List<ScoredCountry> candidates = new ArrayList<>();

        for (String country : allCountries) {
            if (exclude != null && exclude.contains(country.toLowerCase())) {
                continue;
            }

            // Calculate error for this candidate
            double totalError = 0;
            boolean valid = true;

            for (Map.Entry<String, Integer> ref : referencePoints.entrySet()) {
                int actualDistance = getDistance(ref.getKey(), country);

                if (actualDistance < 0) {
                    valid = false;
                    break;
                }

                int targetDistance = ref.getValue();
                double error = Math.abs(actualDistance - targetDistance);

                // Weight the error (closer reference points are more important)
                if (targetDistance < 1000) {
                    error *= 2.0; // Double weight for close references
                }

                totalError += error;
            }

            if (valid) {
                candidates.add(new ScoredCountry(country, totalError));
            }
        }

        // Sort by lowest error
        candidates.sort(Comparator.comparingDouble(sc -> sc.score));

        return candidates.stream()
                .limit(10)
                .map(sc -> sc.country)
                .toList();
    }

    /**
     * Get all country names
     */
    public Set<String> getAllCountries() {
        return geoParser.getAllCountryNames();
    }

    /**
     * Generate and save complete distance matrix (optional, for speed)
     */
    public void generateDistanceMatrix(String outputPath) throws IOException {
        geoParser.generateDistanceMatrix(outputPath);
    }

    /**
     * Get cache statistics
     */
    public void printCacheStats() {
        System.out.println("Distance cache size: " + distanceCache.size() + " pairs");
    }

    // Helper classes
    private static class CountryDistance {
        String country;
        int distance;

        CountryDistance(String country, int distance) {
            this.country = country;
            this.distance = distance;
        }
    }

    private static class ScoredCountry {
        String country;
        double score;

        ScoredCountry(String country, double score) {
            this.country = country;
            this.score = score;
        }
    }
}
