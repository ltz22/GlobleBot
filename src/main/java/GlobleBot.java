import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.List;
import java.util.Scanner;

public class GlobleBot {
    private WebDriver driver;
    private WebDriverWait wait;

    public GlobleBot() {
        // Set the path to your chromedriver
        //System.setProperty("webdriver.chrome.driver", "C:\\Users\\zhang\\Downloads\\chromedriver_win32\\chromedriver.exe");

        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public void start() {
        driver.get("https://globle-game.com/game");
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
            WebElement inputBox = wait.until(ExpectedConditions.
                    presenceOfElementLocated(By.cssSelector("input[type='text']")));

            // Clear any existing text
            inputBox.clear();

            // Type the country name
            inputBox.sendKeys(countryName);
            inputBox.sendKeys(Keys.RETURN);

            // Wait for the guess to be processed
            Thread.sleep(1500);

        } catch (Exception e) {
            System.out.println("Error making guess: " + e.getMessage());
        }
    }

    public GuessResult getLastGuessResult() {
        try {
            Thread.sleep(1000);

            // Find the most recent guess element (adjust selectors based on inspection)
            WebElement countriesList = driver.findElement(
                    By.cssSelector("ul.grid.grid-cols-3")
            );

            // Get all list items (guesses)
            List<WebElement> guesses = countriesList.findElements(By.tagName("li"));

            if (guesses.isEmpty()) {
                System.out.println("No guesses found yet");
                return null;
            }

            // Get the FIRST item (most recent guess)
            WebElement lastGuess = guesses.get(0);

            // Extract country name from the span
            String countryName = lastGuess.findElement(
                    By.cssSelector("span.text-md")
            ).getText();

            // Extract distance from the span with data-testid="closest-border"
            String distance = "";
            try {
                WebElement distanceElement = driver.findElement(
                        By.cssSelector("span[data-testid='closest-border']")
                );
                distance = distanceElement.getText();
            } catch (Exception e2) {
                    System.out.println("Could not find distance element");
            }

            return new GuessResult(countryName, distance);

        } catch (Exception e) {
            System.out.println("Error extracting result: " + e.getMessage());
            return null;
        }
    }

    public List<GuessResult> getAllGuesses(){
        try {
            WebElement countriesList = driver.findElement(
                    By.cssSelector("ul.grid.grid-cols-3")
            );

            List<WebElement> guesses = countriesList.findElements(By.tagName("li"));

            return guesses.stream().map(guess -> {
                try {
                    String countryName = guess.findElement(
                            By.cssSelector("span.text-md")
                    ).getText();

                    String distance = guess.findElement(
                            By.cssSelector("span[data-testid='closest-border']")
                    ).getText();

                    return new GuessResult(countryName, distance);
                } catch (Exception e) {
                    return null;
                }
            }).filter(r -> r != null).toList();

        } catch (Exception e) {
            System.out.println("Error getting all guesses: " + e.getMessage());
            return List.of();
        }
    }

    public void close() {
        driver.quit();
    }
}
