import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.util.Random;

/**
 * Particle represents a single spark in a visual explosion burst.
 * It uses simple physics (velocity, deceleration/friction) and handles
 * transparency fading during rendering.
 */
public class Particle {
    private double x, y;
    private double vx, vy;
    private final Color color;
    private int life;
    private final int maxLife;

    // Constructor
    public Particle(double startX, double startY, Color color) {
        this.x = startX;
        this.y = startY;
        this.color = color;
        
        Random random = new Random();
        
        // Random velocity vector: explode in all directions
        double angle = random.nextDouble() * 2.0 * Math.PI;
        double speed = 1.0 + random.nextDouble() * 4.0; // Speed between 1.0 and 5.0
        this.vx = Math.cos(angle) * speed;
        this.vy = Math.sin(angle) * speed;
        
        // Lifespan between 10 and 20 frames
        this.maxLife = 10 + random.nextInt(11);
        this.life = this.maxLife;
    }

    /**
     * Updates physics position, applies drag friction, and decays lifespan.
     */
    public void update() {
        x += vx;
        y += vy;
        
        // Apply friction to slow particles down gradually (air drag)
        vx *= 0.92;
        vy *= 0.92;
        
        life--;
    }

    /**
     * Checks if the particle has expired.
     */
    public boolean isDead() {
        return life <= 0;
    }

    /**
     * Renders the particle square with a fading transparency based on its life.
     */
    public void draw(Graphics2D g) {
        double ratio = (double) life / maxLife;
        float alpha = (float) Math.max(0.0, Math.min(1.0, ratio));

        // Save original graphics state
        Composite originalComposite = g.getComposite();

        // Apply fading alpha transparency
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.setColor(color);
        
        // Draw a small pixel particle
        int size = 5;
        g.fillRect((int) x - (size / 2), (int) y - (size / 2), size, size);

        // Restore original graphics state
        g.setComposite(originalComposite);
    }
}
