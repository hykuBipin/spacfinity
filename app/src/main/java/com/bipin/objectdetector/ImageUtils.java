package com.bipin.objectdetector;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import androidx.camera.core.ImageProxy;
import java.nio.ByteBuffer;

public class ImageUtils {

    public static Bitmap toBitmap(ImageProxy image) {
        if (image.getFormat() == android.graphics.ImageFormat.YUV_420_888) {
            return null; // Configure RGBA_8888 output format on ImageAnalysis
        }

        ImageProxy.PlaneProxy plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        buffer.rewind();

        int pixelStride = plane.getPixelStride();
        int rowStride = plane.getRowStride();
        int rowPadding = rowStride - pixelStride * image.getWidth();

        Bitmap bitmap = Bitmap.createBitmap(
                image.getWidth() + rowPadding / pixelStride,
                image.getHeight(),
                Bitmap.Config.ARGB_8888
        );
        bitmap.copyPixelsFromBuffer(buffer);

        if (rowPadding > 0) {
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, image.getWidth(), image.getHeight());
        }

        int rotationDegrees = image.getImageInfo().getRotationDegrees();
        if (rotationDegrees != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotationDegrees);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }

        return bitmap;
    }
}