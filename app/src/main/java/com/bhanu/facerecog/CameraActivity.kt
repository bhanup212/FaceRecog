package com.bhanu.facerecog

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.util.Size
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.bhanu.facerecog.tflite.MobileFaceNet
import com.bhanu.facerecog.utils.bitmapToNv21
import com.bhanu.facerecog.utils.toBitmap
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.android.synthetic.main.activity_camera.*
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    companion object {
        const val TAG = "CameraActivity"
    }

    private lateinit var detector: FaceDetector
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var faceNet: MobileFaceNet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        faceNet = MobileFaceNet(assets)
        initFirebaseObject()
        startCamera()
    }

    private fun initFirebaseObject() {
        val accurateOps = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

        detector = FaceDetection.getClient(accurateOps)
    }

    private fun startCamera() {

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(this))

    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview: Preview = Preview.Builder()
            .build()

        val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        val analyzer = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build().apply {
                setAnalyzer(Executors.newSingleThreadExecutor(), FaceAnalyzer())
            }

        val camera = cameraProvider.bindToLifecycle(this, cameraSelector, analyzer, preview)

        preview.setSurfaceProvider(camera_preview.createSurfaceProvider())
    }

    inner class FaceAnalyzer : ImageAnalysis.Analyzer {

        @SuppressLint("UnsafeExperimentalUsageError")
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            val rotation = imageProxy.imageInfo.rotationDegrees
            Log.d(TAG, "rotation is: $rotation")
            mediaImage?.let { image ->
                val bitmap = mediaImage.toBitmap()

                val inputImage = InputImage.fromByteArray(
                    bitmap.bitmapToNv21(), bitmap.width, bitmap.height, rotation,
                    InputImage.IMAGE_FORMAT_NV21
                )
                detector.process(inputImage)
                    .addOnSuccessListener { faces ->
                        Log.d(TAG, "faceList: ${faces.size}")
                        for (face in faces) {
                            try {
                                val rect = face.boundingBox

                                val bitmap = Bitmap.createBitmap(
                                    bitmap,
                                    rect.left,
                                    rect.top,
                                    rect.width(),
                                    rect.height()
                                )

                                val crop = Bitmap.createScaledBitmap(
                                    bitmap, 112, 112, false
                                )

                                val matrix = Matrix()
                                matrix.postRotate(90f)

                                val rotatedBitmap = Bitmap.createBitmap(crop, 0, 0, crop.getWidth(), crop.getHeight(), matrix, true);
                                val similarity = faceNet.compare(MainActivity.userFace, rotatedBitmap)
                                face_img.setImageBitmap(rotatedBitmap)
                                recogne_tv.text = "${similarity * 100}"
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        imageProxy.close()
                    }
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                    }
            }
        }

    }
}