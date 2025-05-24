package ulb.utils;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for extracting dominant colors from images.
 */
public class ColorExtractor {

    /**
     * Extracts the dominant color from an image.
     *
     * @param image The image to extract the dominant color from
     * @return The dominant color as a Color object
     */
    public static Color extractDominantColor(Image image) {
        if (image == null || image.isError()) {
            return Color.web("#404040"); // Default color if image is null or has errors
        }

        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        
        // For large images, sample fewer pixels for performance
        int sampleSize = Math.max(1, Math.min(width, height) / 50);
        
        PixelReader pixelReader = image.getPixelReader();
        Map<Integer, Integer> colorCounts = new HashMap<>();
        
        // Sample pixels from the image
        for (int y = 0; y < height; y += sampleSize) {
            for (int x = 0; x < width; x += sampleSize) {
                Color color = pixelReader.getColor(x, y);
                
                // Convert to RGB int for easier counting
                int rgb = colorToRgbInt(color);
                
                // Count occurrences of each color
                colorCounts.put(rgb, colorCounts.getOrDefault(rgb, 0) + 1);
            }
        }
        
        // Find the most frequent color
        int dominantRgb = 0;
        int maxCount = 0;
        
        for (Map.Entry<Integer, Integer> entry : colorCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                dominantRgb = entry.getKey();
            }
        }
        
        // Convert back to Color
        return rgbIntToColor(dominantRgb);
    }
    
    /**
     * Creates a gradient CSS style string based on the dominant color.
     *
     * @param dominantColor The dominant color to base the gradient on
     * @return A CSS style string for a gradient background
     */
    public static String createGradientStyle(Color dominantColor) {
        // Darken the color for the gradient end
        Color darkened = dominantColor.darker().darker();
        
        // Create a gradient style string
        return String.format("-fx-background-color: linear-gradient(to bottom, %s, %s);",
                colorToHex(dominantColor),
                colorToHex(darkened));
    }
    
    /**
     * Converts a Color object to a hexadecimal string representation.
     *
     * @param color The color to convert
     * @return A hexadecimal string representation of the color
     */
    private static String colorToHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
    
    /**
     * Converts a Color object to an RGB integer.
     *
     * @param color The color to convert
     * @return An RGB integer representation of the color
     */
    private static int colorToRgbInt(Color color) {
        int r = (int) (color.getRed() * 255);
        int g = (int) (color.getGreen() * 255);
        int b = (int) (color.getBlue() * 255);
        
        return (r << 16) | (g << 8) | b;
    }
    
    /**
     * Converts an RGB integer to a Color object.
     *
     * @param rgb The RGB integer to convert
     * @return A Color object representation of the RGB integer
     */
    private static Color rgbIntToColor(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        
        return Color.rgb(r, g, b);
    }
}