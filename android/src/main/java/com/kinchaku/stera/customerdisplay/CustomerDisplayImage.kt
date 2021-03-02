package com.kinchaku.stera.customerdisplay

import com.panasonic.smartpayment.android.api.ICustomerDisplay
import java.lang.StringBuilder

internal class CustomerDisplayImage {
    var imageKind = ICustomerDisplay.IMAGE_KIND_DISPLAY
    var imageNumber = 1

    // Set XML to display parameters.
    val xml: String
        get() {
            val sb = StringBuilder()
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            sb.append("<customerDisplayApi id=\"displayopening\">\n")
            sb.append("<screenPattern>$imageKind</screenPattern>\n")
            sb.append("<imageArea>\n")
            sb.append("<imageAreaNumber>1</imageAreaNumber>\n")
            sb.append("<imageNumber>$imageNumber</imageNumber>\n")
            sb.append("</imageArea>\n")
            sb.append("</customerDisplayApi>\n")

            return sb.toString()
        }

}