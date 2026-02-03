public class GuessResult {
    private String country;
    private String distance;
    private String color;

    public GuessResult(String country, String distance, String color) {
        this.country = country;
        this.distance = distance;
        this.color = color;
    }

    @Override
    public String toString() {
        return String.format("Country: %s, Distance: %s, Color: %s",
                country, distance, color);
    }
}
