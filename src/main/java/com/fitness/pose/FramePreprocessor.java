package com.fitness.pose;

/**
 * Utility class for preprocessing camera frames for ONNX MoveNet inference.
 */
public class FramePreprocessor {

    /**
     * A preprocessed frame containing the RGB float data and metadata to map
     * model coordinates back to the original frame.
     */
    public static class PreprocessedFrame {
        public final float[] rgbData;
        private final double scale;
        private final double xOffset;
        private final double yOffset;
        private final int sourceWidth;
        private final int sourceHeight;
        private final int targetSize;

        public PreprocessedFrame(float[] rgbData, double scale, double xOffset, double yOffset, 
                                 int sourceWidth, int sourceHeight, int targetSize) {
            this.rgbData = rgbData;
            this.scale = scale;
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.sourceWidth = sourceWidth;
            this.sourceHeight = sourceHeight;
            this.targetSize = targetSize;
        }

        /**
         * Maps model output coordinates (normalized 0-1 in the square input) 
         * back to normalized coordinates in the original frame, undoing letterbox padding.
         * 
         * @param x The normalized x coordinate [0.0 - 1.0] in the model output.
         * @param y The normalized y coordinate [0.0 - 1.0] in the model output.
         * @return An array containing the normalized [x, y] coordinates in the original source frame.
         */
        public double[] mapBackToSourceNormalized(double x, double y) {
            double absXTarget = x * targetSize;
            double absYTarget = y * targetSize;
            
            double absXSource = (absXTarget - xOffset) / scale;
            double absYSource = (absYTarget - yOffset) / scale;
            
            double normXSource = absXSource / sourceWidth;
            double normYSource = absYSource / sourceHeight;
            
            return new double[] { normXSource, normYSource };
        }
    }

    /**
     * Resizes a RawFrame to fit within a target square while maintaining aspect ratio,
     * pads with black (letterboxing), and converts from BGR to RGB float array.
     * 
     * @param frame The original captured frame.
     * @param targetSize The size of the square target for inference.
     * @return A PreprocessedFrame ready for ONNX model inference.
     */
    public static PreprocessedFrame letterboxResize(RawFrame frame, int targetSize) {
        int srcWidth = frame.getWidth();
        int srcHeight = frame.getHeight();
        byte[] bgrData = frame.getBgrData();
        
        double scale = Math.min((double) targetSize / srcWidth, (double) targetSize / srcHeight);
        int newWidth = (int) Math.round(srcWidth * scale);
        int newHeight = (int) Math.round(srcHeight * scale);
        
        double xOffset = (targetSize - newWidth) / 2.0;
        double yOffset = (targetSize - newHeight) / 2.0;
        
        float[] rgbData = new float[targetSize * targetSize * 3];
        
        // Bilinear interpolation and BGR to RGB conversion
        for (int yTarget = 0; yTarget < newHeight; yTarget++) {
            for (int xTarget = 0; xTarget < newWidth; xTarget++) {
                double srcX = xTarget / scale;
                double srcY = yTarget / scale;
                
                int x1 = (int) Math.floor(srcX);
                int y1 = (int) Math.floor(srcY);
                int x2 = Math.min(x1 + 1, srcWidth - 1);
                int y2 = Math.min(y1 + 1, srcHeight - 1);
                
                double dx = srcX - x1;
                double dy = srcY - y1;
                
                int idx11 = (y1 * srcWidth + x1) * 3;
                int idx21 = (y1 * srcWidth + x2) * 3;
                int idx12 = (y2 * srcWidth + x1) * 3;
                int idx22 = (y2 * srcWidth + x2) * 3;
                
                int destY = yTarget + (int) yOffset;
                int destX = xTarget + (int) xOffset;
                int destIdx = (destY * targetSize + destX) * 3;
                
                for (int c = 0; c < 3; c++) {
                    int srcC = 2 - c; // BGR to RGB mapping
                    
                    float v11 = bgrData[idx11 + srcC] & 0xFF;
                    float v21 = bgrData[idx21 + srcC] & 0xFF;
                    float v12 = bgrData[idx12 + srcC] & 0xFF;
                    float v22 = bgrData[idx22 + srcC] & 0xFF;
                    
                    float val = (float) (
                        v11 * (1 - dx) * (1 - dy) +
                        v21 * dx * (1 - dy) +
                        v12 * (1 - dx) * dy +
                        v22 * dx * dy
                    );
                    
                    rgbData[destIdx + c] = val;
                }
            }
        }
        
        return new PreprocessedFrame(rgbData, scale, xOffset, yOffset, srcWidth, srcHeight, targetSize);
    }
}
