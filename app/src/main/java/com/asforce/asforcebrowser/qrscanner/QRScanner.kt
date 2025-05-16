package com.asforce.asforcebrowser.qrscanner

import android.content.Context
import android.util.Log
import android.widget.Toast

/**
 * QR Kod Tarama API
 * 
 * QR kod tarama işlemlerini kolayca başlatmak için basit bir API sunar.
 */
object QRScanner {
    
    private const val TAG = "QRScanner"
    
    /**
     * QR kod taramasını başlatır
     * 
     * @param context Aktivite konteksti
     * @param onQRCodeScanned QR kod tarandığında çalıştırılacak işlev
     * @param onDismiss Dialog kapatıldığında çalıştırılacak işlev
     * @return QRScannerDialog nesnesi
     */
    fun startScan(
        context: Context,
        onQRCodeScanned: (String) -> Unit,
        onDismiss: () -> Unit = {}
    ): QRScannerDialog {
        Log.d(TAG, "QR tarama başlatılıyor")
        return QRScannerDialog.show(context, onQRCodeScanned, onDismiss)
    }
    
    /**
     * Temel URL kontrolü yapar
     * 
     * @param url Kontrol edilecek URL
     * @return Geçerli bir URL ise true, değilse false
     */
    fun isValidUrl(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://")
    }
    
    /**
     * Hata durumunda kullanıcıyı bilgilendirir
     * 
     * @param context Aktivite konteksti
     * @param errorMessage Hata mesajı
     */
    fun showError(context: Context, errorMessage: String) {
        Log.e(TAG, "QR tarama hatası: $errorMessage")
        Toast.makeText(context, "QR Kod Hatası: $errorMessage", Toast.LENGTH_SHORT).show()
    }
}