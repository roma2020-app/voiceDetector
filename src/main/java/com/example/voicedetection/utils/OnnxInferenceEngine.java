/*package com.example.voicedetection.utils;

import ai.onnxruntime.*;
import jakarta.annotation.Resource;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;


public class OnnxInferenceEngine {

    private static OrtEnvironment env;
    private static OrtSession session;
    

    // 🔥 Load ONNX once (singleton)
    static {
        try {
            env = OrtEnvironment.getEnvironment();

         

            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT);
           // session = env.createSession(modelBytes, options);

            session = env.createSession("D:/RM/models/voice_ai_detector.onnx", options);
            options.close();

            System.out.println("✅ ONNX model loaded successfully");

        } catch (Exception e) {
            throw new RuntimeException("Failed to load ONNX model", e);
        }
    }

    private OnnxInferenceEngine() {}

    // 🔮 Run inference
    public static float[] run(float[][] features) throws OrtException {

        // Create input tensor
        OnnxTensor inputTensor =
                OnnxUtils.createInputTensor(env, features);

        // Get input name automatically
        String inputName = session.getInputNames().iterator().next();

        Map<String, OnnxTensor> inputs =
                Collections.singletonMap(inputName, inputTensor);

        // Run model
        OrtSession.Result result = session.run(inputs);

        // Extract output
        float[][] output =
                (float[][]) result.get(0).getValue();

        // Cleanup
        inputTensor.close();
        result.close();

        return output[0]; // e.g. [AI, HUMAN]
    }
}*/

package com.example.voicedetection.utils;

import ai.onnxruntime.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

@Component
public class OnnxInferenceEngine {

    private final OrtEnvironment env;
    private final OrtSession session;

    public OnnxInferenceEngine(
            @Value("classpath:models/voice_ai_detector.onnx") Resource onnx,
            @Value("classpath:models/voice_ai_detector.onnx.data") Resource data
    ) throws Exception {

        this.env = OrtEnvironment.getEnvironment();

        // 1️⃣ Create temp directory
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "onnx-model");
        if (!tempDir.exists()) tempDir.mkdirs();

        // 2️⃣ Copy ONNX
        File onnxFile = new File(tempDir, "voice_ai_detector.onnx");
        try (InputStream in = onnx.getInputStream();
             FileOutputStream out = new FileOutputStream(onnxFile)) {
            in.transferTo(out);
        }

        // 3️⃣ Copy ONNX.DATA
        File dataFile = new File(tempDir, "voice_ai_detector.onnx.data");
        try (InputStream in = data.getInputStream();
             FileOutputStream out = new FileOutputStream(dataFile)) {
            in.transferTo(out);
        }

        // 4️⃣ Load model via FILE PATH (required)
        OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);

        this.session = env.createSession(
                onnxFile.getAbsolutePath(),
                options
        );

        System.out.println("✅ ONNX model loaded (external data supported)");
    }

    public float[] run(float[][] features) throws OrtException {

        try (OnnxTensor input =
                     OnnxUtils.createInputTensor(env, features)) {

            String inputName = session.getInputNames().iterator().next();

            OrtSession.Result result =
                    session.run(Map.of(inputName, input));

            float[][] output =
                    (float[][]) result.get(0).getValue();

            return output[0];
        }
    }
}
