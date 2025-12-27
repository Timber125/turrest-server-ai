package be.lefief.game;

/**
 * Defines 16 visually distinct colors for player identification.
 */
public class PlayerColors {
    public static final String[] COLORS = {
        "#E53935",  // 0: Red
        "#1E88E5",  // 1: Blue
        "#43A047",  // 2: Green
        "#FB8C00",  // 3: Orange
        "#8E24AA",  // 4: Purple
        "#00ACC1",  // 5: Cyan
        "#FFB300",  // 6: Amber
        "#D81B60",  // 7: Pink
        "#5E35B1",  // 8: Deep Purple
        "#00897B",  // 9: Teal
        "#7CB342",  // 10: Light Green
        "#F4511E",  // 11: Deep Orange
        "#3949AB",  // 12: Indigo
        "#C0CA33",  // 13: Lime
        "#6D4C41",  // 14: Brown
        "#546E7A"   // 15: Blue Grey
    };

    public static String getColor(int playerNumber) {
        return COLORS[playerNumber % COLORS.length];
    }
}
