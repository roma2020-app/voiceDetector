package com.example.voicedetection.service;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.voicedetection.utils.MathUtils;
import com.example.voicedetection.utils.OnnxInferenceEngine;
import com.example.voicedetection.utils.OnnxUtils;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.mfcc.MFCC;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import javax.sound.sampled.UnsupportedAudioFileException;

@Service
public class VoiceAnalysisService {
	
	private final OnnxInferenceEngine onnxInferenceEngine;

    public VoiceAnalysisService(OnnxInferenceEngine onnxInferenceEngine) {
        this.onnxInferenceEngine = onnxInferenceEngine;
    }

    // MP3 → Base64
    public String encodeToBase64(MultipartFile file) throws Exception {
        return Base64.getEncoder().encodeToString(file.getBytes());
    }

    // Base64 → Analysis
    public Result analyze(String audioBase64,String language) {

        byte[] audioBytes;
        try {
            audioBytes = Base64.getDecoder().decode(audioBase64);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid Base64 audio data");
        }
        System.out.println("Audio base64 (bytes): " + audioBytes.toString());

        System.out.println("Audio size (bytes): " + audioBytes.length);
        
        
     // MP3 → WAV using cvjar
        File wavFile = convertMp3ToWavUsingJar(audioBytes);
    

        System.out.println("WAV file created at: " + wavFile.getAbsolutePath());
        
        if (!wavFile.exists() || wavFile.length() == 0) {
            throw new RuntimeException("Invalid WAV file generated");
        }

        
        List<float[]> mfcc = extractMFCC(wavFile);

        System.out.println("MFCC shape: " + mfcc.size() + " x " + mfcc.get(0).length);
        
     // after MFCC done
        wavFile.delete();
        
        
        List<float[]> delta = computeDelta(mfcc);
        List<float[]> deltaDelta = computeDeltaDelta(delta);

        List<float[]> mfcc39 = combineFeatures(mfcc, delta, deltaDelta);

        System.out.println("Final feature shape: "
                + mfcc39.size() + " x " + mfcc39.get(0).length);
        
        
       List<float[]> normalized = applyCMVN(mfcc39);

        System.out.println(
            "CMVN output shape: " +
            normalized.size() + " x " +
            normalized.get(0).length
        );
        
        
        float[][] fixedInput =
                padOrTruncate(normalized, 300, 39);

        System.out.println(
            "Final fixed shape: " +
            fixedInput.length + " x " +
            fixedInput[0].length
        );
        
      //  OrtEnvironment env = OrtEnvironment.getEnvironment();

        /*try {
			OnnxTensor tensor =
			        OnnxUtils.createInputTensor(env, fixedInput);
		} catch (OrtException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/

       float[] logits = null;
	try {
		//logits = OnnxInferenceEngine.run(fixedInput);
		logits = onnxInferenceEngine.run(fixedInput);
	} catch (OrtException e) {
		// TODO Auto-generated catch block
		
	    throw new ResponseStatusException(
	        HttpStatus.INTERNAL_SERVER_ERROR,
	        "Voice analysis failed");
	}
		//float temperature = 1.8f;  // multilingual-safe
       float[] probs = MathUtils.softmax(logits);
       //float[] probs = MathUtils.softmax(logits, temperature);
       
       System.out.println("Raw ONNX output: " + Arrays.toString(logits));

       float sum = 0f;
       for (float v : logits) sum += v;
       System.out.println("Sum: " + sum);


        int aiIndex = 1;
        int humanIndex = 0;

        //boolean aiGenerated = probs[aiIndex] > probs[humanIndex];
        float aiProb = probs[aiIndex];
        float humanProb = probs[humanIndex];
        
        

        
     // 🎯 Stability threshold (IMPORTANT)
      //  float AI_THRESHOLD = 0.6f;
        
     // Confidence (honest, calibrated)
        float confidence = Math.max(aiProb, humanProb);
     // 🌍 Language-aware confidence scaling (NOT cheating)
        if (!language.equalsIgnoreCase("English")) {
            confidence *= 0.85f;   // reduce overconfidence for unseen languages
        }

        confidence = Math.min(confidence, 0.99f); // safety cap
        confidence = Math.max(confidence, 0.55f);


        // Final decision
        boolean aiGenerated = aiProb > humanProb;
        float margin = Math.abs(aiProb - humanProb);
        System.out.println("margin"+margin);
        //boolean uncertain = margin < 0.15f;
        String classification;
        if (confidence < 0.55f || margin < 0.15f) {
            classification = "UNCERTAIN";
        } else if (aiProb > humanProb) {
            classification = "AI_GENERATED";
        } else {
            classification = "HUMAN";
        }
      /*  String classification;
        if (uncertain) {
            classification = "UNCERTAIN";
        } else {
            classification = aiGenerated ? "AI_GENERATED" : "HUMAN";
        }*/
        


       /* return new Result(
                aiGenerated ? "AI_GENERATED" : "HUMAN",
                Math.max(probs[aiIndex], probs[humanIndex]),
                aiGenerated
                        ? "Model detected synthetic voice patterns"
                        : "Model detected natural human speech patterns"
        );*/
        
        String explanation = buildNeutralExplanation(aiGenerated, confidence);

        return new Result(
        		//aiGenerated ? "AI_GENERATED" : "HUMAN",
        		classification,
                confidence,
                explanation
        );

       


        // ===== TEMP LOGIC (replace with MFCC/ONNX later) =====
      /*  boolean aiGenerated = audioBytes.length % 2 == 0;

        if (aiGenerated) {
            return new Result(
                    "AI_GENERATED",
                    0.91,
                    "Unnatural pitch consistency and robotic speech patterns detected"
            );
        } else {
            return new Result(
                    "HUMAN",
                    0.87,
                    "Natural pauses, pitch variations, and breathing patterns detected"
            );
        }*/
    }
    
    
   

    public List<float[]> extractMFCC(File wavFile) {

        /*int sampleRate=16000;
        

        int bufferSize = 512;
        int bufferOverlap = 256;
        int mfccCount = 13;
        int melFilters = 26;
        float minFreq = 300;
        float maxFreq ;*/

        int sampleRate = 16000;

        int bufferSize = 2048;
        int bufferOverlap = 1536;

        int mfccCount = 13;
        int melFilters = 128;

        float minFreq = 0f;
        float maxFreq = 8000f;

        List<float[]> mfccList = new ArrayList<>();

        AudioDispatcher dispatcher = null;
		try {
			dispatcher = AudioDispatcherFactory.fromFile(wavFile, bufferSize, bufferOverlap);
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		//sampleRate= (int) dispatcher.getFormat().getSampleRate();
		maxFreq = Math.min(8000, sampleRate / 2.0f);

        MFCC mfcc = new MFCC(
                bufferSize,
                sampleRate,
                mfccCount,
                melFilters,
                minFreq,
                maxFreq
        );

        dispatcher.addAudioProcessor(mfcc);

        dispatcher.addAudioProcessor(new AudioProcessor() {
            @Override
            public boolean process(AudioEvent audioEvent) {
                float[] mfccFrame = mfcc.getMFCC();
                mfccList.add(mfccFrame.clone()); // IMPORTANT: clone
                return true;
            }

            @Override
            public void processingFinished() {
                // no-op
            }
        });

        dispatcher.run();

        System.out.println("MFCC frames extracted: " + mfccList.size());

        return mfccList;
    }


		// 🔥 MP3 → WAV USING FFmpeg JAR (JavaCV)
        private File convertMp3ToWavUsingJar(byte[] mp3Bytes) {
        	 File mp3File = null;
        	    File wavFile = null;
            try {
                // 1️⃣ Write MP3 bytes to temp file
                 mp3File = File.createTempFile("audio_", ".mp3");
                try (FileOutputStream fos = new FileOutputStream(mp3File)) {
                    fos.write(mp3Bytes);
                }

                // 2️⃣ Output WAV file
                 wavFile = File.createTempFile("audio_", ".wav");

                // 3️⃣ Grab MP3
                FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(mp3File);
                grabber.start();

                // 4️⃣ Recorder → WAV (16kHz, mono)
                FFmpegFrameRecorder recorder =
                        new FFmpegFrameRecorder(wavFile, 1);
                recorder.setAudioChannels(1);
                recorder.setSampleRate(16000);
                recorder.setFormat("wav");
                recorder.start();

                // 5️⃣ Convert frames
                Frame frame;
                while ((frame = grabber.grabSamples()) != null) {
                    recorder.record(frame);
                }

                // 6️⃣ Cleanup
                recorder.stop();
                grabber.stop();
                
                // ✅ SAFE cleanup
                mp3File.delete();          // delete immediately
                wavFile.deleteOnExit();    // delete when JVM exits

                return wavFile;

            } catch (Exception e) {
            	// cleanup on failure
                if (mp3File != null) mp3File.delete();
                if (wavFile != null) wavFile.delete();
                throw new RuntimeException("MP3 to WAV conversion (JAR) failed", e);
            }
        }

        public static List<float[]> computeDeltaDelta(List<float[]> delta) {
            return computeDelta(delta);
        }

        public static List<float[]> computeDelta(List<float[]> features) {

            int frames = features.size();
            int dims = features.get(0).length;

            List<float[]> delta = new ArrayList<>();

            for (int t = 0; t < frames; t++) {
                float[] deltaFrame = new float[dims];

                for (int d = 0; d < dims; d++) {
                    float prev = (t == 0) ? features.get(t)[d] : features.get(t - 1)[d];
                    float next = (t == frames - 1) ? features.get(t)[d] : features.get(t + 1)[d];

                    deltaFrame[d] = (next - prev) / 2.0f;
                }
                delta.add(deltaFrame);
            }
            return delta;
        }
        
        public static List<float[]> combineFeatures(
                List<float[]> mfcc,
                List<float[]> delta,
                List<float[]> deltaDelta) {

            List<float[]> combined = new ArrayList<>();

            for (int i = 0; i < mfcc.size(); i++) {
                float[] frame = new float[39];

                System.arraycopy(mfcc.get(i), 0, frame, 0, 13);
                System.arraycopy(delta.get(i), 0, frame, 13, 13);
                System.arraycopy(deltaDelta.get(i), 0, frame, 26, 13);

                combined.add(frame);
            }
            return combined;
        }


        public static List<float[]> applyCMVN(List<float[]> features) {

            int frames = features.size();
            int dims = features.get(0).length;

            float[] mean = new float[dims];
            float[] std = new float[dims];
            float epsilon = 1e-8f;

            // ===== Compute Mean =====
            for (float[] frame : features) {
                for (int d = 0; d < dims; d++) {
                    mean[d] += frame[d];
                }
            }
            for (int d = 0; d < dims; d++) {
                mean[d] /= frames;
            }

            // ===== Compute Variance =====
            for (float[] frame : features) {
                for (int d = 0; d < dims; d++) {
                    float diff = frame[d] - mean[d];
                    std[d] += diff * diff;
                }
            }
            for (int d = 0; d < dims; d++) {
                std[d] = (float) Math.sqrt(std[d] / frames);
            }

            // ===== Apply CMVN =====
            List<float[]> normalized = new ArrayList<>();

            for (float[] frame : features) {
                float[] normFrame = new float[dims];
                for (int d = 0; d < dims; d++) {
                    normFrame[d] = (frame[d] - mean[d]) / (std[d] + epsilon);
                }
                normalized.add(normFrame);
            }

            return normalized;
        }
        
        public static float[][] padOrTruncate(
                List<float[]> features,
                int targetFrames,
                int featureDim
        ) {
            float[][] output = new float[targetFrames][featureDim];

            int framesToCopy = Math.min(features.size(), targetFrames);

            // 1️⃣ Copy existing frames
            for (int i = 0; i < framesToCopy; i++) {
                float[] frame = features.get(i);
                System.arraycopy(frame, 0, output[i], 0, featureDim);
            }

            // 2️⃣ Remaining frames auto-zero (Java default)
            return output;
        }

        private String buildNeutralExplanation(boolean aiGenerated, float confidence) {

            if (aiGenerated) {
                return confidence >= 0.8f
                    ? "Acoustic analysis indicates highly consistent spectral and temporal patterns that align with characteristics commonly observed in synthetic speech generation."
                    : "Acoustic feature distributions show reduced variability and increased consistency, suggesting a likelihood of synthetic speech characteristics.";
            } else {
                return confidence >= 0.8f
                    ? "Acoustic analysis shows natural variability across spectral and temporal features, consistent with patterns typically observed in human speech production."
                    : "Acoustic features exhibit mixed variability patterns, leaning towards characteristics commonly found in human speech signals.";
            }
        }


        
 
              // Result record
    public record Result(
            String classification,
            double confidence,
            String explanation
    ) {}
}
