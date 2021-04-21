package com.kinchaku.stera.printer

import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import com.kinchaku.stera.printer.SaveToBMP
import com.kinchaku.stera.qrcode.Encoder
import com.panasonic.smartpayment.android.api.*
import java.util.HashMap

class PrintTicket(
    private val ticket: HashMap<String, Any>,
    private val imageSource: String?,
    private val asImage: Boolean = true,
) {
    private val mCallbackHandler = Handler(Looper.getMainLooper())
    private var mListener: PrinterListener? = null

    fun setPrinterListener(listener: PrinterListener?) {
        Log.d(TAG, "[in] setPrinterListener()")
        mListener = listener
        Log.d(TAG, "[out] setPrinterListener()")
    }

    // Check if printing is success
    private val mIReceiptPrinterListener: IReceiptPrinterListener = object : IReceiptPrinterListener.Stub() {
        @Throws(RemoteException::class)
        override fun onPrintReceipt(result: Result) {
            Log.d(TAG, "[in] onPrintReceipt()")
            mCallbackHandler.post { // Set result to PrinterListener
                mListener!!.onPrintReceipt(result)
            }
            Log.d(TAG, "[out] onPrintReceipt()")
        }
    }

    fun print(paymentDeviceManager: IPaymentDeviceManager) {
        Log.d(TAG, "[in] print()")

        // Get PaymentDeviceManager instance
        val receiptPrinter = paymentDeviceManager.receiptPrinter

        // Create xml for printing
        val xml = content

        try {
            // Print receipt
            receiptPrinter.printReceipt(xml, mIReceiptPrinterListener)
        } catch (eTransaction: TransactionException) {
            eTransaction.printStackTrace()
        } catch (eFatal: FatalException) {
            val resultCode = String.format("0x%08X", eFatal.resultCode)
            val errorMessage = eFatal.message
            val additionalInformation = eFatal.additionalInformation

            // Show log of error information
            Log.d(TAG, "errorResultCode=$resultCode")
            Log.d(TAG, "errorMessage=$errorMessage")
            Log.d(TAG, "additionalInformation=$additionalInformation")
        }
        Log.d(TAG, "[out] printTicket()")
    }

    // Input receipt information
    private val content: String
        get() {
            Log.d(TAG, "[in] getContent()")

            // Input receipt information
            val sb = StringBuilder()
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><paymentApi id=\"printer\">\n")
            sb.append("<page>\n")
            sb.append("<printElements>\n")
            sb.append("<sheet>\n")

            sb.append("<line><text scale=\"4\">${ticket["brand"].toString()}</text></line>\n")
            if (ticket.containsKey("venue")) {
                sb.append("<line><text scale=\"1\">${ticket["venue"].toString()}</text></line>\n")
            }

            sb.append("<lineFeed num=\"1\"/>\n")
            sb.append("<ruledLines><dashedLine thickness=\"4\"><horizontal length=\"0\" horizontalPosition=\"0\" verticalPosition=\"0\"/></dashedLine></ruledLines>\n")

            if (ticket.containsKey("event")) {
                sb.append("<lineFeed num=\"1\"/>\n")
                sb.append("<line><text scale=\"2\">${ticket["event"]}</text></line>\n")
                sb.append("<lineFeed num=\"1\"/>\n")
            }

            if (ticket.containsKey("category")) {
                sb.append("<line><text scale=\"2\">${ticket["category"]}</text></line>\n")
            }
            if (ticket.containsKey("price")) {
                sb.append("<line><text scale=\"1\">${ticket["price"]}</text></line>\n")
            }

            sb.append("<lineFeed num=\"1\"/>\n")
            sb.append("<ruledLines><dashedLine thickness=\"4\"><horizontal length=\"0\" horizontalPosition=\"0\" verticalPosition=\"0\"/></dashedLine></ruledLines>\n")
            sb.append("<lineFeed num=\"2\"/>\n")
            sb.append("</sheet>\n")

            if (imageSource != null) {
                if (asImage) {
                    sb.append("<image horizontalPosition=\"12\" scale=\"1\" src=\"$imageSource\" />\n")
                } else {
                    val encoder = Encoder(200)
                    val bm = encoder.encodeAsBitmap(imageSource)
                    if (bm != null) {
                        val image = SaveToBMP.bytesToHexadecimalString(encoder.bitmapToBytes(bm)!!)
                        sb.append("<image horizontalPosition=\"1\" scale=\"1\">$image</image>\n")
                    }
                }
            }

            sb.append("<sheet>\n")
            if (ticket.containsKey("id")) {
                sb.append("<line><text scale=\"2\">            ${ticket["id"]}</text></line>\n")
            }

            sb.append("<lineFeed num=\"1\"/>\n")
            sb.append("<ruledLines><dashedLine thickness=\"4\"><horizontal length=\"0\" horizontalPosition=\"0\" verticalPosition=\"0\"/></dashedLine></ruledLines>\n")

            if (ticket.containsKey("details")) {
                sb.append("<lineFeed num=\"1\"/>\n")
                sb.append("<line><text scale=\"2\">${ticket["details"]}</text></line>\n")
            }
            sb.append("</sheet>\n")
            sb.append("</printElements>\n")
            sb.append("<paperCut paperCuttingMethod=\"partialcut\"/>\n")
            sb.append("</page>\n")
            sb.append("</paymentApi>\n")
            Log.d(TAG, "XML=$sb")
            Log.d(TAG, "[out] getContent()")
            return sb.toString()
        }

    companion object {
        private const val TAG = "PrintTicket"
    }
}