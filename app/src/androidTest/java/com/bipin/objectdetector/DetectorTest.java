package com.bipin.objectdetector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DetectorTest {

    @Test
    public void testInference() throws Exception {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Initialize detector
        YoloOnnxDetector detector = new YoloOnnxDetector(appContext.getAssets(), "yolov8n.onnx");
        assertNotNull(detector);

        // Load test image
        InputStream is = appContext.getAssets().open("test_image.jpg");
        Bitmap bitmap = BitmapFactory.decodeStream(is);
        assertNotNull(bitmap);
        is.close();

        // Run inference
        List<YoloOnnxDetector.Detection> detections = detector.detect(bitmap);

        // Log results
        android.util.Log.d("DetectorTest", "Detections count: " + detections.size());
        for (YoloOnnxDetector.Detection d : detections) {
            android.util.Log.d("DetectorTest", "Found class: " + d.classId + " with confidence: " + d.confidence);
        }

        // We expect at least one detection (the person/people in the bus image)
        assertTrue("Expected to detect at least one person in the test image", detections.size() > 0);
    }
}
