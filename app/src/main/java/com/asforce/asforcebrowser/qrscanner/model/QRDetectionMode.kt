package com.asforce.asforcebrowser.qrscanner.model

/**
 * QR Kod Tarama Modları
 * 
 * Farklı ışık koşullarına uygun tarama modlarını tanımlar.
 */
enum class QRDetectionMode {
    /**
     * Normal ışık koşulları için standart tarama modu
     */
    NORMAL,
    
    /**
     * Düşük ışık koşulları için geliştirilmiş kontrast ve parlaklık ayarları
     */
    LOW_LIGHT,
    
    /**
     * Çok düşük ışık (gece, karanlık ortam) koşulları için özel görüntü işleme teknikleri
     */
    EXTREME_LOW_LIGHT
}