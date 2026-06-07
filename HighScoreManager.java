import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * HighScoreManager handles persistence of the all-time high score.
 * Upgraded to use CompletableFuture for non-blocking asynchronous file operations.
 */
public class HighScoreManager {
    private static final String FILE_NAME = "highscore.txt";

    /**
     * Loads the high score asynchronously.
     *
     * @return a CompletableFuture resolving to the saved high score.
     */
    public static CompletableFuture<Integer> loadHighScoreAsync() {
        return CompletableFuture.supplyAsync(HighScoreManager::loadHighScore);
    }

    /**
     * Saves the high score asynchronously.
     *
     * @param score the score to save.
     */
    public static void saveHighScoreAsync(int score) {
        CompletableFuture.runAsync(() -> saveHighScore(score));
    }

    /**
     * Loads the high score from the local file (synchronous back-end).
     */
    private static int loadHighScore() {
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            return 0;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (line != null) {
                return Integer.parseInt(line.trim());
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error reading high score: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Saves the high score to the local file (synchronous back-end).
     */
    private static void saveHighScore(int score) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME))) {
            writer.write(String.valueOf(score));
        } catch (IOException e) {
            System.err.println("Error saving high score: " + e.getMessage());
        }
    }
}
