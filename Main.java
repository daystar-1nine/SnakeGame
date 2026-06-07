import javax.swing.SwingUtilities;

/**
 * Main is the entry point of the Snake Game.
 * It is responsible for launching the game UI safely.
 */
public class Main {
    public static void main(String[] args) {
        // Run GUI code on the Event Dispatch Thread (EDT) for thread safety
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Initialize the game window
                new GameFrame();
            }
        });
    }
}
