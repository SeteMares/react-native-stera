/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kinchaku.stera.qrcode

import android.graphics.Bitmap
import com.kinchaku.stera.qrcode.common.BitMatrix
import com.kinchaku.stera.qrcode.qrcode.QRCodeWriter

/**
 * This class does the work of decoding the user's request and extracting all the data
 * to be encoded in a barcode.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
class Encoder internal constructor(private val dimension: Int) {
    @Throws(WriterException::class)
    fun encodeAsBitmap(contentsToEncode: String?): Bitmap? {
        if (contentsToEncode == null) {
            return null
        }
        val result: BitMatrix = try {
            QRCodeWriter().encode(contentsToEncode, BarcodeFormat.QR_CODE, dimension, dimension)
        } catch (iae: IllegalArgumentException) {
            // Unsupported format
            return null
        }
        val width = result.width
        val height = result.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (result[x, y]) BLACK else WHITE
//                Log.d("Encoder", String.format("#%06X", 0xFFFFFF and col))
            }
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    companion object {
        private const val WHITE = -0x1
        private const val BLACK = -0x1000000
    }

    /**
     * Convert Bitmap instance to a byte array compatible with ESC/POS printer.
     *
     * @param bitmap Bitmap to be convert
     * @return Bytes contain the image in ESC/POS command
     */
    fun bitmapToBytes(bitmap: Bitmap): ByteArray? {
        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height
        val bytesByLine = Math.ceil((bitmapWidth.toFloat() / 8f).toDouble()).toInt()
        val imageBytes = ByteArray( bytesByLine * bitmapHeight)
        var i = 0
        for (posY in 0 until bitmapHeight) {
            var j = 0
            while (j < bitmapWidth) {
                val stringBinary = StringBuilder()
                for (k in 0..7) {
                    val posX = j + k
                    if (posX < bitmapWidth) {
                        val color = bitmap.getPixel(posX, posY)
                        val r = color shr 16 and 0xff
                        val g = color shr 8 and 0xff
                        val b = color and 0xff
                        if (r > 160 && g > 160 && b > 160) {
                            stringBinary.append("0")
                        } else {
                            stringBinary.append("1")
                        }
                    } else {
                        stringBinary.append("0")
                    }
                }
                imageBytes[i++] = stringBinary.toString().toInt(2).toByte()
                j += 8
            }
        }
        return imageBytes
    }

    /**
     * Convert byte array to a hexadecimal string of the image data.
     *
     * @param bytes Bytes contain the image in ESC/POS command.
     * @return A hexadecimal string of the image data.
     */
    fun bytesToHexadecimalString(bytes: ByteArray): String? {
        val imageHexString = java.lang.StringBuilder()
        for (aByte in bytes) {
            var hexString = Integer.toHexString(aByte.toInt() and 0xFF)
            if (hexString.length == 1) {
                hexString = "0$hexString"
            }
            imageHexString.append(hexString)
        }
        return imageHexString.toString()
    }

    /**
     * Convert byte array to a 0 and 1 string of the image data.
     *
     * @param bytes Bytes contain the image in ESC/POS command.
     * @return A hexadecimal string of the image data.
     */
    fun bytesToBinaryString(bytes: ByteArray): String? {
        val imageString = java.lang.StringBuilder()
        for (aByte in bytes) {
            var binString = if (aByte.toInt() == 0) 0 else 1
            imageString.append(binString)
        }
        return imageString.toString()
    }
}