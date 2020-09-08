package com.bhanu.facerecog

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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
        const val PICK_IMAGE_CODE = 110
        const val PERMISSION_CODE = 111
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
        firebaseFaceDetector(getSampleImage())
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ), PERMISSION_CODE
        )
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
        open_camera.setOnClickListener {
            if (userFace != null) {
                val intent = Intent(this, CameraActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(
                    this,
                    "No face has been detected in the document. please upload correct document",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        choose_doc_btn.setOnClickListener {
            openImagePicker()
        }

    }

    private fun openImagePicker() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_PICK
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            PERMISSION_CODE -> {
                Log.d(TAG, " all permissions granded")
            }
            PICK_IMAGE_CODE -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val selectedImage: Uri = data.data!!
                    val picturePath = getPathFromURI(selectedImage)
                    Log.d(TAG," picture path: $picturePath")
                    val bitmap = BitmapFactory.decodeFile(picturePath)
                    if (bitmap != null) {
                        firebaseFaceDetector(bitmap)
                    } else {
                        Toast.makeText(this, "File error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun getPathFromURI(contentUri: Uri): String? {
        var res: String? = null
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor: Cursor? = contentResolver.query(contentUri, proj, "", null, "")
        if (cursor != null && cursor.moveToFirst()) {
            val column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            res = cursor.getString(column_index)
        }
        cursor?.close()
        return res
    }

    private fun firebaseFaceDetector(bitmapPerson: Bitmap) {
        user_face_img.setImageBitmap(bitmapPerson)
        val inputImage = InputImage.fromByteArray(
            bitmapPerson.bitmapToNv21(),
            bitmapPerson.width,
            bitmapPerson.height,
            0,
            InputImage.IMAGE_FORMAT_NV21
        )

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {

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
                        bitmap, 112, 112, false
                    )

                    userFace = crop
                    person_face_img.setImageBitmap(crop)
                    Log.d(TAG, "Similary Score: ${faceNet.compare(crop, crop)}")
                } else {
                    Log.e(TAG, "No faces found")
                    Toast.makeText(this, "No face detected", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    private fun getSampleImage(): Bitmap {
        val drawable = ContextCompat.getDrawable(this, R.drawable.sample_aadhar)
        return (drawable as BitmapDrawable).bitmap
    }
}