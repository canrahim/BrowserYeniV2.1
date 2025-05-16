package com.asforce.asforcebrowser.qrscanner.binding

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.camera.view.PreviewView
import com.asforce.asforcebrowser.R

/**
 * QR Tarama Dialog'u için View Binding adaptörü
 * 
 * Dialog içindeki UI bileşenlerine kolay erişim sağlar.
 * Bu sınıf geçici bir çözüm olarak kullanılmaktadır ve ileride 
 * Android Jetpack ViewBinding ile değiştirilmelidir.
 */
class DialogQrScannerBinding private constructor(private val rootView: View) {
    
    // Root view
    val root: View get() = rootView
    
    // Başlık çubuğu
    val dialogTitle: TextView = rootView.findViewById(R.id.dialog_title)
    val btnClose: ImageButton = rootView.findViewById(R.id.btn_close)
    
    // Kamera önizleme
    val cameraPreview: PreviewView = rootView.findViewById(R.id.cameraPreview)
    val scannerOverlay: View = rootView.findViewById(R.id.scannerOverlay)
    val focusIndicator: View = rootView.findViewById(R.id.focusIndicator)
    
    // Durum ve bilgi metinleri
    val qrStatusText: TextView = rootView.findViewById(R.id.qrStatusText)
    val qrResultPreview: TextView = rootView.findViewById(R.id.qrResultPreview)
    
    // Kontrol butonları
    val btnFlash: Button = rootView.findViewById(R.id.btnFlash)
    val btnZoom: Button = rootView.findViewById(R.id.btnZoom)
    val btnScanMode: Button = rootView.findViewById(R.id.btnScanMode)
    
    companion object {
        /**
         * LayoutInflater kullanarak binding nesnesini oluşturur
         */
        fun inflate(inflater: LayoutInflater): DialogQrScannerBinding {
            val rootView = inflater.inflate(R.layout.dialog_qr_scanner, null, false)
            return DialogQrScannerBinding(rootView)
        }
    }
}