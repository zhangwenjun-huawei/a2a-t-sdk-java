package net.openan.a2at.sdk.prompt.taskrendering.exception;

import net.openan.a2at.sdk.core.exception.SdkException;

/**
 * Raised when task prompt template rendering fails.
 *
 * @since 2026-06
 */
public final class TaskPromptRenderException extends SdkException {

    /**
     * Creates a task prompt rendering exception.
     *
     * @param message failure message
     */
    public TaskPromptRenderException(String message) {
        super(message);
    }
}
