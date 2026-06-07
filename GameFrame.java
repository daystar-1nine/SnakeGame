import javax.swing.JFrame;
import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * GameFrame represents the main application window (the frame).
 * It acts as the shell/window container that holds our GamePanel.
 */
public class GameFrame extends JFrame {

    // Constructor
    public GameFrame() {
        // Create an instance of our GamePanel
        GamePanel panel = new GamePanel();
        
        // Add the panel to the frame
        this.add(panel);
        
        // Set the window title
        this.setTitle("Snake Game");
        
        // Ensure the application closes completely when clicking the 'X' button
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Prevent the user from resizing the window to keep our game grid aligned
        this.setResizable(false);
        
        // Load the logo image and set it as the window/taskbar icon
        // Tries loading as resource stream (JAR packaging) first, with a filesystem fallback
        try {
            Image icon = null;
            try (InputStream is = getClass().getResourceAsStream("/logo.png")) {
                if (is != null) {
                    icon = ImageIO.read(is);
                }
            }
            
            // Filesystem fallback
            if (icon == null) {
                File logoFile = new File("logo.png");
                if (logoFile.exists()) {
                    icon = ImageIO.read(logoFile);
                }
            }

            if (icon != null) {
                this.setIconImage(icon);
            }
        } catch (IOException e) {
            System.err.println("Could not load application icon: " + e.getMessage());
        }

        // Automatically size the window to fit the preferred size of our GamePanel
        this.pack();
        
        // Center the window on the screen (null centers it relative to the screen)
        this.setLocationRelativeTo(null);
        
        // Make the window visible
        this.setVisible(true);
    }
}
