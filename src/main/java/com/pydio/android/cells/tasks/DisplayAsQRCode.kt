package com.pydio.android.cells.tasks

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.widget.ImageView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RTreeNode

fun displayAsQRCode(
    context: Context,
    node: RTreeNode
): Boolean {

    val dialog = MaterialAlertDialogBuilder(context)
        .setTitle(R.string.display_as_qrcode_dialog_title)
        .setIcon(R.drawable.ic_baseline_link_24)
        .setView(R.layout.dialog_link_qrcode)
        .setMessage(
            context.resources.getString(
                R.string.display_as_qrcode_dialog_desc,
                node.getStateID()
            )
        )
        .setPositiveButton(R.string.button_ok) { _, _ ->
            // showMessage(context, "Done")
        }
        .create()
    dialog.show()

    val content = node.getShareAddress()

    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(
        content,
        BarcodeFormat.QR_CODE,
        context.resources.getInteger(R.integer.qrcode_width),
        context.resources.getInteger(R.integer.qrcode_width)
    )
    val width = bitMatrix.width
    val height = bitMatrix.height
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
    for (x in 0 until width) {
        for (y in 0 until height) {
            bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
        }
    }
    dialog.window?.findViewById<ImageView>(R.id.qr_code)?.setImageBitmap(bitmap)
    return true
}
