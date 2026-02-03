import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.Scanner;

public class GlobleBot {
    private WebDriver driver;
    private WebDriverWait wait;

    public GlobleBot() {
        // Set the path to your chromedriver
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\zhang\\Downloads\\chromedriver_win32");

        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public static void main(String[] args) {
        GlobleBot bot = new GlobleBot();
        Scanner scanner = new Scanner(System.in);

        try {
            bot.start();

            System.out.print("Enter your first guess: ");
            String firstGuess = scanner.nextLine();

            bot.makeGuess(firstGuess);
            GuessResult result = bot.getLastGuessResult();

            if (result != null) {
                System.out.println("Result: " + result);
            }

            // Keep the browser open
            System.out.println("Press Enter to close...");
            scanner.nextLine();

        } finally {
            bot.close();
            scanner.close();
        }
    }

    public void start() {
        driver.get("https://globle-game.com/");
        // Wait for page to load
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void makeGuess(String countryName) {
        try {
            // Find the input box (you'll need to inspect the actual selector)
            WebElement inputBox = wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector("input[type='text']") // Adjust this selector
                    )
            );

            // Clear any existing text
            inputBox.clear();

            // Type the country name
            inputBox.sendKeys(countryName);

            // Wait for autocomplete dropdown to appear
            Thread.sleep(500);

            // Select the first suggestion (or press Enter)
            WebElement firstSuggestion = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.cssSelector(".suggestion-item:first-child") // Adjust selector
                    )
            );
            firstSuggestion.click();

            // Wait for the guess to be processed
            Thread.sleep(1000);

        } catch (Exception e) {
            System.out.println("Error making guess: " + e.getMessage());
        }
    }

    public GuessResult getLastGuessResult() {
        try {
            // Find the most recent guess element (adjust selectors based on inspection)
            WebElement lastGuess = driver.findElement(
                    By.cssSelector(".guess-list .guess-item:first-child")
            );

            // Extract country name
            String country = lastGuess.findElement(By.cssSelector(".country-name")).getText();

            // Extract distance or proximity percentage
            // Globle might show distance in km or a percentage
            String distanceText = lastGuess.findElement(By.cssSelector(".distance")).getText();

            // Extract color (background color of the element)
            String backgroundColor = lastGuess.getCssValue("background-color");

            return new GuessResult(country, distanceText, backgroundColor);

        } catch (Exception e) {
            System.out.println("Error extracting result: " + e.getMessage());
            return null;
        }
    }

    public void close() {
        driver.quit();
    }
}
