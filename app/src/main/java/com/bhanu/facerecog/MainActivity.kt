package com.bhanu.facerecog

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.util.Size
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bhanu.facerecog.PersonClassifier.Recognition
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException


class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
        const val TF_OD_API_INPUT_SIZE = 112
        const val TF_OD_API_IS_QUANTIZED = false
        const val TF_OD_API_MODEL_FILE = "mobile_face_net.tflite"
        const val TF_OD_API_LABELS_FILE = "file:///android_asset/labels.txt"
        private const val MINIMUM_CONFIDENCE_TF_OD_API = 0.5f
        private const val MAINTAIN_ASPECT = false
        private val DESIRED_PREVIEW_SIZE: Size = Size(640, 480)
        lateinit var personClassifier: PersonClassifier
    }

    private lateinit var faceDetector: FaceDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        clickListeners()
        initTfLite()
        firebaseFaceDetector()
    }

    private fun initTfLite() {
        try {
            personClassifier = FaceRecogModel.create(
                assets,
                TF_OD_API_MODEL_FILE,
                TF_OD_API_LABELS_FILE,
                TF_OD_API_INPUT_SIZE,
                TF_OD_API_IS_QUANTIZED
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun clickListeners() {
        user_face_img.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
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

        val drawable = ContextCompat.getDrawable(this, R.drawable.mark2)
        val bitmapPerson = (drawable as BitmapDrawable).bitmap
        val inputImage = InputImage.fromBitmap(bitmapPerson, 0)

        faceDetector = FaceDetection.getClient(highAccuracyOpts)

        val result = faceDetector.process(inputImage)
            .addOnSuccessListener { faces ->
                Log.d(TAG, "size: ${faces.size}")
                for (face: Face in faces) {
                    val rect = face.boundingBox
                    val rectF = RectF(rect)

                    val bitmap = Bitmap.createBitmap(
                        bitmapPerson,
                        rect.left,
                        rect.top,
                        rect.width(),
                        rect.height()
                    )
                    val crop = Bitmap.createScaledBitmap(bitmap,112, 112, false)
                    user_face_img.setImageDrawable(BitmapDrawable(resources, crop))
                    val recognition = Recognition("1234","person",-1f,rectF)
                    recognition.crop = crop
                    personClassifier.register("lady",recognition)
                    // val list1 = personClassifier.recognizeImage(crop,true)
                    val list = personClassifier.recognizeImage(crop,true)
                    if (list.isNotEmpty()){
                        val p = list[0]
                        Log.d(TAG,"label: ${p.title}, distance: ${p.distance}")
                    }
                }

            }
            .addOnFailureListener { e ->
                // Task failed with an exception
                Log.e(TAG, "error: ${e.message}")

            }
    }

    private fun onFaceDetected(face: Face, bitmap: Bitmap) {

        val mappedRecognitions = ArrayList<Recognition>()
        val boundingBox = RectF(face.boundingBox)

        val resultsAux: List<Recognition> = personClassifier.recognizeImage(bitmap, true)

        var confidence = -1f
        var color: Int = Color.BLUE

        if (resultsAux.isNotEmpty()){
            val result = resultsAux[0]
            confidence = result.distance
            color = if (result.id == "0") {
                Color.GREEN
            } else {
                Color.RED
            }

        }
        /*val result = Recognition(
            "0", confidence, boundingBox
        )*/

        // result.location = boundingBox
        // mappedRecognitions.add(result)
    }

    private fun createTransform(
        srcWidth: Int,
        srcHeight: Int,
        dstWidth: Int,
        dstHeight: Int,
        applyRotation: Int
    ): Matrix {
        val matrix = Matrix()
        if (applyRotation != 0) {
            if (applyRotation % 90 != 0) {
                Log.d(
                    CameraActivity.TAG,
                    "Rotation of %d % 90 != 0 $applyRotation"
                )
            }
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f)
            matrix.postRotate(applyRotation.toFloat())
        }
        if (applyRotation != 0) {
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f)
        }
        return matrix
    }
}