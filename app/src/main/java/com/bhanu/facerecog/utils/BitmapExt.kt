package com.bhanu.facerecog.utils

import android.graphics.*
import android.media.Image
import java.io.ByteArrayOutputStream
import kotlin.math.ceil


/**
 * Created by Bhanu Prakash Pasupula on 04,Sep-2020.
 * Email: pasupula1995@gmail.com
 */

fun Image.toBitmap(): Bitmap {
    val yBuffer = this.planes[0].buffer
    val uBuffer = this.planes[1].buffer
    val vBuffer = this.planes[2].buffer
    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()
    val nv21 = ByteArray(ySize + uSize + vSize)
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
    val yuv = out.toByteArray()
    return BitmapFactory.decodeByteArray(yuv, 0, yuv.size)
}

fun Bitmap.bitmapToNv21(): ByteArray {
    val argb = IntArray(this.width * this.height )
    this.getPixels(argb, 0, this.width, 0, 0, this.width, this.height)
    val yuv = ByteArray(this.height * this.width + 2 * ceil(this.height / 2.0).toInt()
            * ceil(this.width / 2.0).toInt())
    encodeYUV420SP( yuv, argb, this.width, this.height)
    return yuv
}

private fun encodeYUV420SP(yuv420sp: ByteArray, argb: IntArray, width: Int, height: Int) {
    val frameSize = width * height
    var yIndex = 0
    var uvIndex = frameSize
    var R: Int
    var G: Int
    var B: Int
    var Y: Int
    var U: Int
    var V: Int
    var index = 0
    for (j in 0 until height) {
        for (i in 0 until width) {
            R = argb[index] and 0xff0000 shr 16
            G = argb[index] and 0xff00 shr 8
            B = argb[index] and 0xff shr 0
            Y = (66 * R + 129 * G + 25 * B + 128 shr 8) + 16
            U = (-38 * R - 74 * G + 112 * B + 128 shr 8) + 128
            V = (112 * R - 94 * G - 18 * B + 128 shr 8) + 128
            yuv420sp[yIndex++] = (if (Y < 0) 0 else if (Y > 255) 255 else Y).toByte()
            if (j % 2 == 0 && index % 2 == 0) {
                yuv420sp[uvIndex++] = (if (V < 0) 0 else if (V > 255) 255 else V).toByte()
                yuv420sp[uvIndex++] = (if (U < 0) 0 else if (U > 255) 255 else U).toByte()
            }
            index++
        }
    }
}