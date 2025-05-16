package com.asforce.asforcebrowser.qrscanner.utils

/**
 * QR Kod İşleme Yardımcı Sınıfı
 * 
 * QR kod içeriklerini işlemek için yardımcı fonksiyonlar içerir.
 */
object QRScanUtils {
    
    /**
     * QR kod içeriğini URL formatına dönüştürür
     * 
     * Eğer QR içeriği bir URL değilse veya http(s) protokolüne sahip değilse
     * https:// öneki ekler. Özellikle sayısal QR kodlar için szutest.com.tr'ye yönlendirme yapar.
     * 
     * @param qrContent QR kod içeriği
     * @return URL formatında düzenlenmiş içerik
     */
    fun formatQrContentToUrl(qrContent: String): String {
        // Boş içeriği kontrol et
        if (qrContent.isBlank()) return qrContent
        
        // QR kod içeriğindeki boşlukları temizle
        val trimmedContent = qrContent.trim()
        
        // Zaten http/https protokolü içeriyorsa değiştirme
        if (trimmedContent.startsWith("http://") || trimmedContent.startsWith("https://")) {
            return trimmedContent
        }
        
        // Sayısal QR kodları kontrol et (szutest.com.tr'ye özel yönlendirme)
        if (trimmedContent.all { it.isDigit() }) {
            val baseUrl = "https://app.szutest.com.tr/EXT/PKControl/Equipment/"
            return baseUrl + trimmedContent
        }
        
        // "app.szutest.com.tr" formatındaki içerikleri kontrol et
        if (trimmedContent.contains("szutest.com.tr", ignoreCase = true) && 
            !trimmedContent.contains(" ") && 
            !trimmedContent.startsWith("http")) {
            
            return "https://" + trimmedContent
        }
        
        // Genel URL formatına benziyor mu kontrol et (örn: www.example.com)
        if (trimmedContent.contains(".") && !trimmedContent.contains(" ")) {
            return "https://" + trimmedContent
        }
        
        // Diğer durumlarda orijinal içeriği döndür
        return trimmedContent
    }
}