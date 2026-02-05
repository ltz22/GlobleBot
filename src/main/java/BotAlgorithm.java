import java.util.*;

/**
 * Optimal Globle solver using live GeoJSON distance calculations.
 * No pre-calculated matrix needed!
 */
public class BotAlgorithm {

    private GlobleBot bot;
    private LiveDistanceCalculator distanceCalc;
    private List<GuessResult> guessHistory;
    private Set<String> guessedCountries;

    public BotAlgorithm(GlobleBot bot, LiveDistanceCalculator distanceCalc) {
        this.bot = bot;
        this.distanceCalc = distanceCalc;
        this.guessHistory = new ArrayList<>();
        this.guessedCountries = new HashSet<>();
    }

    public void solve(String initialGuess) {
        try {
            System.out.println("\n=== Starting GeoJSON-Based Globle Solver ===\n");

            // Make initial guess
            System.out.println("Guess #1: " + initialGuess);
            bot.makeGuess(initialGuess);
            GuessResult result = bot.getLastGuessResult();

            if (result != null) {
                guessHistory.add(result);
                guessedCountries.add(result.country.toLowerCase());
                System.out.println(result);

                if (isSolved(result)) {
                    System.out.println("\n🎉 SOLVED! Answer: " + result.country);
                    return;
                }
            }

            int guessCount = 1;
            int maxGuesses = 50;

            while (guessCount < maxGuesses) {
                String nextGuess = getNextOptimalGuess();

                if (nextGuess == null) {
                    System.out.println("\nNo more candidates");
                    break;
                }

                guessCount++;
                System.out.println("\nGuess #" + guessCount + ": " + nextGuess);

                Thread.sleep(1500);
                bot.makeGuess(nextGuess);
                result = bot.getLastGuessResult();

                if (result != null) {
                    guessHistory.add(result);
                    guessedCountries.add(result.country.toLowerCase());
                    System.out.println(result);

                    if (isSolved(result)) {
                        System.out.println("\n🎉 SOLVED in " + guessCount + " guesses!");
                        System.out.println("Answer: " + result.country);

                        // Print cache stats
                        distanceCalc.printCacheStats();
                        return;
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isSolved(GuessResult result) {
        return result.getDistanceAsInt() == 0 || result.getDistanceAsInt() == -1;
    }

    private String getNextOptimalGuess() {
        int numGuesses = guessHistory.size();

        if (numGuesses == 1) {
            return getStrategicSecondGuess();
        } else if (numGuesses == 2) {
            return triangulateFromTwoPoints();
        } else {
            return triangulateFromMultiplePoints();
        }
    }

    private String getStrategicSecondGuess() {
        GuessResult first = guessHistory.get(0);
        int firstDistance = first.getDistanceAsInt();

        System.out.println("Searching for strategic second guess...");

        if (firstDistance < 2000) {
            // Close guess - pick something far away
            return findCountryInDistanceRange(first.country, 8000, 12000);
        } else if (firstDistance < 6000) {
            // Medium - pick opposite hemisphere
            return findCountryInDistanceRange(first.country, 10000, 15000);
        } else {
            // Far - pick medium distance
            return findCountryInDistanceRange(first.country, 4000, 8000);
        }
    }

    private String triangulateFromTwoPoints() {
        GuessResult first = guessHistory.get(0);
        GuessResult second = guessHistory.get(1);

        System.out.println("Triangulating from 2 reference points...");

        Map<String, Integer> referencePoints = new HashMap<>();
        referencePoints.put(first.country, first.getDistanceAsInt());
        referencePoints.put(second.country, second.getDistanceAsInt());

        List<String> candidates = distanceCalc.triangulate(referencePoints, guessedCountries);

        if (!candidates.isEmpty()) {
            System.out.println("Top candidates: " +
                    candidates.subList(0, Math.min(5, candidates.size())));
            return candidates.get(0);
        }

        // Fallback
        GuessResult closest = getClosestGuess();
        return getCountryNearReference(closest.country, closest.getDistanceAsInt());
    }

    private String triangulateFromMultiplePoints() {
        System.out.println("Triangulating from " + guessHistory.size() + " reference points...");

        Map<String, Integer> referencePoints = new HashMap<>();
        for (GuessResult guess : guessHistory) {
            referencePoints.put(guess.country, guess.getDistanceAsInt());
        }

        List<String> candidates = distanceCalc.triangulate(referencePoints, guessedCountries);

        if (!candidates.isEmpty()) {
            System.out.println("Top candidates: " +
                    candidates.subList(0, Math.min(3, candidates.size())));
            return candidates.get(0);
        }

        // Fallback
        GuessResult closest = getClosestGuess();
        return getCountryNearReference(closest.country, closest.getDistanceAsInt());
    }

    private String findCountryInDistanceRange(String referenceCountry, int minDist, int maxDist) {
        List<String> candidates = distanceCalc.getCountriesInRange(
                referenceCountry, minDist, maxDist);

        candidates.removeIf(c -> guessedCountries.contains(c.toLowerCase()));

        if (!candidates.isEmpty()) {
            return pickBestCandidate(candidates);
        }

        return getAnyUnguessedCountry();
    }

    private String getCountryNearReference(String referenceCountry, int targetDistance) {
        int margin = Math.max(500, targetDistance / 4);
        List<String> candidates = distanceCalc.getCountriesInRange(
                referenceCountry,
                Math.max(0, targetDistance - margin),
                targetDistance + margin
        );

        candidates.removeIf(c -> guessedCountries.contains(c.toLowerCase()));

        if (!candidates.isEmpty()) {
            return pickBestCandidate(candidates);
        }

        List<String> closest = distanceCalc.getClosestCountries(
                referenceCountry, 20, guessedCountries);

        return closest.isEmpty() ? getAnyUnguessedCountry() : closest.get(0);
    }

    private String pickBestCandidate(List<String> candidates) {
        // Prioritize well-known countries
        String[] priority = {
                "United States", "China", "Russia", "India", "Brazil", "Germany",
                "France", "United Kingdom", "Italy", "Spain", "Canada", "Australia",
                "Mexico", "Indonesia", "Turkey", "Saudi Arabia", "Argentina", "Poland",
                "Japan", "South Korea", "Vietnam", "Thailand", "Egypt", "South Africa"
        };

        for (String p : priority) {
            for (String c : candidates) {
                if (c.equalsIgnoreCase(p)) {
                    return c;
                }
            }
        }

        return candidates.get(0);
    }

    private GuessResult getClosestGuess() {
        return guessHistory.stream()
                .min(Comparator.comparingInt(GuessResult::getDistanceAsInt))
                .orElse(guessHistory.get(0));
    }

    private String getAnyUnguessedCountry() {
        Set<String> allCountries = distanceCalc.getAllCountries();
        for (String country : allCountries) {
            if (!guessedCountries.contains(country.toLowerCase())) {
                return country;
            }
        }
        return null;
    }
}
