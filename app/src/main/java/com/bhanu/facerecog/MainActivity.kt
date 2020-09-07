package com.bhanu.facerecog

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bhanu.facerecog.tflite.MobileFaceNet
import com.bhanu.facerecog.utils.bitmapToNv21
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
        var userFace: Bitmap? = null
    }

    private lateinit var detector: FaceDetector
    private lateinit var faceNet: MobileFaceNet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        faceNet = MobileFaceNet(assets)
        requestCameraPermission()
        initFaceVision()
        clickListeners()
        firebaseFaceDetector()
    }

    private fun requestCameraPermission(){
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
    }

    private fun initFaceVision() {
        val accurateOps = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

        detector = FaceDetection.getClient(accurateOps)
    }

    private fun clickListeners() {
        user_face_img.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }
    }

    private fun firebaseFaceDetector() {
        val drawable = ContextCompat.getDrawable(this, R.drawable.bhanu)
        val bitmapPerson = (drawable as BitmapDrawable).bitmap

        val inputImage = InputImage.fromByteArray(
            bitmapPerson.bitmapToNv21(),
            bitmapPerson.width,
            bitmapPerson.height,
            0,
            InputImage.IMAGE_FORMAT_NV21
        )

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                if ( faces.isNotEmpty() ) {

                    Log.d(TAG, "faces Found")
                    val rect = faces[0]!!.boundingBox
                    val bitmap = Bitmap.createBitmap(
                        bitmapPerson,
                        rect.left,
                        rect.top,
                        rect.width(),
                        rect.height()
                    )

                    val crop = Bitmap.createScaledBitmap(
                        bitmap, 112, 112, false)

                    userFace = crop
                    Log.d(TAG, "rect is: $rect")
                    user_face_img.setImageBitmap(crop)
                    Log.d(TAG,"Similary Score: ${faceNet.compare(crop, crop)}")
                }else{
                    Log.e(TAG, "No faces found")
                    Toast.makeText(this, "No face detected", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }
}