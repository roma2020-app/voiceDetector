package com.example.voicedetection.utils;

public class MathUtils {

    private MathUtils() {}

    public static float[] softmax(float[] logits) {

        float max = Float.NEGATIVE_INFINITY;
        for (float v : logits) {
            max = Math.max(max, v);
        }

        float sum = 0f;
        float[] exp = new float[logits.length];

        for (int i = 0; i < logits.length; i++) {
            exp[i] = (float) Math.exp(logits[i] - max);
            sum += exp[i];
        }

        for (int i = 0; i < exp.length; i++) {
            exp[i] /= sum;
        }

        return exp;
    }
}
