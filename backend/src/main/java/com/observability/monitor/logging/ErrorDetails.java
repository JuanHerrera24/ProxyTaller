package com.observability.monitor.logging;

import java.util.Arrays;
import java.util.List;

/**
 * Compact description of a failure: exception type, message and a summarized stack trace that keeps only
 * the most relevant frames instead of the full JVM output.
 */
public record ErrorDetails(String type, String message, List<String> stackTrace) {

    static final int MAX_FRAMES = 6;
    private static final String APPLICATION_PACKAGE = "com.observability.monitor";

    public static ErrorDetails from(Throwable failure) {
        Throwable rootCause = rootCauseOf(failure);
        String message = rootCause.getMessage() != null ? rootCause.getMessage() : rootCause.getClass().getSimpleName();
        return new ErrorDetails(rootCause.getClass().getSimpleName(), message, summarize(rootCause));
    }

    /**
     * Prioritizes frames from the application package; falls back to the top frames when none are present.
     */
    static List<String> summarize(Throwable failure) {
        StackTraceElement[] frames = failure.getStackTrace();

        List<String> applicationFrames = Arrays.stream(frames)
                .filter(frame -> frame.getClassName().startsWith(APPLICATION_PACKAGE))
                .limit(MAX_FRAMES)
                .map(ErrorDetails::formatFrame)
                .toList();

        if (!applicationFrames.isEmpty()) {
            return applicationFrames;
        }
        return Arrays.stream(frames).limit(MAX_FRAMES).map(ErrorDetails::formatFrame).toList();
    }

    private static Throwable rootCauseOf(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private static String formatFrame(StackTraceElement frame) {
        String simpleClassName = frame.getClassName().substring(frame.getClassName().lastIndexOf('.') + 1);
        return "at %s.%s(%s:%d)".formatted(simpleClassName, frame.getMethodName(), frame.getFileName(), frame.getLineNumber());
    }
}
