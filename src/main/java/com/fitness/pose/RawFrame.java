package com.fitness.pose;

/**
 * Immutable data class representing a captured camera frame.
 * Holds the raw BGR byte data and metadata for the frame.
 */
public class RawFrame {
    private final int width;
    private final int height;
    private final byte[] bgrData;
    private final long timestampNanos;

    /**
     * Constructs a new RawFrame.
     * 
     * @param width The width of the frame in pixels.
     * @param height The height of the frame in pixels.
     * @param bgrData The interleaved BGR byte data of the frame.
     * @param timestampNanos The timestamp of the frame in nanoseconds.
     */
    public RawFrame(int width, int height, byte[] bgrData, long timestampNanos) {
        this.width = width;
        this.height = height;
        this.bgrData = bgrData;
        this.timestampNanos = timestampNanos;
    }

    /**
     * @return The width of the frame.
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return The height of the frame.
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return The raw BGR data array.
     */
    public byte[] getBgrData() {
        return bgrData;
    }

    /**
     * @return The timestamp in nanoseconds when this frame was captured.
     */
    public long getTimestampNanos() {
        return timestampNanos;
    }
}
