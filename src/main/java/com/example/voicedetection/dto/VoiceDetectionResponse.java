package com.example.voicedetection.dto;

public class VoiceDetectionResponse {

    private String status;
    private String language;
    private String classification;   // AI_GENERATED | HUMAN
    private double confidenceScore;   // 0.0 – 1.0
    private String explanation;

    public VoiceDetectionResponse(
            String status,
            String language,
            String classification,
            double confidenceScore,
            String explanation) {

        this.status = status;
        this.language = language;
        this.classification = classification;
        this.confidenceScore = confidenceScore;
        this.explanation = explanation;
    }

    public String getStatus() {
        return status;
    }

    public String getLanguage() {
        return language;
    }

    public String getClassification() {
        return classification;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public String getExplanation() {
        return explanation;
    }
}
