package com.bipin.objectdetector;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.onnxruntime.*;

public class YoloOnnxDetector {

    private static final String TAG = "YoloOnnxDetector";

    private final OrtEnvironment env;
    private final OrtSession session;

    private static final int INPUT_SIZE = 640;

    public static class Detection {
        public float x, y, w, h, confidence;
        public int classId;

        public Detection(float x, float y, float w, float h, float confidence, int classId) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.confidence = confidence;
            this.classId = classId;
        }
    }

    public YoloOnnxDetector(AssetManager assetManager, String modelPath) throws Exception {

        env = OrtEnvironment.getEnvironment();

        OrtSession.SessionOptions options = new OrtSession.SessionOptions();

        byte[] modelBytes = loadModelBytes(assetManager, modelPath);

        session = env.createSession(modelBytes, options);

        Log.d(TAG, "Model loaded");

        for (String name : session.getInputNames()) {
            Log.d(TAG, "Input: " + name);
        }
    }

    // =========================
    // MAIN DETECT FUNCTION
    // =========================
    public List<Detection> detect(Bitmap bitmap) throws Exception {

        Bitmap resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);

        float[] input = preprocess(resized);

        FloatBuffer buffer = ByteBuffer
                .allocateDirect(input.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        buffer.put(input);
        buffer.rewind();

        long[] shape = new long[]{1, 3, INPUT_SIZE, INPUT_SIZE};

        OnnxTensor tensor = OnnxTensor.createTensor(env, buffer, shape);

        String inputName = session.getInputNames().iterator().next();

        Map<String, OnnxTensor> inputs = new HashMap<>();
        inputs.put(inputName, tensor);

        OrtSession.Result result = session.run(inputs);

        tensor.close();

        // 🔥 IMPORTANT: convert raw output → detections
        return nms(parseOutput(result));
    }

    // =========================
    // OUTPUT PARSER FOR YOLOv8
    // =========================
    private List<Detection> parseOutput(OrtSession.Result result) {
        List<Detection> detections = new ArrayList<>();
        try {
            OnnxValue value = result.get(0);
            float[][][] output = (float[][][]) value.getValue();
            long[] shape = ((TensorInfo) value.getInfo()).getShape();

            int numBoxes;
            int numElements;
            boolean isTransposed = false;

            if (shape[1] == 8400 || shape[1] > shape[2]) {
                // Shape is [1, 8400, 84]
                numBoxes = (int) shape[1];
                numElements = (int) shape[2];
                isTransposed = true;
            } else {
                // Shape is [1, 84, 8400]
                numBoxes = (int) shape[2];
                numElements = (int) shape[1];
                isTransposed = false;
            }

            for (int i = 0; i < numBoxes; i++) {
                float x, y, w, h;
                float maxScore = 0f;
                int classId = -1;

                if (isTransposed) {
                    x = output[0][i][0];
                    y = output[0][i][1];
                    w = output[0][i][2];
                    h = output[0][i][3];

                    for (int c = 4; c < numElements; c++) {
                        float score = output[0][i][c];
                        if (score > maxScore) {
                            maxScore = score;
                            classId = c - 4;
                        }
                    }
                } else {
                    x = output[0][0][i];
                    y = output[0][1][i];
                    w = output[0][2][i];
                    h = output[0][3][i];

                    for (int c = 4; c < numElements; c++) {
                        float score = output[0][c][i];
                        if (score > maxScore) {
                            maxScore = score;
                            classId = c - 4;
                        }
                    }
                }

                if (maxScore > 0.40f) { // Confidence threshold (lowered to 0.40 to capture detections)
                    // YOLO outputs center x,y. Convert to top-left x,y.
                    float left = x - w / 2f;
                    float top = y - h / 2f;
                    detections.add(new Detection(left, top, w, h, maxScore, classId));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Parse error: " + e.getMessage());
            e.printStackTrace();
        }
        return detections;
    }

    // =========================
    // NON-MAXIMUM SUPPRESSION (NMS)
    // =========================
    private List<Detection> nms(List<Detection> detections) {
        List<Detection> result = new ArrayList<>();
        // Sort by confidence descending
        detections.sort((d1, d2) -> Float.compare(d2.confidence, d1.confidence));

        while (!detections.isEmpty()) {
            Detection best = detections.remove(0);
            result.add(best);

            // Remove overlapping boxes with IoU threshold of 0.45
            detections.removeIf(next -> boxIoU(best, next) > 0.45f);
        }
        return result;
    }

    private float boxIoU(Detection a, Detection b) {
        float ax1 = a.x;
        float ay1 = a.y;
        float ax2 = a.x + a.w;
        float ay2 = a.y + a.h;

        float bx1 = b.x;
        float by1 = b.y;
        float bx2 = b.x + b.w;
        float by2 = b.y + b.h;

        float intersectionLeft = Math.max(ax1, bx1);
        float intersectionTop = Math.max(ay1, by1);
        float intersectionRight = Math.min(ax2, bx2);
        float intersectionBottom = Math.min(ay2, by2);

        if (intersectionLeft >= intersectionRight || intersectionTop >= intersectionBottom) {
            return 0f;
        }

        float intersectionArea = (intersectionRight - intersectionLeft) * (intersectionBottom - intersectionTop);
        float areaA = a.w * a.h;
        float areaB = b.w * b.h;

        return intersectionArea / (areaA + areaB - intersectionArea);
    }

    // =========================
    // PREPROCESS
    // =========================
    private float[] preprocess(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        float[] output = new float[3 * width * height];
        int r = 0, g = width * height, b = 2 * width * height;

        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            output[r++] = ((p >> 16) & 0xFF) / 255f;
            output[g++] = ((p >> 8) & 0xFF) / 255f;
            output[b++] = (p & 0xFF) / 255f;
        }
        return output;
    }

    private byte[] loadModelBytes(AssetManager assetManager, String modelPath) throws IOException {
        try (java.io.InputStream is = assetManager.open(modelPath)) {
            java.io.ByteArrayOutputStream byteBuffer = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[1024 * 1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            return byteBuffer.toByteArray();
        }
    }
}