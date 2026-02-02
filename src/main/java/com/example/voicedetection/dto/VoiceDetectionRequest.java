package com.example.voicedetection.dto;

import jakarta.validation.constraints.NotBlank;

public class VoiceDetectionRequest {

    @NotBlank(message = "language is required")
    private String language;

    @NotBlank(message = "audioFormat is required")
    private String audioFormat;

    @NotBlank(message = "audioBase64 is required")
    private String audioBase64;

    // ===== GETTERS & SETTERS =====

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getAudioFormat() {
        return audioFormat;
    }

    public void setAudioFormat(String audioFormat) {
        this.audioFormat = audioFormat;
    }

    public String getAudioBase64() {
        return audioBase64;
    }

    public void setAudioBase64(String audioBase64) {
        this.audioBase64 = audioBase64;
    }
}
