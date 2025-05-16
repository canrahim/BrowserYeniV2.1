package com.asforce.asforcebrowser.qrscanner.analyzer

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.asforce.asforcebrowser.qrscanner.model.QRDetectionMode
import com.asforce.asforcebrowser.qrscanner.model.QRScanResult
import com.asforce.asforcebrowser.qrscanner.utils.ImageProcessor
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicBoolean

/**
 * QR Kod Analiz Motoru
 * 
 * Kameradan gelen görüntüleri analiz ederek QR kodlarını tespit eden sınıf.
 * Farklı ışık koşullarına göre uyarlanabilir görüntü işleme algoritmaları kullanır.
 * 
 * Kaynak:
 * - Google ML Kit: https://developers.google.com/ml-kit/vision/barcode-scanning
 * - "A Robust QR Code Detection Algorithm Using Image Processing" (IEEE)
 */
class QRCodeAnalyzer(
    private val context: Context,
    initialMode: QRDetectionMode = QRDetectionMode.NORMAL,
    private val onBarcodeDetected: (QRScanResult) -> Unit
) : ImageAnalysis.Analyzer {

    // Görüntü işleme yardımcısı
    private val imageProcessor = ImageProcessor()
    
    // ML Kit barcode tarayıcısı
    private val barcodeScanner: BarcodeScanner
    
    // İşlem durumu
    private var isProcessing = AtomicBoolean(false)
    private var scanMode = initialMode
    
    // İstatistikler
    private var frameCount = 0
    private var successCount = 0
    
    init {
        // QR tarama için optimize edilmiş barkod tarayıcı seçenekleri
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_AZTEC,
                Barcode.FORMAT_DATA_MATRIX
            )
            .build()
            
        // Barkod tarayıcıyı başlat
        barcodeScanner = BarcodeScanning.getClient(options)
        
        Log.d(TAG, "QR kod analiz motoru başlatıldı. Mod: $scanMode")
    }

    /**
     * Tarama modunu ayarlar
     */
    fun setScanMode(mode: QRDetectionMode) {
        scanMode = mode
        Log.d(TAG, "QR tarama modu değiştirildi: $mode")
    }

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        // Aynı anda birden fazla görüntüyü işlemeyi önle
        if (isProcessing.get()) {
            imageProxy.close()
            return
        }
        
        frameCount++
        
        try {
            isProcessing.set(true)
            val mediaImage = imageProxy.image
            
            if (mediaImage != null) {
                // Seçilen moda göre tarama stratejisini belirle
                when (scanMode) {
                    QRDetectionMode.NORMAL -> processWithStandardMethod(imageProxy)
                    QRDetectionMode.LOW_LIGHT -> processWithLowLightEnhancement(imageProxy)
                    QRDetectionMode.EXTREME_LOW_LIGHT -> processWithExtremeEnhancement(imageProxy)
                }
            } else {
                imageProxy.close()
                isProcessing.set(false)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Görüntü analiz hatası: ${e.message}")
            imageProxy.close()
            isProcessing.set(false)
        }
    }
    
    /**
     * Standart tarama yöntemi
     */
    @ExperimentalGetImage
    private fun processWithStandardMethod(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return
        
        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )
        
        processInputImage(image, imageProxy)
    }
    
    /**
     * Düşük ışık ortamlarında geliştirilmiş tarama yöntemi
     */
    @ExperimentalGetImage
    private fun processWithLowLightEnhancement(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return
        
        // Önce standart yöntemle dene
        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )
        
        // Düşük ışık için geliştirilmiş kontrast ve parlaklık ayarları
        imageProcessor.applyLowLightEnhancement(mediaImage)
        
        processInputImage(image, imageProxy)
    }
    
    /**
     * Çok düşük ışık ortamlarında ekstra geliştirilmiş tarama yöntemi
     */
    @ExperimentalGetImage
    private fun processWithExtremeEnhancement(imageProxy: ImageProxy) {
        try {
            val mediaImage = imageProxy.image ?: return
            
            // Bitmap'e dönüştür ve iyileştirme uygula
            val bitmap = imageProcessor.imageToBitmap(mediaImage)
            
            if (bitmap != null) {
                // Ekstra görüntü iyileştirme
                val enhancedBitmap = imageProcessor.applyExtremeEnhancement(bitmap)
                
                // İyileştirilmiş görüntüyü tara
                val enhancedImage = InputImage.fromBitmap(enhancedBitmap, 0)
                
                barcodeScanner.process(enhancedImage)
                    .addOnSuccessListener { barcodes ->
                        processBarcodeResults(barcodes, imageProxy)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Barkod tarama hatası: ${e.message}")
                        imageProxy.close()
                        isProcessing.set(false)
                    }
            } else {
                // Bitmap dönüştürme başarısız olduysa standart yönteme geri dön
                processWithStandardMethod(imageProxy)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ekstra iyileştirme hatası: ${e.message}")
            processWithStandardMethod(imageProxy)
        }
    }
    
    /**
     * Giriş görüntüsünü işler
     */
    private fun processInputImage(image: InputImage, imageProxy: ImageProxy) {
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                processBarcodeResults(barcodes, imageProxy)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Barkod tarama hatası: ${e.message}")
                imageProxy.close()
                isProcessing.set(false)
            }
    }
    
    /**
     * Barkod tarama sonuçlarını işler
     */
    private fun processBarcodeResults(barcodes: List<Barcode>, imageProxy: ImageProxy) {
        try {
            if (barcodes.isNotEmpty()) {
                successCount++
                
                // En kaliteli QR kodu seç
                val bestBarcode = barcodes.sortedByDescending { evaluateBarcodeQuality(it) }.firstOrNull()
                
                if (bestBarcode?.rawValue != null) {
                    Log.d(TAG, "QR kod algılandı: ${bestBarcode.rawValue}, başarı oranı: ${successCount * 100 / frameCount}%")
                    onBarcodeDetected(QRScanResult.Success(bestBarcode.rawValue!!))
                } else {
                    onBarcodeDetected(QRScanResult.Error("QR kod içeriği okunamadı"))
                }
            } else {
                onBarcodeDetected(QRScanResult.NotFound)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sonuç işleme hatası: ${e.message}")
            onBarcodeDetected(QRScanResult.Error(e.message ?: "Bilinmeyen hata"))
        } finally {
            imageProxy.close()
            isProcessing.set(false)
        }
    }
    
    /**
     * Barkod kalitesini değerlendirir (0-100 arası puan)
     */
    private fun evaluateBarcodeQuality(barcode: Barcode): Int {
        var qualityScore = 0
        
        // Barkod boyutunu değerlendir
        val boundingBox = barcode.boundingBox
        if (boundingBox != null) {
            val area = boundingBox.width() * boundingBox.height()
            // Alan skorlaması
            qualityScore += when {
                area < 1000 -> 20  // Küçük alanlar için az puan
                area < 10000 -> 40 // Orta alanlar için orta puan
                else -> 60         // Büyük alanlar için yüksek puan
            }
        }
        
        // Değerin varlığı ve uzunluğu
        barcode.rawValue?.let {
            qualityScore += 10  // Değer varsa +10 puan
            
            // İçerik uzunluğu değerlendirmesi
            if (it.length > 3) qualityScore += 10  // Anlamlı içerik için +10 puan
        }
        
        // QR kod formatı için ek puan
        if (barcode.format == Barcode.FORMAT_QR_CODE) {
            qualityScore += 20  // QR kod formatı için +20 puan
        }
        
        // Maksimum 100 ile sınırla
        return qualityScore.coerceAtMost(100)
    }
    
    /**
     * Analiz motorunu kapatır
     */
    fun shutdown() {
        try {
            barcodeScanner.close()
        } catch (e: Exception) {
            Log.e(TAG, "Barkod tarayıcı kapatma hatası: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "QRCodeAnalyzer"
    }
}