import java.util.Scanner;

public class Main {

    /**
     * Main entry point for Globle bot using GeoJSON multipolygon data.
     */
    public static void main(String[] args) {
        GlobleBot bot = null;
        Scanner scanner = new Scanner(System.in);

        try {
            // Path to your GeoJSON file (extract from Globle website)
            String geoJsonPath = "C:\\Users\\zhang\\GlobleBot\\src\\main\\resources\\countries.geojson";

            // Load GeoJSON data
            LiveDistanceCalculator distanceCalc = new LiveDistanceCalculator(geoJsonPath);

            System.out.print("\nEnter your initial guess country: ");
            String initialGuess = scanner.nextLine().trim();

            System.out.println("\nStarting browser in 2 seconds...");
            Thread.sleep(2000);

            bot = new GlobleBot();
            bot.start();

            System.out.println("\nSwitch to the browser window.");
            System.out.println("Press Enter when ready to start...");
            scanner.nextLine();

            System.out.println("Starting in 5 seconds...");
            Thread.sleep(5000);

            // Run the algorithm
            BotAlgorithm algorithm = new BotAlgorithm(bot, distanceCalc);
            algorithm.solve(initialGuess);

            System.out.println("\n=== Finished ===");
            System.out.println("Press Enter to close browser...");
            scanner.nextLine();

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();

            if (e.getMessage() != null && e.getMessage().contains("countries.geojson")) {
                System.out.println("ERROR: GeoJSON file not found");
                /*
                System.out.println("Please extract the GeoJSON file from Globle:");
                System.out.println("1. Open https://globle-game.com");
                System.out.println("2. Press F12 (DevTools)");
                System.out.println("3. Go to Sources tab");
                System.out.println("4. Find countries.geojson or world.json");
                System.out.println("5. Save it to your project directory");
                 */
            }
        } finally {
            if (bot != null) {
                bot.close();
            }
            scanner.close();
        }
    }
}
