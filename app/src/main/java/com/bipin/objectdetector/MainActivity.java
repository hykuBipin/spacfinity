package com.bipin.objectdetector;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

 private PreviewView previewView;
 private BoundingBoxView boxView;

 private YoloOnnxDetector detector;
 private final List<String> labels = new ArrayList<>();
 private java.util.concurrent.ExecutorService cameraExecutor;

 private static final int CAMERA_REQUEST = 100;

 @Override
 protected void onCreate(Bundle savedInstanceState) {
  super.onCreate(savedInstanceState);
  setContentView(R.layout.activity_main);

  previewView = findViewById(R.id.previewView);
  boxView = findViewById(R.id.boxView);

  cameraExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();

  // Load labels
  try {
   java.io.BufferedReader reader = new java.io.BufferedReader(
           new java.io.InputStreamReader(getAssets().open("labels.txt"))
   );
   String line;
   while ((line = reader.readLine()) != null) {
    if (!line.trim().isEmpty()) {
     labels.add(line.trim());
    }
   }
   reader.close();
  } catch (java.io.IOException e) {
   e.printStackTrace();
  }

  // Load ONNX model
  try {
   detector = new YoloOnnxDetector(getAssets(), "yolov8n.onnx");
  } catch (Exception e) {
   e.printStackTrace();
  }

  // Permission check
  if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
          == PackageManager.PERMISSION_GRANTED) {
   startCamera();
  } else {
   ActivityCompat.requestPermissions(
           this,
           new String[]{Manifest.permission.CAMERA},
           CAMERA_REQUEST
   );
  }
 }

 private void startCamera() {

  ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
          ProcessCameraProvider.getInstance(this);

  cameraProviderFuture.addListener(() -> {

   try {

    ProcessCameraProvider cameraProvider =
            cameraProviderFuture.get();

    // PREVIEW
    Preview preview = new Preview.Builder().build();
    preview.setSurfaceProvider(previewView.getSurfaceProvider());

    // IMAGE ANALYSIS (YOLO INPUT STREAM)
    ImageAnalysis imageAnalysis =
            new ImageAnalysis.Builder()
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .setBackpressureStrategy(
                            ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                    )
                    .build();

    imageAnalysis.setAnalyzer(
            cameraExecutor,
            this::analyzeImage
    );

    CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
    try {
     if (!cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
      if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
       cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
      } else {
       android.util.Log.e("MainActivity", "No cameras available on this device.");
       return;
      }
     }
    } catch (Exception e) {
     e.printStackTrace();
    }

    cameraProvider.unbindAll();

    cameraProvider.bindToLifecycle(
            this,
            cameraSelector,
            preview,
            imageAnalysis
    );

   } catch (ExecutionException | InterruptedException e) {
    e.printStackTrace();
   }

  }, ContextCompat.getMainExecutor(this));
 }

 // =========================
 // YOLO INFERENCE PIPELINE
 // =========================
 private void analyzeImage(ImageProxy imageProxy) {

  if (detector == null) {
   imageProxy.close();
   return;
  }

  try {

   Bitmap bitmap = ImageUtils.toBitmap(imageProxy);
   if (bitmap == null) {
    imageProxy.close();
    return;
   }

   long startTime = System.currentTimeMillis();
   List<YoloOnnxDetector.Detection> detections =
           detector.detect(bitmap);
   long duration = System.currentTimeMillis() - startTime;

   android.util.Log.d("MainActivity", "Inference took " + duration + " ms. Detected: " + detections.size() + " objects");

   runOnUiThread(() -> {

    List<BoundingBoxView.Box> boxes =
            new ArrayList<>();

    float viewWidth = boxView.getWidth();
    float viewHeight = boxView.getHeight();

    for (YoloOnnxDetector.Detection d : detections) {

     BoundingBoxView.Box box =
             new BoundingBoxView.Box();

     box.left = d.x * viewWidth;
     box.top = d.y * viewHeight;
     box.right = (d.x + d.w) * viewWidth;
     box.bottom = (d.y + d.h) * viewHeight;
     String name = (d.classId >= 0 && d.classId < labels.size()) ? labels.get(d.classId) : "Class " + d.classId;
     box.label = name + " " + String.format("%.2f", d.confidence);
     box.confidence = d.confidence;

     boxes.add(box);
    }

    boxView.setBoxes(boxes);
   });

  } catch (Exception e) {
   e.printStackTrace();
  } finally {
   imageProxy.close();
  }
 }

 // =========================
 // PERMISSION RESULT
 // =========================
 @Override
 public void onRequestPermissionsResult(
         int requestCode,
         @NonNull String[] permissions,
         @NonNull int[] grantResults
 ) {
  super.onRequestPermissionsResult(requestCode, permissions, grantResults);

  if (requestCode == CAMERA_REQUEST
          && grantResults.length > 0
          && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
   startCamera();
  }
 }

 @Override
 protected void onDestroy() {
  super.onDestroy();
  if (cameraExecutor != null) {
   cameraExecutor.shutdown();
  }
 }
}