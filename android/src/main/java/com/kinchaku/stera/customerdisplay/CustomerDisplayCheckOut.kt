package com.kinchaku.stera.customerdisplay

import com.panasonic.smartpayment.android.api.ICustomerDisplay
import android.os.Environment
import android.util.Log
import java.lang.StringBuilder

internal class CustomerDisplayCheckOut {
    var mImageKind = 0
    var mImageYesKind = 0
    var mImageNoKind = 0
    var mImageNumber = 0
    var mImageYesNumber = 0
    var mImageNoNumber = 0
    var mImageSource: String? = null
    var mImageYesSource: String? = null
    var mImageNoSource: String? = null

    // Details of coupon image files
    val imageValue: CustomerDisplayCheckOut
        get() {
            Log.d(TAG, "[in] getImageValue()")
            val customerDisplayCheckOut = CustomerDisplayCheckOut()
            customerDisplayCheckOut.mImageKind = ICustomerDisplay.IMAGE_KIND_DISPLAY
            customerDisplayCheckOut.mImageYesKind = ICustomerDisplay.IMAGE_KIND_BUTTON
            customerDisplayCheckOut.mImageNoKind = ICustomerDisplay.IMAGE_KIND_BUTTON
            customerDisplayCheckOut.mImageNumber = 2
            customerDisplayCheckOut.mImageYesNumber = 4
            customerDisplayCheckOut.mImageNoNumber = 5
            customerDisplayCheckOut.mImageSource = FILE_PATH_DISPLAY_IMAGE
            customerDisplayCheckOut.mImageYesSource = FILE_PATH_BUTTON_YES_IMAGE
            customerDisplayCheckOut.mImageNoSource = FILE_PATH_BUTTON_NO_IMAGE
            Log.d(TAG, "[out] getImageValue()")
            return customerDisplayCheckOut
        }// set XML to display parameters.

    // Set XML to display parameters.
    val xml: String
        get() {
            Log.d(TAG, "[in] getXML()")

            // set XML to display parameters.
            val sb = StringBuilder()
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            sb.append("<customerDisplayApi id=\"displaycoupon\">\n")
            sb.append("<screenPattern>3</screenPattern>\n")
            sb.append("<headerArea>\n")
            sb.append("<headerAreaNumber>1</headerAreaNumber>\n")
            sb.append("<customerString>お客様確認画面</customerString>\n")
            sb.append("</headerArea>\n")
            sb.append("<headerArea>\n")
            sb.append("<headerAreaNumber>2</headerAreaNumber>\n")
            sb.append("<customerString>＃クーポン１</customerString>\n")
            sb.append("</headerArea>\n")
            sb.append("<headerArea>\n")
            sb.append("<headerAreaNumber>3</headerAreaNumber>\n")
            sb.append("<customerString>クーポンGet！</customerString>\n")
            sb.append("</headerArea>\n")
            sb.append("<imageArea>\n")
            sb.append("<imageAreaNumber>1</imageAreaNumber>\n")
            sb.append("<imageNumber>2</imageNumber>\n")
            sb.append("</imageArea>\n")
            sb.append("<messageArea>\n")
            sb.append("<messageAreaNumber>1</messageAreaNumber>\n")
            sb.append("<customerString>クーポンを印刷しますか？</customerString>\n")
            sb.append("</messageArea>\n")
            sb.append("<buttonTop>\n")
            sb.append("<buttonNumber>1</buttonNumber>\n")
            sb.append("<imageNumber>4</imageNumber>\n")
            sb.append("</buttonTop>\n")
            sb.append("<buttonTop>\n")
            sb.append("<buttonNumber>2</buttonNumber>\n")
            sb.append("<imageNumber>5</imageNumber>\n")
            sb.append("</buttonTop>\n")
            sb.append("</customerDisplayApi>\n")
            Log.d(TAG, "XML=$sb")
            Log.d(TAG, "[out] getXML()")
            return sb.toString()
        }

    companion object {
        private const val TAG = "CustomerDisplayCheckOut"
        private val FILE_PATH_DISPLAY_IMAGE =
            Environment.getExternalStorageDirectory().absolutePath + "/coupon.jpg"
        private val FILE_PATH_BUTTON_YES_IMAGE = Environment.getExternalStorageDirectory().absolutePath + "/YES.jpg"
        private val FILE_PATH_BUTTON_NO_IMAGE = Environment.getExternalStorageDirectory().absolutePath + "/NO.jpg"
    }
}