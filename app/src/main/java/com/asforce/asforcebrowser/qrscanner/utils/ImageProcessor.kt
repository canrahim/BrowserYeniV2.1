package com.asforce.asforcebrowser.qrscanner.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.media.Image
import android.util.Log
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * Görüntü İşleme Yardımcı Sınıfı
 * 
 * QR kod tarama performansını artırmak için görüntü geliştirme işlemleri sunar.
 * Özellikle düşük ışık koşullarında tarama başarısını artırmaya yöneliktir.
 * 
 * Kaynak:
 * - Android Graphics API: https://developer.android.com/guide/topics/graphics
 * - "Low Light Image Enhancement Techniques" - IEEE (2021)
 */
class ImageProcessor {

    // Renk matrisi ve filtresi
    private val colorMatrix = ColorMatrix()
    private val paint = Paint().apply {
        colorFilter = ColorMatrixColorFilter(colorMatrix)
    }
    
    /**
     * Image nesnesini Bitmap'e dönüştürür
     * 
     * @param image Kameradan alınan görüntü
     * @return Oluşturulan Bitmap, başarısız olursa null döner
     */
    fun imageToBitmap(image: Image): Bitmap? {
        return try {
            // YUV_420_888 formatından Bitmap'e dönüştürme
            val yBuffer = image.planes[0].buffer
            val uBuffer = image.planes[1].buffer
            val vBuffer = image.planes[2].buffer
            
            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()
            
            val nv21 = ByteArray(ySize + uSize + vSize)
            
            // Y düzlemi
            yBuffer.get(nv21, 0, ySize)
            
            // UV düzlemleri
            val uvPixelStride = image.planes[1].pixelStride
            
            if (uvPixelStride == 2) { // NV21 formatı doğrudan destekleniyor
                vBuffer.get(nv21, ySize, vSize)
                uBuffer.get(nv21, ySize + vSize, uSize)
            } else {
                // Farklı formatlar için dönüşüm
                val uBegin = ySize
                val vBegin = ySize + vSize
                
                for (i in 0 until vSize) {
                    nv21[vBegin + i] = vBuffer.get(i * uvPixelStride)
                }
                
                for (i in 0 until uSize) {
                    nv21[uBegin + i] = uBuffer.get(i * uvPixelStride)
                }
            }
            
            // NV21'den Bitmap'e
            val yuvImage = android.graphics.YuvImage(
                nv21, 
                android.graphics.ImageFormat.NV21, 
                image.width, 
                image.height, 
                null
            )
            
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(
                android.graphics.Rect(0, 0, image.width, image.height), 
                100, 
                out
            )
            
            BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
        } catch (e: Exception) {
            Log.e("ImageProcessor", "Bitmap dönüştürme hatası: ${e.message}")
            null
        }
    }
    
    /**
     * Görüntüye düşük ışık iyileştirmeleri uygular
     * 
     * @param image Kameradan alınan görüntü
     */
    fun applyLowLightEnhancement(image: Image) {
        // Işık seviyesini analiz et
        val lightLevel = analyzeLightLevel(image)
        
        // Düşük ışık seviyesine göre kontrast ve parlaklık ayarları
        val contrast = 1.5f + (0.3f * (1f - lightLevel))
        val brightness = 0.05f + (0.1f * (1f - lightLevel))
        
        // Filtreleri güncelle
        updateColorMatrix(contrast, brightness)
    }
    
    /**
     * Bitmap'e ekstra görüntü iyileştirmeleri uygular
     * 
     * @param bitmap İşlenecek bitmap
     * @return Geliştirilmiş bitmap
     */
    fun applyExtremeEnhancement(bitmap: Bitmap): Bitmap {
        // Yüksek kontrast ve parlaklık ayarları
        updateColorMatrix(2.0f, 0.25f)
        
        // Yeni bitmap oluştur
        val resultBitmap = Bitmap.createBitmap(
            bitmap.width, 
            bitmap.height, 
            Bitmap.Config.ARGB_8888
        )
        
        // Filtre uygula
        val canvas = Canvas(resultBitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return resultBitmap
    }
    
    /**
     * Görüntüdeki ışık seviyesini analiz eder
     * 
     * @param image Kameradan alınan görüntü
     * @return Işık seviyesi (0.0: karanlık, 1.0: aydınlık)
     */
    private fun analyzeLightLevel(image: Image): Float {
        // Parlaklık düzleminden (Y) örnek piksel alarak ışık seviyesi tahmin et
        val yBuffer = image.planes[0].buffer
        val sampleCount = 10
        val pixelSkip = image.width * image.height / sampleCount
        
        var totalBrightness = 0f
        var samples = 0
        
        // Görüntüden örnek piksel değerleri al
        for (i in 0 until yBuffer.remaining() step pixelSkip) {
            if (samples >= sampleCount) break
            
            // Y düzlemindeki piksel değeri (YUV formatında Y parlaklık değeridir)
            val pixelValue = yBuffer.get(i).toInt() and 0xFF
            totalBrightness += pixelValue
            samples++
        }
        
        // Ortalama parlaklık değeri (0-255 aralığından 0-1 aralığına normalize et)
        return (totalBrightness / samples) / 255f
    }
    
    /**
     * Renk matrisini günceller
     * 
     * @param contrast Kontrast değeri
     * @param brightness Parlaklık değeri
     */
    private fun updateColorMatrix(contrast: Float, brightness: Float) {
        // ColorMatrix'i sıfırla
        colorMatrix.reset()
        
        // Kontrast ayarı
        val scale = contrast
        val translate = (-.5f * scale + .5f) * 255f
        colorMatrix.set(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        
        // Parlaklık ayarı
        if (brightness != 0f) {
            val brightnessMatrix = ColorMatrix()
            brightnessMatrix.set(floatArrayOf(
                1f, 0f, 0f, 0f, brightness * 255f,
                0f, 1f, 0f, 0f, brightness * 255f,
                0f, 0f, 1f, 0f, brightness * 255f,
                0f, 0f, 0f, 1f, 0f
            ))
            colorMatrix.postConcat(brightnessMatrix)
        }
        
        // Filter'ı güncelle
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
    }
}