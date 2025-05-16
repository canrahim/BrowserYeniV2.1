package com.asforce.asforcebrowser.qr_old.enhancement

/**
 * @deprecated Bu sınıf artık kullanılmamaktadır.
 * 
 * Bu sınıf yalnızca derleme hatalarını önlemek için burada tutulmaktadır.
 */
@Deprecated("Bu sınıf artık kullanılmamaktadır.")
class LowLightEnhancer {
    
    /**
     * QR tarama modları
     */
    enum class ScanMode {
        EXTREME_LOW_LIGHT,  // Çok düşük ışık (gece, karanlık ortam)
        LOW_LIGHT,          // Düşük ışık (loş ortam)
        NORMAL,             // Normal ışık (iç mekan)
        BRIGHT,             // Parlak ışık (dış mekan, güneşli)
        AUTO                // Otomatik mod - ışık seviyesine göre uyarlanır
    }
}