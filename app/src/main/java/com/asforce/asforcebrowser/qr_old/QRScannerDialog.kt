package com.asforce.asforcebrowser.qr_old

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import com.asforce.asforcebrowser.R

/**
 * @deprecated Bu sınıf artık kullanılmamaktadır. Yerine [com.asforce.asforcebrowser.qrscanner.QRScannerDialog] kullanın.
 *
 * Bu sınıf yalnızca derleme hatalarını önlemek için burada tutulmaktadır.
 * Güncel QR tarama işlevselliği için lütfen qrscanner paketindeki sınıfları kullanın.
 */
@Deprecated("Bu QR tarayıcı sürümü artık kullanılmamaktadır. Lütfen yeni sürümü kullanın.")
class QRScannerDialog(
    private val context: Context,
    private val onQRCodeScanned: (String) -> Unit,
    private val onDismiss: () -> Unit = {}
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Basit bir metin görünümü göster
        val textView = TextView(context)
        textView.text = "Bu sürüm kullanımdan kaldırılmıştır."
        setContentView(textView)
        
        // Kullanıcıyı bilgilendir
        onDismiss()
    }

    // Boş metotlar - derleme hatalarını önlemek için
    fun btnLowLightMode() {}
    fun startScanAnimation() {}
    fun showFocusIndicator(x: Float, y: Float) {}
    fun updateStatusMessage(message: String) {}
    fun stopScanAnimation() {}
    fun updateLightMode() {}

    /**
     * Dialog'u kapatır
     */
    override fun dismiss() {
        super.dismiss()
        onDismiss()
    }

    companion object {
        /**
         * @deprecated Yerine [com.asforce.asforcebrowser.qrscanner.QRScannerDialog.show] kullanın.
         */
        @Deprecated("Bu metot artık kullanılmamaktadır. Yeni sürümü kullanın.")
        fun show(
            context: Context,
            onQRCodeScanned: (String) -> Unit,
            onDismiss: () -> Unit = {}
        ): QRScannerDialog {
            // Yeni sürümü başlat
            com.asforce.asforcebrowser.qrscanner.QRScannerDialog.show(context, onQRCodeScanned, onDismiss)
            
            // Geriye uyumluluk için
            return QRScannerDialog(context, onQRCodeScanned, onDismiss)
        }
    }
}