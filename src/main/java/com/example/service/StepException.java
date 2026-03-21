package com.example.service;

/**
 * Thrown inside {@code DeployProgressService#executeDeployment} when a specific
 * pipeline step fails. Carries the step index so the caller can emit the correct
 * {@code step-failed} SSE event.
 */
public class StepException extends RuntimeException {

    private final int stepIndex;

    public StepException(int stepIndex, String message) {
        super(message);
        this.stepIndex = stepIndex;
    }

    public StepException(int stepIndex, String message, Throwable cause) {
        super(message, cause);
        this.stepIndex = stepIndex;
    }

    public int getStepIndex() {
        return stepIndex;
    }
}


