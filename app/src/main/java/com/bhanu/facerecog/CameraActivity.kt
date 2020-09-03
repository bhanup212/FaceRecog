package com.bhanu.facerecog

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
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
import com.bhanu.facerecog.MainActivity.Companion.TF_OD_API_INPUT_SIZE
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.android.synthetic.main.activity_camera.*
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    companion object {
        const val TAG = "CameraActivity"
    }

    private lateinit var objectDetector: ObjectDetector
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        initFirebaseObject()
        startCamera()
    }

    private fun initFirebaseObject() {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableClassification()  // Optional
            .build()
        objectDetector = ObjectDetection.getClient(options)
        val faceDetector = FaceDetection.getClient()
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
            val rotation = degreesToFirebaseRotation(imageProxy.imageInfo.rotationDegrees)
            // Log.d(TAG,"rotation is: $rotation")
            mediaImage?.let { image ->
                val overlay = imageProxy.toBitmap()
                val firebaseImage = InputImage.fromMediaImage(mediaImage, rotation)

                FaceDetection.getClient().process(firebaseImage)
                    .addOnSuccessListener { faceList ->
                        // Log.d(TAG, "faceList: ${faceList.size}")
                        if (faceList.isNotEmpty()){
                            for (face in faceList){
                                val rect = face.boundingBox

                                try {
                                    val bitmap = Bitmap.createBitmap(
                                        overlay,
                                        rect.left,
                                        rect.top,
                                        rect.width(),
                                        rect.height()
                                    )
                                    val crop = Bitmap.createScaledBitmap(bitmap,112, 112, false)
                                    face_img.setImageBitmap(crop)
                                    val recognizeList = MainActivity.personClassifier.recognizeImage(
                                        crop,
                                        false
                                    )
                                    if (recognizeList.isNotEmpty()){
                                        Log.d(TAG, "recognition %: ${recognizeList[0].distance}")
                                        recogne_tv.text = "${recognizeList[0].distance}"
                                    }else{
                                        Log.e(TAG, "faces didn't match")
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }

                            }
                        }
                        imageProxy.close()
                    }
                    .addOnFailureListener { e ->
                        // IMPORTANT
                        imageProxy.close()
                    }
            }
        }

    }

    private fun degreesToFirebaseRotation(degrees: Int): Int = when (degrees) {
        0 -> 0
        90 -> 90
        180 -> 180
        270 -> 270
        else -> throw Exception("Rotation must be 0, 90, 180, or 270.")
    }
}