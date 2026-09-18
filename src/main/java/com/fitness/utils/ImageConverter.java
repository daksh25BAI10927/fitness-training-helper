package com.fitness.utils;

import com.fitness.pose.RawFrame;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

/**
 * Converts a camera-agnostic {@link RawFrame} (interleaved BGR bytes,
 * matching OpenCV's native {@code Mat} layout) into a JavaFX {@link WritableImage}
 * for the camera preview.
 *
 * <p>JavaFX does not provide a BGR {@link PixelFormat}, so this class
 * swaps the Blue and Red channels in-place before writing via
 * {@link PixelFormat#getByteRgbInstance()}.</p>
 */
public final class ImageConverter {

    private ImageConverter() {}

    /**
     * Converts a {@link RawFrame} with BGR byte data into a JavaFX {@link WritableImage}.
     *
     * @param frame the raw camera frame (BGR interleaved)
     * @return a new WritableImage suitable for rendering on a JavaFX Canvas
     */
    public static WritableImage toFxImage(RawFrame frame) {
        int width = frame.getWidth();
        int height = frame.getHeight();
        byte[] bgr = frame.getBgrData();

        // Swap BGR → RGB in a copy so we don't mutate the caller's array
        byte[] rgb = new byte[bgr.length];
        for (int i = 0; i < bgr.length; i += 3) {
            rgb[i]     = bgr[i + 2]; // R ← B
            rgb[i + 1] = bgr[i + 1]; // G ← G
            rgb[i + 2] = bgr[i];     // B ← R
        }

        WritableImage image = new WritableImage(width, height);
        image.getPixelWriter().setPixels(
                0, 0, width, height,
                PixelFormat.getByteRgbInstance(),
                rgb, 0, width * 3);
        return image;
    }
}
