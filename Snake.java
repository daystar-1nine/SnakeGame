import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.LinkedList;

/**
 * Snake encapsulates coordinates and visual behaviors of the snake.
 * Now supports portals and head teleportation offsets.
 */
public class Snake {
    private final LinkedList<Point> body;
    private Direction direction;
    private boolean growPending;

    // Constructor
    public Snake(int startX, int startY, int initialParts, int tileSize) {
        body = new LinkedList<>();
        direction = Direction.RIGHT;
        growPending = false;

        // Populate initial body segments horizontally
        for (int i = 0; i < initialParts; i++) {
            body.add(new Point(startX - (i * tileSize), startY));
        }
    }

    /**
     * Updates the snake's position.
     */
    public void move(int tileSize) {
        Point head = body.getFirst();
        Point newHead = new Point(head.x, head.y);

        switch (direction) {
            case UP:
                newHead.y -= tileSize;
                break;
            case DOWN:
                newHead.y += tileSize;
                break;
            case LEFT:
                newHead.x -= tileSize;
                break;
            case RIGHT:
                newHead.x += tileSize;
                break;
        }

        body.addFirst(newHead);

        if (growPending) {
            growPending = false;
        } else {
            body.removeLast();
        }
    }

    /**
     * Teleports the snake's head to the destination point.
     * Offsets the head by one tile in the active direction to prevent infinite teleportation loops.
     *
     * @param destination the portal destination coordinate.
     * @param tileSize    the grid tile size in pixels.
     */
    public void teleportTo(Point destination, int tileSize) {
        Point head = body.getFirst();
        int newX = destination.x;
        int newY = destination.y;

        switch (direction) {
            case UP:
                newY -= tileSize;
                break;
            case DOWN:
                newY += tileSize;
                break;
            case LEFT:
                newX -= tileSize;
                break;
            case RIGHT:
                newX += tileSize;
                break;
        }
        head.setLocation(newX, newY);
    }

    /**
     * Triggers the snake to grow on the next move.
     */
    public void grow() {
        growPending = true;
    }

    /**
     * Checks if the snake's head has collided with its body.
     */
    public boolean checkSelfCollision() {
        Point head = body.getFirst();
        for (int i = 1; i < body.size(); i++) {
            if (head.equals(body.get(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the snake has hit the boundaries.
     */
    public boolean checkBoundaryCollision(int screenWidth, int screenHeight) {
        Point head = body.getFirst();
        return (head.x < 0 || head.x >= screenWidth || head.y < 0 || head.y >= screenHeight);
    }

    /**
     * Renders the drop shadows of the snake's body segments.
     */
    public void drawShadow(Graphics2D g, int tileSize) {
        g.setColor(new Color(0, 0, 0, 90));
        
        for (int i = 0; i < body.size(); i++) {
            Point p = body.get(i);
            
            double factor = 1.0 - 0.45 * ((double) i / body.size());
            int size = (int) (tileSize * factor);
            int offset = (tileSize - size) / 2;
            
            g.fillRoundRect(p.x + offset + 4, p.y + offset + 4, size, size, 8, 8);
        }
    }

    /**
     * Renders the snake segments on the screen.
     */
    public void draw(Graphics2D g, int tileSize, GameTheme theme) {
        for (int i = body.size() - 1; i >= 0; i--) {
            Point p = body.get(i);
            
            double factor = 1.0 - 0.45 * ((double) i / body.size());
            int size = (int) (tileSize * factor);
            int offset = (tileSize - size) / 2;
            
            int drawX = p.x + offset;
            int drawY = p.y + offset;

            if (i == 0) {
                // --- DRAW HEAD ---
                g.setColor(theme.getSnakeHeadColor());
                g.fillRoundRect(drawX, drawY, size, size, 8, 8);

                drawEyes(g, drawX, drawY, size);
                drawFlickeringTongue(g, drawX, drawY, size);
                
                g.setColor(new Color(255, 255, 255, 60));
                g.fillOval(drawX + 3, drawY + 3, size / 3, size / 3);

            } else {
                // --- DRAW BODY SEGMENTS ---
                if (i % 2 == 0) {
                    g.setColor(theme.getSnakeBodyEvenColor());
                } else {
                    g.setColor(theme.getSnakeBodyOddColor());
                }
                g.fillRoundRect(drawX + 1, drawY + 1, size - 2, size - 2, 6, 6);

                g.setColor(new Color(255, 255, 255, 45));
                g.fillOval(drawX + 3, drawY + 3, (size - 2) / 3, (size - 2) / 3);
            }
        }
    }

    private void drawEyes(Graphics2D g, int drawX, int drawY, int size) {
        int eyeSize = 6;
        int pupilSize = 3;
        int eyeOffset = 5;

        g.setColor(Color.WHITE);
        
        int eyeLeftX = 0, eyeLeftY = 0;
        int eyeRightX = 0, eyeRightY = 0;
        int pupilOffsetX = 0, pupilOffsetY = 0;

        switch (direction) {
            case RIGHT:
                eyeLeftX = drawX + size - eyeOffset - eyeSize;
                eyeLeftY = drawY + eyeOffset;
                eyeRightX = drawX + size - eyeOffset - eyeSize;
                eyeRightY = drawY + size - eyeOffset - eyeSize;
                pupilOffsetX = 1;
                break;
            case LEFT:
                eyeLeftX = drawX + eyeOffset;
                eyeLeftY = drawY + eyeOffset;
                eyeRightX = drawX + eyeOffset;
                eyeRightY = drawY + size - eyeOffset - eyeSize;
                pupilOffsetX = -1;
                break;
            case UP:
                eyeLeftX = drawX + eyeOffset;
                eyeLeftY = drawY + eyeOffset;
                eyeRightX = drawX + size - eyeOffset - eyeSize;
                eyeRightY = drawY + eyeOffset;
                pupilOffsetY = -1;
                break;
            case DOWN:
                eyeLeftX = drawX + eyeOffset;
                eyeLeftY = drawY + size - eyeOffset - eyeSize;
                eyeRightX = drawX + size - eyeOffset - eyeSize;
                eyeRightY = drawY + size - eyeOffset - eyeSize;
                pupilOffsetY = 1;
                break;
        }

        g.setColor(Color.WHITE);
        g.fillOval(eyeLeftX, eyeLeftY, eyeSize, eyeSize);
        g.setColor(Color.BLACK);
        g.fillOval(eyeLeftX + (eyeSize - pupilSize) / 2 + pupilOffsetX, eyeLeftY + (eyeSize - pupilSize) / 2 + pupilOffsetY, pupilSize, pupilSize);

        g.setColor(Color.WHITE);
        g.fillOval(eyeRightX, eyeRightY, eyeSize, eyeSize);
        g.setColor(Color.BLACK);
        g.fillOval(eyeRightX + (eyeSize - pupilSize) / 2 + pupilOffsetX, eyeRightY + (eyeSize - pupilSize) / 2 + pupilOffsetY, pupilSize, pupilSize);
    }

    private void drawFlickeringTongue(Graphics2D g, int drawX, int drawY, int size) {
        if (System.currentTimeMillis() % 400 < 200) {
            g.setColor(new Color(239, 68, 68));
            
            int headCenterX = drawX + size / 2;
            int headCenterY = drawY + size / 2;

            int stemLength = 6;
            int forkLength = 4;

            switch (direction) {
                case RIGHT:
                    int rx = drawX + size;
                    g.drawLine(rx, headCenterY, rx + stemLength, headCenterY);
                    g.drawLine(rx + stemLength, headCenterY, rx + stemLength + forkLength, headCenterY - 3);
                    g.drawLine(rx + stemLength, headCenterY, rx + stemLength + forkLength, headCenterY + 3);
                    break;
                case LEFT:
                    g.drawLine(drawX, headCenterY, drawX - stemLength, headCenterY);
                    g.drawLine(drawX - stemLength, headCenterY, drawX - stemLength - forkLength, headCenterY - 3);
                    g.drawLine(drawX - stemLength, headCenterY, drawX - stemLength - forkLength, headCenterY + 3);
                    break;
                case UP:
                    g.drawLine(headCenterX, drawY, headCenterX, drawY - stemLength);
                    g.drawLine(headCenterX, drawY - stemLength, headCenterX - 3, drawY - stemLength - forkLength);
                    g.drawLine(headCenterX, drawY - stemLength, headCenterX + 3, drawY - stemLength - forkLength);
                    break;
                case DOWN:
                    int dy = drawY + size;
                    g.drawLine(headCenterX, dy, headCenterX, dy + stemLength);
                    g.drawLine(headCenterX, dy + stemLength, headCenterX - 3, dy + stemLength + forkLength);
                    g.drawLine(headCenterX, dy + stemLength, headCenterX + 3, dy + stemLength + forkLength);
                    break;
            }
        }
    }

    // --- GETTERS & SETTERS ---
    
    public Point getHead() {
        return body.getFirst();
    }

    public LinkedList<Point> getBody() {
        return body;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }
}
