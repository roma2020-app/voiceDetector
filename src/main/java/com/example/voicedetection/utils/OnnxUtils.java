package com.example.voicedetection.utils;

import ai.onnxruntime.*;
import java.nio.FloatBuffer;

public class OnnxUtils {

    private OnnxUtils() {} // prevent instantiation

    public static OnnxTensor createInputTensor(
            OrtEnvironment env,
            float[][] features   // [300][39]
    ) throws OrtException {

        int frames = features.length;
        int dims   = features[0].length;

        float[] flat = new float[frames * dims];
        int index = 0;

        for (int i = 0; i < frames; i++) {
            for (int j = 0; j < dims; j++) {
                flat[index++] = features[i][j];
            }
        }

        FloatBuffer buffer = FloatBuffer.wrap(flat);
        long[] shape = new long[]{1, frames, dims};

        return OnnxTensor.createTensor(env, buffer, shape);
    }
}
