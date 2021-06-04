package com.kinchaku.stera.customerdisplay

import com.kinchaku.stera.Message
import java.lang.StringBuilder

internal class CustomerDisplayMessage (private val msg: Message?) {

    // Set XML to display parameters.
    val xml: String
        get() {
               if (msg === null) return ""

               val sb = StringBuilder()
               sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
               sb.append("<customerDisplayApi id=\"displaymessage\">\n")
               sb.append("<screenPattern>6</screenPattern>\n")
               sb.append("<headerArea>\n")
               sb.append("<headerAreaNumber>1</headerAreaNumber>\n")
               sb.append("<customerString>${msg.header1}</customerString>\n")
               sb.append("</headerArea>\n")
               sb.append("<headerArea>\n")
               sb.append("<headerAreaNumber>2</headerAreaNumber>\n")
               sb.append("<customerString>${msg.header2}</customerString>\n")
               sb.append("</headerArea>\n")
               sb.append("<headerArea>\n")
               sb.append("<headerAreaNumber>3</headerAreaNumber>\n")
               sb.append("<customerString>${msg.header3}</customerString>\n")
               sb.append("</headerArea>\n")
               sb.append("<imageArea>\n")
               sb.append("<imageAreaNumber>1</imageAreaNumber>\n")
               sb.append("<imageNumber>1</imageNumber>\n")
               sb.append("</imageArea>\n")
               sb.append("<messageArea>\n")
               sb.append("<messageAreaNumber>1</messageAreaNumber>\n")
               sb.append("<customerString>${msg.message1}</customerString>\n")
               sb.append("</messageArea>\n")
               sb.append("</customerDisplayApi>\n")

            return sb.toString()
        }

}