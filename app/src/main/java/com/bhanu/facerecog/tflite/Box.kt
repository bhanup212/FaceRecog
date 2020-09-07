package com.bhanu.facerecog.tflite

import android.graphics.Point
import android.graphics.Rect

/**
 * Created by Bhanu Prakash Pasupula on 07,Sep-2020.
 * Email: pasupula1995@gmail.com
 */
class Box {
    var box: IntArray
    var score = 0f
    var bbr: FloatArray
    var deleted: Boolean
    var landmark: Array<Point?>
    fun left(): Int {
        return box[0]
    }

    fun right(): Int {
        return box[2]
    }

    fun top(): Int {
        return box[1]
    }

    fun bottom(): Int {
        return box[2]
    }

    fun width(): Int {
        return box[2] - box[0] + 1
    }

    fun height(): Int {
        return box[3] - box[1] + 1
    }

    fun transform2Rect(): Rect {
        val rect = Rect()
        rect.left = box[0]
        rect.top = box[1]
        rect.right = box[2]
        rect.bottom = box[3]
        return rect
    }

    init {
        box = IntArray(4)
        bbr = FloatArray(4)
        deleted = false
        landmark = arrayOfNulls(5)
    }
}