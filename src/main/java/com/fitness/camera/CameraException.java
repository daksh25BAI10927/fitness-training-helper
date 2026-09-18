package com.fitness.camera;

/** Thrown when the webcam can't be opened, configured, or read from. */
public class CameraException extends Exception {

    private static final long serialVersionUID = 1L;

    public CameraException(String message) {
        super(message);
    }

    public CameraException(String message, Throwable cause) {
        super(message, cause);
    }
}
