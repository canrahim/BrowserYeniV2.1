package com.asforce.asforcebrowser.qr_old.enhancement

import android.content.Context
import androidx.camera.core.ImageAnalysis

/**
 * @deprecated Bu sınıf artık kullanılmamaktadır.
 * 
 * Bu sınıf yalnızca derleme hatalarını önlemek için burada tutulmaktadır.
 */
@Deprecated("Bu sınıf artık kullanılmamaktadır.")
class AdvancedBarcodeAnalyzer(
    context: Context,
    private val onBarcodeDetected: (List<Any>) -> Unit
) : ImageAnalysis.Analyzer {

    override fun analyze(image: androidx.camera.core.ImageProxy) {
        // Boş uygulama - eski referans için
        image.close()
    }
    
    fun setBarcodeOptions(options: Any) {
        // Boş uygulama - eski referans için
    }
    
    fun getCurrentMode(): LowLightEnhancer.ScanMode {
        // Varsayılan değer döndür
        return LowLightEnhancer.ScanMode.NORMAL
    }
    
    fun shouldUseFlash(): Boolean {
        // Varsayılan değer döndür
        return false
    }
}