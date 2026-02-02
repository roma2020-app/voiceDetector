package com.example.voicedetection.controller;

import com.example.voicedetection.dto.VoiceDetectionRequest;
import com.example.voicedetection.dto.VoiceDetectionResponse;
import com.example.voicedetection.service.VoiceAnalysisService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

@RestController
@RequestMapping("/api")
public class VoiceDetectionController {

    private final VoiceAnalysisService service;

    public VoiceDetectionController(VoiceAnalysisService service) {
        this.service = service;
    }

    /* =========================================================
       1️⃣ JSON + Base64 API (MAIN – as per assignment)
       ========================================================= */
    @PostMapping(
            value = "/voice-detection",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public VoiceDetectionResponse detectVoiceJson(
            @RequestBody VoiceDetectionRequest request) {

        String base64Audio = request.getAudioBase64();

        var result = service.analyze(base64Audio,request.getLanguage());

        return new VoiceDetectionResponse(
                "success",
                request.getLanguage(),
                result.classification(),
                result.confidence(),
                result.explanation()
        );
    }

    /* =========================================================
       2️⃣ MP3 Upload API (OPTIONAL – convenience)
       ========================================================= */
    @PostMapping(
            value = "/voice-detection/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public VoiceDetectionResponse detectVoiceUpload(
           @RequestParam String language,
            @RequestParam MultipartFile audio) throws IOException {

        // MP3 validation
        if (audio.isEmpty()) {
            throw new IllegalArgumentException("Audio file is required");
        }

        if (!audio.getOriginalFilename().toLowerCase().endsWith(".mp3")) {
            throw new IllegalArgumentException("Only MP3 files are allowed");
        }

        // Convert MP3 → Base64
        byte[] audioBytes = audio.getBytes();
        String base64Audio = Base64.getEncoder().encodeToString(audioBytes);

        var result = service.analyze(base64Audio,language);

        return new VoiceDetectionResponse(
                "success",
                language,
                result.classification(),
                result.confidence(),
                result.explanation()
        );
    }
}
