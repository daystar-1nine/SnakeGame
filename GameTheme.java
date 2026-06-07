import java.awt.Color;

/**
 * GameTheme defines the visual palettes available in the game.
 * It encapsulates color values for components so the game's visuals can be swapped dynamically.
 */
public enum GameTheme {
    MIDNIGHT(
        "Midnight Slate (Default)",
        new Color(15, 23, 42),      // Background: Rich slate-900
        new Color(30, 41, 59, 100), // Grid: semi-transparent slate
        new Color(16, 185, 129),    // Snake Head: Emerald Green
        new Color(5, 150, 105),     // Snake Body (Even)
        new Color(4, 120, 87),      // Snake Body (Odd)
        new Color(239, 68, 68),     // Food Normal: Ruby Red
        new Color(34, 197, 94),     // Food Leaf: Emerald Green
        Color.WHITE,                // HUD Text
        new Color(56, 189, 248)     // Accent / Info: Sky Blue
    ),
    CYBERPUNK(
        "Cyberpunk Neon",
        new Color(24, 12, 44),      // Background: Deep dark violet
        new Color(76, 29, 149, 100),// Grid: semi-transparent purple
        new Color(6, 182, 212),     // Snake Head: Electric Cyan
        new Color(14, 116, 144),    // Snake Body (Even)
        new Color(21, 94, 117),     // Snake Body (Odd)
        new Color(244, 63, 94),     // Food Normal: Hot Rose Pink
        new Color(236, 72, 153),    // Food Leaf: Magenta
        new Color(253, 224, 71),    // HUD Text: Lemon Yellow
        new Color(244, 63, 94)      // Accent: Hot Rose Pink
    ),
    MATRIX(
        "Matrix Console",
        new Color(0, 0, 0),         // Background: Pitch Black
        new Color(0, 50, 0, 100),   // Grid: Dark terminal green lines
        new Color(0, 255, 0),       // Snake Head: Glowing Neon Green
        new Color(0, 200, 0),       // Snake Body (Even)
        new Color(0, 150, 0),       // Snake Body (Odd)
        new Color(180, 255, 180),   // Food Normal: Light matrix green
        new Color(0, 255, 0),       // Food Leaf: Neon green
        new Color(34, 197, 94),     // HUD Text: Classic green terminal
        new Color(0, 255, 0)        // Accent: Pure green
    );

    private final String displayName;
    private final Color backgroundColor;
    private final Color gridColor;
    private final Color snakeHeadColor;
    private final Color snakeBodyEvenColor;
    private final Color snakeBodyOddColor;
    private final Color foodColor;
    private final Color leafColor;
    private final Color hudColor;
    private final Color accentColor;

    GameTheme(String displayName, Color backgroundColor, Color gridColor, Color snakeHeadColor,
              Color snakeBodyEvenColor, Color snakeBodyOddColor, Color foodColor, Color leafColor,
              Color hudColor, Color accentColor) {
        this.displayName = displayName;
        this.backgroundColor = backgroundColor;
        this.gridColor = gridColor;
        this.snakeHeadColor = snakeHeadColor;
        this.snakeBodyEvenColor = snakeBodyEvenColor;
        this.snakeBodyOddColor = snakeBodyOddColor;
        this.foodColor = foodColor;
        this.leafColor = leafColor;
        this.hudColor = hudColor;
        this.accentColor = accentColor;
    }

    // --- GETTERS ---
    
    public String getDisplayName() { return displayName; }
    public Color getBackgroundColor() { return backgroundColor; }
    public Color getGridColor() { return gridColor; }
    public Color getSnakeHeadColor() { return snakeHeadColor; }
    public Color getSnakeBodyEvenColor() { return snakeBodyEvenColor; }
    public Color getSnakeBodyOddColor() { return snakeBodyOddColor; }
    public Color getFoodColor() { return foodColor; }
    public Color getLeafColor() { return leafColor; }
    public Color getHudColor() { return hudColor; }
    public Color getAccentColor() { return accentColor; }
}
