package com.asforce.asforcebrowser.qr_old.enhancement

import android.content.Context
import android.util.AttributeSet
import android.view.View

/**
 * @deprecated Bu sınıf artık kullanılmamaktadır.
 * 
 * Bu sınıf yalnızca derleme hatalarını önlemek için burada tutulmaktadır.
 */
@Deprecated("Bu sınıf artık kullanılmamaktadır.")
class ScannerOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    /**
     * Tarama animasyonunu başlatır
     */
    fun startScanAnimation() {
        // Boş uygulama - eski referans için
    }
    
    /**
     * Tarama animasyonunu durdurur
     */
    fun stopScanAnimation() {
        // Boş uygulama - eski referans için
    }
    
    /**
     * Odak noktasını gösterir
     */
    fun showFocusIndicator(x: Float, y: Float) {
        // Boş uygulama - eski referans için
    }
    
    /**
     * Durum mesajını günceller
     */
    fun updateStatusMessage(message: String) {
        // Boş uygulama - eski referans için
    }
    
    /**
     * Tarama modunu günceller
     */
    fun updateLightMode(mode: LowLightEnhancer.ScanMode) {
        // Boş uygulama - eski referans için
    }
}