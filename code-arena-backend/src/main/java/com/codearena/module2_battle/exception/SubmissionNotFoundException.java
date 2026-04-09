package com.codearena.module2_battle.exception;

public class SubmissionNotFoundException extends RuntimeException {

    public SubmissionNotFoundException(String submissionId) {
        super("Submission " + submissionId + " not found");
    }
}
