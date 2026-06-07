import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.List;
import java.util.Random;

/**
 * Food encapsulates coordinates, type state, dropshadows,
 * 3D spherical shading, and specular lighting glares.
 * Updated to verify spawn exclusions against active portal gates.
 */
public class Food {
    private final Point position;
    private final Random random;

    // --- GOLDEN APPLE VARIABLES ---
    private boolean isGolden = false;
    private long spawnTimeMillis;
    private static final long GOLDEN_DURATION_MS = 6000;

    // Constructor
    public Food() {
        this.position = new Point();
        this.random = new Random();
    }

    /**
     * Randomly repositions the food on the board.
     * Prevents overlapping with the snake, obstacles, and portals.
     */
    public void spawn(int screenWidth, int screenHeight, int tileSize, Snake snake, 
                      List<Point> obstacles, Point portalBlue, Point portalOrange) {
        int maxSlotsX = screenWidth / tileSize;
        int maxSlotsY = screenHeight / tileSize;

        int gridX = random.nextInt(maxSlotsX) * tileSize;
        int gridY = random.nextInt(maxSlotsY) * tileSize;
        position.setLocation(gridX, gridY);

        // 1. Ensure no overlap with snake
        for (Point segment : snake.getBody()) {
            if (segment.equals(position)) {
                spawn(screenWidth, screenHeight, tileSize, snake, obstacles, portalBlue, portalOrange);
                return;
            }
        }

        // 2. Ensure no overlap with active obstacles
        if (obstacles != null) {
            for (Point obs : obstacles) {
                if (obs.equals(position)) {
                    spawn(screenWidth, screenHeight, tileSize, snake, obstacles, portalBlue, portalOrange);
                    return;
                }
            }
        }

        // 3. Ensure no overlap with portals
        if (portalBlue != null && position.equals(portalBlue)) {
            spawn(screenWidth, screenHeight, tileSize, snake, obstacles, portalBlue, portalOrange);
            return;
        }
        if (portalOrange != null && position.equals(portalOrange)) {
            spawn(screenWidth, screenHeight, tileSize, snake, obstacles, portalBlue, portalOrange);
            return;
        }

        // 15% probability of spawning a Golden Apple
        isGolden = random.nextDouble() < 0.15;
        if (isGolden) {
            spawnTimeMillis = System.currentTimeMillis();
        }
    }

    /**
     * Checks if the Golden Apple timer has run out.
     */
    public boolean isExpired() {
        if (!isGolden) {
            return false;
        }
        return (System.currentTimeMillis() - spawnTimeMillis > GOLDEN_DURATION_MS);
    }

    /**
     * Reverts a Golden Apple to a normal apple.
     */
    public void revertToNormal() {
        isGolden = false;
    }

    /**
     * Returns the remaining time of the Golden Apple in seconds.
     */
    public double getSecondsRemaining() {
        if (!isGolden) {
            return 0;
        }
        long elapsed = System.currentTimeMillis() - spawnTimeMillis;
        long remaining = Math.max(0, GOLDEN_DURATION_MS - elapsed);
        return remaining / 1000.0;
    }

    /**
     * Renders a soft dropshadow under the food coordinate.
     */
    public void drawShadow(Graphics2D g, int tileSize) {
        g.setColor(new Color(0, 0, 0, 90));
        g.fillOval(position.x + 2 + 4, position.y + 2 + 4, tileSize - 4, tileSize - 4);
    }

    /**
     * Draws the food with 3D spherical shading and specular highlights.
     */
    public void draw(Graphics2D g, int tileSize, GameTheme theme) {
        if (isGolden) {
            // Draw dynamic pulsing golden apple
            long elapsed = System.currentTimeMillis() % 600;
            double scale = 1.0 + 0.10 * Math.sin((elapsed / 600.0) * 2.0 * Math.PI);
            int size = (int) ((tileSize - 4) * scale);
            int offset = (tileSize - size) / 2;

            int drawX = position.x + offset;
            int drawY = position.y + offset;

            // Translucent outer golden glow
            g.setColor(new Color(253, 224, 71, 110));
            g.fillOval(drawX - 2, drawY - 2, size + 4, size + 4);

            g.setColor(new Color(202, 138, 4));
            g.fillOval(drawX, drawY, size, size);

            g.setColor(new Color(250, 204, 21));
            g.fillOval(drawX + 1, drawY + 1, size - 2, size - 2);

            g.setColor(new Color(255, 255, 255, 220));
            g.fillOval(drawX + 3, drawY + 3, size / 4, size / 4);

            g.setColor(Color.WHITE);
            g.fillOval(position.x + (tileSize / 2) - 1, position.y + 1, 4, 6);
        } else {
            int drawX = position.x + 2;
            int drawY = position.y + 2;
            int size = tileSize - 4;

            g.setColor(theme.getFoodColor().darker());
            g.fillOval(drawX + 1, drawY + 1, size - 1, size - 1);

            g.setColor(theme.getFoodColor());
            g.fillOval(drawX, drawY, size - 1, size - 1);

            g.setColor(new Color(255, 255, 255, 190));
            g.fillOval(drawX + 3, drawY + 3, size / 4, size / 4);

            g.setColor(theme.getLeafColor());
            g.fillOval(position.x + (tileSize / 2) - 1, position.y + 1, 4, 6);
        }
    }

    // --- GETTERS ---
    
    public Point getPosition() {
        return position;
    }

    public int getX() {
        return position.x;
    }

    public int getY() {
        return position.y;
    }

    public boolean isGolden() {
        return isGolden;
    }
}
