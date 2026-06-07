import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

/**
 * GamePanel manages the game loop, user inputs (using a queue buffer), updates logic,
 * particle explosions, levels, Bullet Time, and Portal Wormholes.
 * Refactored to eliminate EDT-blocking file operations, improve focus management, and support JAR packaging.
 */
public class GamePanel extends JPanel implements ActionListener {

    // --- CONSTANTS & CONFIGURATION ---
    private static final int SCREEN_WIDTH = 600;
    private static final int SCREEN_HEIGHT = 600;
    private static final int TILE_SIZE = 25; // Size of each grid square in pixels
    private static final int INITIAL_DELAY = 120; // Starting timer delay in milliseconds

    // --- GAME ENTITIES & UTILS ---
    private Snake snake;
    private Food food;
    private Image logoImage;
    private GameTheme currentTheme = GameTheme.MIDNIGHT; // Active theme selection
    
    // --- STANDOUT FEATURES & STATS ---
    private Point portalBlue;
    private Point portalOrange;
    private boolean bulletTimeActive = false; // Bullet Time state
    private double reflexEnergy = 100.0; // Slow-motion energy meter [0.0, 100.0]
    private int bulletTimeTickCounter = 0; // Skip-tick accumulator

    // --- GAME STABILIZATION & EFFECTS ---
    private final Queue<Direction> inputQueue = new LinkedList<>(); // Key event queue buffer
    private final List<Particle> particles = new ArrayList<>(); // Active particle list
    private final List<Point> obstacles = new PointList(); // Grid obstacles list

    // --- GAME STATE VARIABLES ---
    private boolean showStartScreen = true; // Splash title screen state
    private boolean running = false;
    private boolean gameOver = false;
    private boolean paused = false;
    
    private int score = 0;
    private int highScore = 0;
    private int currentLevel = 1;

    private Timer timer;

    // Helper custom list to avoid IDE warnings
    private static class PointList extends ArrayList<Point> {}

    // Constructor
    public GamePanel() {
        // Define panel properties
        this.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        this.setBackground(currentTheme.getBackgroundColor()); // Apply active theme background
        this.setFocusable(true); // Allows panel to capture key events
        
        // Register key listener to capture user keystrokes
        this.addKeyListener(new GameKeyAdapter());
        
        // Load the persistent high score asynchronously (moves File I/O off the EDT)
        HighScoreManager.loadHighScoreAsync().thenAccept(score -> {
            this.highScore = score;
            repaint(); // Repaint once loaded to display high score on start screen
        });

        // Load the game logo asset (classpath resource stream with a filesystem fallback)
        try {
            Image img = null;
            try (InputStream is = getClass().getResourceAsStream("/logo.png")) {
                if (is != null) {
                    img = javax.imageio.ImageIO.read(is);
                }
            }
            // Filesystem fallback
            if (img == null) {
                File logoFile = new File("logo.png");
                if (logoFile.exists()) {
                    img = javax.imageio.ImageIO.read(logoFile);
                }
            }
            logoImage = img;
        } catch (java.io.IOException e) {
            System.err.println("Could not load logo image: " + e.getMessage());
        }
    }

    /**
     * Initializes or resets the game variables and starts the loop timer.
     */
    public void startGame() {
        // Guarantee focus capture for keyboard event triggers
        this.requestFocusInWindow();

        // Reset game stats and state
        score = 0;
        currentLevel = 1;
        gameOver = false;
        paused = false;
        showStartScreen = false;
        
        // Reset Bullet Time properties
        bulletTimeActive = false;
        reflexEnergy = 100.0;
        bulletTimeTickCounter = 0;

        // Clear runtime state collections
        inputQueue.clear();
        particles.clear();
        obstacles.clear();
        
        // Refresh all-time high score from local file asynchronously
        HighScoreManager.loadHighScoreAsync().thenAccept(score -> {
            this.highScore = score;
        });

        // Create new instances of Snake and Food
        snake = new Snake(100, 100, 3, TILE_SIZE);
        loadPortals(); // Generate Blue/Orange portal gates
        food = new Food();
        food.spawn(SCREEN_WIDTH, SCREEN_HEIGHT, TILE_SIZE, snake, obstacles, portalBlue, portalOrange);

        // Reset and start the game loop timer
        running = true;
        if (timer != null) {
            timer.stop();
        }
        timer = new Timer(INITIAL_DELAY, this);
        timer.start();
    }

    /**
     * Generates wormhole gates ensuring they are placed on empty playable cells.
     */
    private void loadPortals() {
        portalBlue = getSafePortalLocation();
        portalOrange = getSafePortalLocation();
    }

    /**
     * Finds a grid cell that is not occupied by the snake or obstacles.
     */
    private Point getSafePortalLocation() {
        Random r = new Random();
        int maxGridX = SCREEN_WIDTH / TILE_SIZE;
        int maxGridY = SCREEN_HEIGHT / TILE_SIZE;
        Point p = new Point();
        
        boolean valid = false;
        while (!valid) {
            int gx = r.nextInt(maxGridX) * TILE_SIZE;
            int gy = r.nextInt(maxGridY) * TILE_SIZE;
            p.setLocation(gx, gy);
            
            valid = true;
            
            // Exclude snake segments
            for (Point seg : snake.getBody()) {
                if (seg.equals(p)) {
                    valid = false;
                    break;
                }
            }
            
            // Exclude level obstacles
            for (Point obs : obstacles) {
                if (obs.equals(p)) {
                    valid = false;
                    break;
                }
            }
            
            // Exclude food position
            if (food != null && food.getPosition().equals(p)) {
                valid = false;
            }
            
            // Exclude other portal position
            if (portalBlue != null && portalBlue.equals(p)) {
                valid = false;
            }
        }
        return p;
    }

    /**
     * Spawns a radial burst of particles at the specified coordinates.
     */
    private void spawnParticles(double startX, double startY, Color color) {
        int count = 15;
        for (int i = 0; i < count; i++) {
            particles.add(new Particle(startX, startY, color));
        }
    }

    /**
     * Loads obstacle coordinates based on the active level.
     */
    private void loadObstacles() {
        obstacles.clear();
        
        if (currentLevel >= 2) {
            obstacles.add(new Point(5 * TILE_SIZE, 5 * TILE_SIZE));
            obstacles.add(new Point(18 * TILE_SIZE, 5 * TILE_SIZE));
            obstacles.add(new Point(5 * TILE_SIZE, 18 * TILE_SIZE));
            obstacles.add(new Point(18 * TILE_SIZE, 18 * TILE_SIZE));
        }
        
        if (currentLevel >= 3) {
            obstacles.add(new Point(11 * TILE_SIZE, 11 * TILE_SIZE));
            obstacles.add(new Point(12 * TILE_SIZE, 11 * TILE_SIZE));
            obstacles.add(new Point(11 * TILE_SIZE, 12 * TILE_SIZE));
            obstacles.add(new Point(12 * TILE_SIZE, 12 * TILE_SIZE));
            
            obstacles.add(new Point(10 * TILE_SIZE, 11 * TILE_SIZE));
            obstacles.add(new Point(13 * TILE_SIZE, 11 * TILE_SIZE));
            obstacles.add(new Point(11 * TILE_SIZE, 10 * TILE_SIZE));
            obstacles.add(new Point(11 * TILE_SIZE, 13 * TILE_SIZE));
        }
        
        // After loading obstacles, re-evaluate portals so they aren't trapped
        loadPortals();
    }

    /**
     * Checks if the score warrants a level-up.
     */
    private void checkLevelUp() {
        int oldLevel = currentLevel;
        
        if (score >= 120) {
            currentLevel = 3;
        } else if (score >= 50) {
            currentLevel = 2;
        } else {
            currentLevel = 1;
        }

        if (currentLevel != oldLevel) {
            SoundPlayer.playToneAsync(600, 1200, 250);
            loadObstacles();
        }
    }

    /**
     * Swing framework calls this method automatically whenever repaint() is requested.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        draw(g2d);
    }

    /**
     * Handles drawing game elements based on the current state.
     */
    private void draw(Graphics2D g) {
        if (showStartScreen) {
            drawStartScreen(g);
            return;
        }

        // --- LAYER 1: GRID GUIDES ---
        g.setColor(currentTheme.getGridColor());
        for (int i = 0; i < SCREEN_WIDTH / TILE_SIZE; i++) {
            g.drawLine(i * TILE_SIZE, 0, i * TILE_SIZE, SCREEN_HEIGHT);
            g.drawLine(0, i * TILE_SIZE, SCREEN_WIDTH, i * TILE_SIZE);
        }

        // --- LAYER 2: DROPSHADOWS ---
        g.setColor(new Color(0, 0, 0, 95));
        for (Point obs : obstacles) {
            g.fillRoundRect(obs.x + 1 + 4, obs.y + 1 + 4, TILE_SIZE - 2, TILE_SIZE - 2, 6, 6);
        }
        g.setColor(new Color(0, 0, 0, 60));
        g.fillOval(portalBlue.x + 3, portalBlue.y + 3, TILE_SIZE, TILE_SIZE);
        g.fillOval(portalOrange.x + 3, portalOrange.y + 3, TILE_SIZE, TILE_SIZE);
        
        food.drawShadow(g, TILE_SIZE);
        snake.drawShadow(g, TILE_SIZE);

        // --- LAYER 3: AMBIENT GLOW LIGHTING ---
        Point head = snake.getHead();
        Color headColor = currentTheme.getSnakeHeadColor();
        g.setColor(new Color(headColor.getRed(), headColor.getGreen(), headColor.getBlue(), 12));
        g.fillOval(head.x - 20, head.y - 20, TILE_SIZE + 40, TILE_SIZE + 40);

        if (food.isGolden()) {
            g.setColor(new Color(250, 204, 21, 15));
            g.fillOval(food.getX() - 30, food.getY() - 30, TILE_SIZE + 60, TILE_SIZE + 60);
            g.setColor(new Color(250, 204, 21, 30));
            g.fillOval(food.getX() - 15, food.getY() - 15, TILE_SIZE + 30, TILE_SIZE + 30);
        }

        // --- LAYER 4: PORTAL WORMEHOLES ---
        drawPortals(g);

        // --- LAYER 5: SOLID ENTITIES ---
        drawObstacles(g);
        food.draw(g, TILE_SIZE, currentTheme);
        snake.draw(g, TILE_SIZE, currentTheme);

        // --- LAYER 6: DYNAMIC PARTICLES ---
        for (Particle p : particles) {
            p.draw(g);
        }

        // --- LAYER 7: HUD DISPLAY ---
        g.setColor(currentTheme.getHudColor());
        g.setFont(new Font("Segoe UI", Font.BOLD, 15));
        g.drawString("Score: " + score, 15, 30);
        g.drawString("High Score: " + highScore, 15, 52);
        
        g.setColor(currentTheme.getAccentColor());
        g.drawString("Level: " + currentLevel, 15, 74);

        if (food.isGolden()) {
            drawGoldenAppleTimer(g);
        }

        drawReflexMeter(g);

        // --- LAYER 8: BULLET TIME BORDER VIGNETTE ---
        if (bulletTimeActive) {
            drawBulletTimeVignette(g);
        }

        // --- LAYER 9: SCREEN STATE OVERLAYS ---
        if (paused) {
            drawPauseScreen(g);
        } else if (gameOver) {
            drawGameOverScreen(g);
        }
    }

    /**
     * Renders a glowing progress bar indicating time dilation energy.
     */
    private void drawReflexMeter(Graphics2D g) {
        g.setColor(new Color(6, 182, 212, 70));
        g.drawRect(SCREEN_WIDTH - 145, 15, 130, 14);

        int fillWidth = (int) ((reflexEnergy / 100.0) * 130);
        g.setColor(new Color(6, 182, 212, 180)); 
        g.fillRect(SCREEN_WIDTH - 144, 16, fillWidth - 1, 12);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Segoe UI", Font.BOLD, 10));
        FontMetrics fm = g.getFontMetrics();
        String txt = "REFLEX METER";
        int tx = SCREEN_WIDTH - 145 + (130 - fm.stringWidth(txt)) / 2;
        g.drawString(txt, tx, 26);
    }

    /**
     * Draws a cyan vignette screen frame representing time slow.
     */
    private void drawBulletTimeVignette(Graphics2D g) {
        g.setColor(new Color(6, 182, 212, 35)); 
        g.setStroke(new BasicStroke(18));
        g.drawRect(9, 9, SCREEN_WIDTH - 18, SCREEN_HEIGHT - 18);
    }

    /**
     * Draws swirling, pulsing circular vortexes for both portals.
     */
    private void drawPortals(Graphics2D g) {
        long elapsed = System.currentTimeMillis();
        
        // 1. Blue Portal
        double pulseBlue = 1.0 + 0.15 * Math.sin((elapsed % 400) / 400.0 * 2.0 * Math.PI);
        int sizeB = (int) (TILE_SIZE * pulseBlue);
        int offsetB = (TILE_SIZE - sizeB) / 2;
        
        g.setColor(new Color(59, 130, 246, 80)); 
        g.fillOval(portalBlue.x - 3 + offsetB, portalBlue.y - 3 + offsetB, sizeB + 6, sizeB + 6);
        g.setColor(new Color(37, 99, 235)); 
        g.fillOval(portalBlue.x + offsetB, portalBlue.y + offsetB, sizeB, sizeB);
        g.setColor(Color.WHITE); 
        g.fillOval(portalBlue.x + (TILE_SIZE - 6) / 2, portalBlue.y + (TILE_SIZE - 6) / 2, 6, 6);
        
        // 2. Orange Portal
        double pulseOrange = 1.0 + 0.15 * Math.sin(((elapsed + 200) % 400) / 400.0 * 2.0 * Math.PI);
        int sizeO = (int) (TILE_SIZE * pulseOrange);
        int offsetO = (TILE_SIZE - sizeO) / 2;
        
        g.setColor(new Color(249, 115, 22, 80)); 
        g.fillOval(portalOrange.x - 3 + offsetO, portalOrange.y - 3 + offsetO, sizeO + 6, sizeO + 6);
        g.setColor(new Color(234, 88, 12)); 
        g.fillOval(portalOrange.x + offsetO, portalOrange.y + offsetO, sizeO, sizeO);
        g.setColor(Color.WHITE); 
        g.fillOval(portalOrange.x + (TILE_SIZE - 6) / 2, portalOrange.y + (TILE_SIZE - 6) / 2, 6, 6);
    }

    /**
     * Draws stone pillars with metallic highlights, dark shadows, and rock cracks.
     */
    private void drawObstacles(Graphics2D g) {
        for (Point obs : obstacles) {
            g.setColor(new Color(30, 41, 59));
            g.fillRoundRect(obs.x + 1, obs.y + 1, TILE_SIZE - 2, TILE_SIZE - 2, 6, 6);

            g.setColor(new Color(148, 163, 184, 180)); 
            g.drawLine(obs.x + 2, obs.y + 2, obs.x + TILE_SIZE - 3, obs.y + 2);
            g.drawLine(obs.x + 2, obs.y + 2, obs.x + 2, obs.y + TILE_SIZE - 3);

            g.setColor(new Color(15, 23, 42, 220)); 
            g.drawLine(obs.x + 2, obs.y + TILE_SIZE - 2, obs.x + TILE_SIZE - 2, obs.y + TILE_SIZE - 2);
            g.drawLine(obs.x + TILE_SIZE - 2, obs.y + 2, obs.x + TILE_SIZE - 2, obs.y + TILE_SIZE - 2);

            g.setColor(new Color(15, 23, 42, 120)); 
            g.drawLine(obs.x + 5, obs.y + 6, obs.x + 10, obs.y + 12);
            g.drawLine(obs.x + 10, obs.y + 12, obs.x + 7, obs.y + 18);
            g.drawLine(obs.x + 20, obs.y + 5, obs.x + 15, obs.y + 12);

            g.setColor(new Color(148, 163, 184, 80)); 
            g.drawLine(obs.x + 6, obs.y + 6, obs.x + 11, obs.y + 12);
            g.drawLine(obs.x + 11, obs.y + 12, obs.x + 8, obs.y + 18);
            g.drawLine(obs.x + 21, obs.y + 5, obs.x + 16, obs.y + 12);
        }
    }

    /**
     * Renders a custom countdown timer bar and text for Golden Apples.
     */
    private void drawGoldenAppleTimer(Graphics2D g) {
        double remaining = food.getSecondsRemaining();
        
        g.setColor(currentTheme.getAccentColor());
        g.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g.drawString(String.format("Golden Apple: %.1fs", remaining), 15, 96);

        g.setColor(new Color(250, 204, 21, 50)); 
        g.drawRect(15, 104, 120, 8);

        int fillWidth = (int) ((remaining / 6.0) * 120);
        g.setColor(new Color(250, 204, 21)); 
        g.fillRect(16, 105, fillWidth - 1, 6);
    }

    /**
     * Renders a stunning Splash Start Screen showing the game's logo.
     */
    private void drawStartScreen(Graphics2D g) {
        if (logoImage != null) {
            int logoWidth = 180;
            int logoHeight = 180;
            int logoX = (SCREEN_WIDTH - logoWidth) / 2;
            int logoY = 90;
            g.drawImage(logoImage, logoX, logoY, logoWidth, logoHeight, null);
        }

        Font titleFont = new Font("Segoe UI", Font.BOLD, 44);
        Font promptFont = new Font("Segoe UI", Font.PLAIN, 18);
        Font themeFont = new Font("Segoe UI", Font.BOLD, 14);
        Font guideFont = new Font("Segoe UI", Font.PLAIN, 13);

        g.setColor(currentTheme.getSnakeHeadColor());
        g.setFont(titleFont);
        FontMetrics metrics1 = getFontMetrics(titleFont);
        String titleText = "SNAKE GAME";
        int x1 = (SCREEN_WIDTH - metrics1.stringWidth(titleText)) / 2;
        g.drawString(titleText, x1, 315);

        g.setColor(Color.WHITE);
        g.setFont(promptFont);
        FontMetrics metrics2 = getFontMetrics(promptFont);
        String promptText = "Press SPACE or ENTER to Play";
        int x2 = (SCREEN_WIDTH - metrics2.stringWidth(promptText)) / 2;
        g.drawString(promptText, x2, 365);

        g.setColor(currentTheme.getAccentColor());
        g.setFont(themeFont);
        FontMetrics metrics3 = getFontMetrics(themeFont);
        String themeText = "Press 'T' to Change Theme: " + currentTheme.getDisplayName();
        int x3 = (SCREEN_WIDTH - metrics3.stringWidth(themeText)) / 2;
        g.drawString(themeText, x3, 420);

        g.setColor(new Color(148, 163, 184)); 
        g.setFont(guideFont);
        FontMetrics metrics4 = getFontMetrics(guideFont);
        String guideText = "Controls: Arrows/WASD | Pause: P/ESC | Reflex: SHIFT";
        int x4 = (SCREEN_WIDTH - metrics4.stringWidth(guideText)) / 2;
        g.drawString(guideText, x4, 475);
    }

    /**
     * Renders a blurred translucent overlay indicating the game is paused.
     */
    private void drawPauseScreen(Graphics2D g) {
        g.setColor(new Color(15, 23, 42, 180)); 
        g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        Font titleFont = new Font("Segoe UI", Font.BOLD, 48);
        Font promptFont = new Font("Segoe UI", Font.PLAIN, 16);

        g.setColor(currentTheme.getAccentColor());
        g.setFont(titleFont);
        FontMetrics metrics1 = getFontMetrics(titleFont);
        String pausedText = "PAUSED";
        int x1 = (SCREEN_WIDTH - metrics1.stringWidth(pausedText)) / 2;
        int y1 = (SCREEN_HEIGHT / 2) - 10;
        g.drawString(pausedText, x1, y1);

        g.setColor(Color.WHITE);
        g.setFont(promptFont);
        FontMetrics metrics2 = getFontMetrics(promptFont);
        String promptText = "Press 'P' or 'ESC' to Resume";
        int x2 = (SCREEN_WIDTH - metrics2.stringWidth(promptText)) / 2;
        int y2 = (SCREEN_HEIGHT / 2) + 30;
        g.drawString(promptText, x2, y2);
    }

    /**
     * Renders a premium Game Over screen with transparency and instructions.
     */
    private void drawGameOverScreen(Graphics2D g) {
        g.setColor(new Color(15, 23, 42, 220)); 
        g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        if (logoImage != null) {
            int logoWidth = 100;
            int logoHeight = 100;
            int logoX = (SCREEN_WIDTH - logoWidth) / 2;
            int logoY = 80;
            g.drawImage(logoImage, logoX, logoY, logoWidth, logoHeight, null);
        }

        Font titleFont = new Font("Segoe UI", Font.BOLD, 50);
        Font scoreFont = new Font("Segoe UI", Font.BOLD, 24);
        Font promptFont = new Font("Segoe UI", Font.PLAIN, 16);

        // Draw "GAME OVER"
        g.setColor(new Color(239, 68, 68)); 
        g.setFont(titleFont);
        FontMetrics metrics1 = getFontMetrics(titleFont);
        String gameOverText = "GAME OVER";
        int x1 = (SCREEN_WIDTH - metrics1.stringWidth(gameOverText)) / 2;
        int y1 = 230;
        g.drawString(gameOverText, x1, y1);

        // Draw Final Score
        g.setColor(Color.WHITE);
        g.setFont(scoreFont);
        FontMetrics metrics2 = getFontMetrics(scoreFont);
        String scoreText = "Your Final Score: " + score;
        int x2 = (SCREEN_WIDTH - metrics2.stringWidth(scoreText)) / 2;
        int y2 = 280;
        g.drawString(scoreText, x2, y2);

        // Draw High Score
        g.setColor(new Color(250, 204, 21)); 
        FontMetrics metrics3 = getFontMetrics(scoreFont);
        String highText = "All-Time High: " + highScore;
        int x3 = (SCREEN_WIDTH - metrics3.stringWidth(highText)) / 2;
        int y3 = 315;
        g.drawString(highText, x3, y3);

        // Draw Restart Instruction
        g.setColor(new Color(148, 163, 184)); 
        g.setFont(promptFont);
        FontMetrics metrics4 = getFontMetrics(promptFont);
        String restartText = "Press 'R' or 'SPACE' to Restart";
        int x4 = (SCREEN_WIDTH - metrics4.stringWidth(restartText)) / 2;
        int y4 = 380;
        g.drawString(restartText, x4, y4);
    }

    /**
     * Checks if the snake's head coordinates match the food coordinates.
     */
    private void checkFood() {
        if (snake.getHead().equals(food.getPosition())) {
            snake.grow(); 
            
            Color particleColor;
            
            if (food.isGolden()) {
                score += 30;
                particleColor = new Color(250, 204, 21); // Golden particles
                SoundPlayer.playToneAsync(500, 1000, 180); // Higher tone sweep
            } else {
                score += 10;
                particleColor = currentTheme.getFoodColor(); // Theme food color particles
                SoundPlayer.playEatSound();
            }

            // Restore reflex slow-motion energy by +15%
            reflexEnergy = Math.min(100.0, reflexEnergy + 15.0);

            // Spawn particle explosion burst centered at the food position
            spawnParticles(food.getX() + (TILE_SIZE / 2.0), food.getY() + (TILE_SIZE / 2.0), particleColor);

            checkLevelUp(); // Check if level upgraded

            food.spawn(SCREEN_WIDTH, SCREEN_HEIGHT, TILE_SIZE, snake, obstacles, portalBlue, portalOrange);

            if (score > highScore) {
                highScore = score;
            }

            // Adjust speed dynamically: Speed increases by reducing timer delay
            int newDelay = Math.max(50, INITIAL_DELAY - (score / 10) * 3);
            timer.setDelay(newDelay);
        }
    }

    /**
     * Checks if snake head sits on wormhole coordinates and teleports.
     */
    private void checkPortals() {
        Point head = snake.getHead();
        if (head.equals(portalBlue)) {
            snake.teleportTo(portalOrange, TILE_SIZE);
            SoundPlayer.playToneAsync(400, 800, 100); // Teleport audio blip
            
            // Spawn orange sparks at exit portal orange
            spawnParticles(portalOrange.x + (TILE_SIZE / 2.0), portalOrange.y + (TILE_SIZE / 2.0), new Color(249, 115, 22));
        } else if (head.equals(portalOrange)) {
            snake.teleportTo(portalBlue, TILE_SIZE);
            SoundPlayer.playToneAsync(400, 800, 100); // Teleport audio blip
            
            // Spawn blue sparks at exit portal blue
            spawnParticles(portalBlue.x + (TILE_SIZE / 2.0), portalBlue.y + (TILE_SIZE / 2.0), new Color(59, 130, 246));
        }
    }

    /**
     * Detects collision with boundaries, snake's own body, or level obstacles.
     */
    private void checkCollisions() {
        if (snake.checkSelfCollision()) {
            endGame();
            return;
        }

        if (snake.checkBoundaryCollision(SCREEN_WIDTH, SCREEN_HEIGHT)) {
            endGame();
            return;
        }

        for (Point obs : obstacles) {
            if (snake.getHead().equals(obs)) {
                endGame();
                return;
            }
        }
    }

    /**
     * Terminates the game loop, registers high score, and plays death sound.
     */
    private void endGame() {
        running = false;
        gameOver = true;
        bulletTimeActive = false;
        timer.stop();

        SoundPlayer.playGameOverSound();

        // Save score asynchronously (moves File I/O off the EDT)
        if (score >= highScore) {
            HighScoreManager.saveHighScoreAsync(score);
        }
    }

    /**
     * Performs standard coordinate updates, collision controls, food ingestion, and portal checks.
     */
    private void updateGame() {
        // Check if active Golden Apple has timed out
        if (food.isGolden() && food.isExpired()) {
            food.revertToNormal();
        }

        // Pull the next buffered direction from our queue
        if (!inputQueue.isEmpty()) {
            Direction nextDir = inputQueue.poll();
            snake.setDirection(nextDir);
        }
        
        snake.move(TILE_SIZE);
        checkPortals(); // Check if snake is stepping through wormholes
        checkFood();
        checkCollisions();
    }

    /**
     * The event handler invoked at every clock tick of our Timer.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        // Update particles (always run so sparks fade smoothly even during pauses)
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.update();
            if (p.isDead()) {
                particles.remove(i);
            }
        }

        if (running && !paused) {
            // Manage ambient portal swirl sparks
            if (Math.random() < 0.08) {
                particles.add(new Particle(portalBlue.x + (TILE_SIZE / 2.0), portalBlue.y + (TILE_SIZE / 2.0), new Color(59, 130, 246)));
                particles.add(new Particle(portalOrange.x + (TILE_SIZE / 2.0), portalOrange.y + (TILE_SIZE / 2.0), new Color(249, 115, 22)));
            }

            // Manage Reflex Energy regeneration/depletion
            if (bulletTimeActive) {
                reflexEnergy -= 0.6; // Deplete energy
                if (reflexEnergy <= 0.0) {
                    reflexEnergy = 0.0;
                    bulletTimeActive = false;
                    SoundPlayer.playClickSound(); // Audio cue for energy depletion exit
                }
            } else {
                reflexEnergy = Math.min(100.0, reflexEnergy + 0.1); // Regenerate energy
            }

            // Bullet Time slow-motion logic: Skip updates to create slow-motion ticks
            if (bulletTimeActive) {
                bulletTimeTickCounter++;
                if (bulletTimeTickCounter % 3 == 0) { // Updates at 33% of regular speed
                    updateGame();
                }
            } else {
                updateGame();
            }
        }
        
        repaint();
    }

    /**
     * Helper to verify if two directions are opposing.
     */
    private boolean isOpposite(Direction d1, Direction d2) {
        return (d1 == Direction.LEFT && d2 == Direction.RIGHT) ||
               (d1 == Direction.RIGHT && d2 == Direction.LEFT) ||
               (d1 == Direction.UP && d2 == Direction.DOWN) ||
               (d1 == Direction.DOWN && d2 == Direction.UP);
    }

    /**
     * Nested inner class to capture player key inputs.
     */
    private class GameKeyAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();

            if (showStartScreen) {
                if (key == KeyEvent.VK_T) {
                    int nextOrdinal = (currentTheme.ordinal() + 1) % GameTheme.values().length;
                    currentTheme = GameTheme.values()[nextOrdinal];
                    setBackground(currentTheme.getBackgroundColor());
                    SoundPlayer.playClickSound();
                    repaint();
                } else if (key == KeyEvent.VK_SPACE || key == KeyEvent.VK_ENTER) {
                    SoundPlayer.playClickSound();
                    startGame();
                }
                return;
            }

            if (running) {
                // Handle Pause trigger (ESC or P)
                if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_P) {
                    paused = !paused;
                    SoundPlayer.playClickSound();
                    repaint();
                    return;
                }

                // Handle Bullet Time Slow Motion trigger (SHIFT)
                if (key == KeyEvent.VK_SHIFT) {
                    if (!paused) {
                        // Prevent activating with 0 energy
                        if (!bulletTimeActive && reflexEnergy > 10.0) {
                            bulletTimeActive = true;
                            SoundPlayer.playClickSound();
                        } else if (bulletTimeActive) {
                            bulletTimeActive = false;
                            SoundPlayer.playClickSound();
                        }
                        repaint();
                    }
                    return;
                }

                // If game is paused, do not process direction inputs
                if (paused) {
                    return;
                }

                // Buffer direction changes to resolve rapid double-keystroke self-deaths
                Direction pressedDir = null;
                switch (key) {
                    case KeyEvent.VK_LEFT:
                    case KeyEvent.VK_A:
                        pressedDir = Direction.LEFT;
                        break;
                    case KeyEvent.VK_RIGHT:
                    case KeyEvent.VK_D:
                        pressedDir = Direction.RIGHT;
                        break;
                    case KeyEvent.VK_UP:
                    case KeyEvent.VK_W:
                        pressedDir = Direction.UP;
                        break;
                    case KeyEvent.VK_DOWN:
                    case KeyEvent.VK_S:
                        pressedDir = Direction.DOWN;
                        break;
                }

                if (pressedDir != null) {
                    Direction lastQueued = inputQueue.isEmpty() 
                        ? snake.getDirection() 
                        : ((LinkedList<Direction>) inputQueue).getLast();

                    if (pressedDir != lastQueued && !isOpposite(pressedDir, lastQueued)) {
                        if (inputQueue.size() < 2) {
                            inputQueue.add(pressedDir);
                        }
                    }
                }
            } else if (gameOver) {
                // Restart on pressing R or Space
                if (key == KeyEvent.VK_R || key == KeyEvent.VK_SPACE) {
                    SoundPlayer.playClickSound();
                    startGame();
                }
            }
        }
    }
}
