package com.bhanu.facerecog

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object{
        const val TAG = "MainActivity"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        firebaseFaceDetector()
        clickListeners()
    }

    private fun clickListeners() {
        user_face_img.setOnClickListener {
            val intent = Intent(this,CameraActivity::class.java)
            startActivity(intent)
        }
    }

    private fun firebaseFaceDetector() {
        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build()

        val drawable = ContextCompat.getDrawable(this, R.drawable.sample_aadhar)
        val bitmap = (drawable as BitmapDrawable).bitmap
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        val detector = FaceDetection.getClient(highAccuracyOpts)

        val result = detector.process(inputImage)
            .addOnSuccessListener { faces ->
                Log.d(TAG,"size: ${faces.size}")
                for (face: Face in faces){
                    val rect = face.boundingBox
                    val bitmap = Bitmap.createBitmap(bitmap,rect.left,rect.top,rect.width(),rect.height())
                    user_face_img.setImageDrawable(BitmapDrawable(resources,bitmap))
                }

            }
            .addOnFailureListener { e ->
                // Task failed with an exception
                Log.e(TAG,"error: ${e.message}")

            }
    }
}