public class GuessResult {
    String country;
    private String distance;

    public GuessResult(String country, String distance) {
        this.country = country;
        this.distance = distance;
    }

    public int getDistanceAsInt() {
        if (distance == null || distance.isEmpty()) {
            return -1; // Return -1 if distance not found (might mean you won!)
        }
        return Integer.parseInt(distance.replace(",", "").replace(":", "").trim());
    }

    @Override
    public String toString() {
        return String.format("Country: %s, Distance: %s",
                country, distance);
    }
}
