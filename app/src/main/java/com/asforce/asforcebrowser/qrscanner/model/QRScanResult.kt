package com.asforce.asforcebrowser.qrscanner.model

/**
 * QR Kod Tarama Sonuçları
 * 
 * QR kod tarama işleminin olası sonuçlarını temsil eden sealed class.
 */
sealed class QRScanResult {
    
    /**
     * Başarılı QR kod taraması
     * 
     * @property content QR kod içeriği
     */
    data class Success(val content: String) : QRScanResult()
    
    /**
     * QR kod bulunamadı
     */
    object NotFound : QRScanResult()
    
    /**
     * QR kod tarama hatası
     * 
     * @property message Hata mesajı
     */
    data class Error(val message: String) : QRScanResult()
}