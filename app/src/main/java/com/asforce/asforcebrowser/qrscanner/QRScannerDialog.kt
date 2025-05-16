package com.asforce.asforcebrowser.qrscanner

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.util.Size
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.asforce.asforcebrowser.R
import com.asforce.asforcebrowser.qrscanner.binding.DialogQrScannerBinding
import com.asforce.asforcebrowser.qrscanner.analyzer.QRCodeAnalyzer
import com.asforce.asforcebrowser.qrscanner.model.QRDetectionMode
import com.asforce.asforcebrowser.qrscanner.model.QRScanResult
import com.asforce.asforcebrowser.qrscanner.utils.QRScanUtils
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * QR Kod Tarama Dialog Bileşeni
 * 
 * CameraX ve ML Kit ile entegre edilmiş basit ve etkili bir QR kod tarama arayüzü.
 * Düşük ışık koşullarında geliştirilmiş görüntü işleme ile tarama kabiliyeti sunar.
 * 
 * Özellikler:
 * - Hızlı QR kod algılama
 * - Otomatik odaklama
 * - Flaş kontrolü
 * - Zoom kontrolü
 * - Uyarlanabilir parlaklık ayarları
 * 
 * Kaynak: 
 * - Google ML Kit Barkod Tarama: https://developers.google.com/ml-kit/vision/barcode-scanning/android
 * - CameraX API: https://developer.android.com/training/camerax
 */
class QRScannerDialog(
    private val context: Context,
    private val onQRCodeScanned: (String) -> Unit,
    private val onDismiss: () -> Unit = {}
) : Dialog(context, R.style.Theme_Dialog_QRScanner) {

    private lateinit var binding: DialogQrScannerBinding
    
    // Kamera bileşenleri
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private lateinit var cameraExecutor: ExecutorService
    
    // QR kod analiz motoru
    private lateinit var qrCodeAnalyzer: QRCodeAnalyzer
    
    // Kontrol değişkenleri
    private var isFlashEnabled = false
    private var currentZoomRatio = 1.0f
    private var scanningActive = true
    private var lastScanTime = 0L
    private var currentDetectionMode = QRDetectionMode.NORMAL
    
    // Titreşim servisi
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Dialog pencere ayarları
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.apply {
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            setBackgroundDrawableResource(android.R.color.transparent)
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        
        // View binding (manuel adaptör)
        binding = DialogQrScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Kamera thread havuzu
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        // Titreşim servisini başlat
        initializeVibrator()
        
        // QR kod analiz motoru oluştur
        initializeQRAnalyzer()
        
        // Kullanıcı arayüzü bileşenlerini ayarla
        setupUI()
        
        // Kamera izni kontrolü ve başlatma
        checkCameraPermission()
    }

    /**
     * QR kod analiz motorunu başlatır
     */
    private fun initializeQRAnalyzer() {
        qrCodeAnalyzer = QRCodeAnalyzer(context, QRDetectionMode.NORMAL) { result ->
            if (result is QRScanResult.Success) {
                handleSuccessfulScan(result.content)
            }
        }
    }
    
    /**
     * Titreşim servisini başlatır
     */
    private fun initializeVibrator() {
        vibrator = try {
            // Android 12+ için VibratorManager kullan
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                // Eski Android sürümleri için
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
        } catch (e: Exception) {
            Log.e(TAG, "Titreşim servisi başlatılamadı: ${e.message}")
            null
        }
    }

    /**
     * UI bileşenlerini ve olay dinleyicilerini ayarlar
     */
    private fun setupUI() {
        // Kapatma butonu
        binding.btnClose.setOnClickListener {
            dismiss()
        }
        
        // Flaş butonu
        binding.btnFlash.setOnClickListener {
            toggleFlash()
        }
        
        // Zoom butonu
        binding.btnZoom.setOnClickListener {
            toggleZoom()
        }
        
        // Mod butonu
        binding.btnScanMode.setOnClickListener {
            toggleScanMode()
        }
        
        // Durum metni
        binding.qrStatusText.text = "QR kodu tarayıcı içinde hizalayın"
        
        // Kameraya dokunarak odaklama
        binding.cameraPreview.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                focusOnPoint(event.x, event.y)
                true
            } else {
                false
            }
        }
    }
    
    /**
     * Belirli bir noktaya odaklanır
     */
    private fun focusOnPoint(x: Float, y: Float) {
        try {
            val factory = binding.cameraPreview.meteringPointFactory
            val point = factory.createPoint(x, y)
            
            val action = FocusMeteringAction.Builder(point)
                .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                .build()
                
            camera?.cameraControl?.startFocusAndMetering(action)
            
            // Odak noktası göstergesi
            showFocusPoint(x, y)
        } catch (e: Exception) {
            Log.e(TAG, "Odaklanma hatası: ${e.message}")
        }
    }
    
    /**
     * Odak noktası göstergesi animasyonu
     */
    private fun showFocusPoint(x: Float, y: Float) {
        binding.focusIndicator.apply {
            translationX = x - width / 2
            translationY = y - height / 2
            alpha = 0f
            scaleX = 1.5f
            scaleY = 1.5f
            visibility = View.VISIBLE
            
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .withEndAction {
                    postDelayed({
                        animate()
                            .alpha(0f)
                            .setDuration(300)
                            .withEndAction {
                                visibility = View.INVISIBLE
                            }
                            .start()
                    }, 1000)
                }
                .start()
        }
    }

    /**
     * Kamera iznini kontrol eder
     */
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) 
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            // İzin yoksa kullanıcıyı bilgilendir ve dialog'u kapat
            Toast.makeText(context, "Kamera izni gerekli! Lütfen izin verin.", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    /**
     * Kamera önizlemesini ve QR kod tarayıcısını başlatır
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            try {
                // Kamera sağlayıcısını al
                cameraProvider = cameraProviderFuture.get()
                
                // Kamera kullanım durumlarını bağla
                bindCameraUseCases()
                
            } catch (e: Exception) {
                Log.e(TAG, "Kamera başlatma hatası: ${e.message}")
                Toast.makeText(context, "Kamera başlatılamadı: ${e.message}", Toast.LENGTH_SHORT).show()
                dismiss()
            }
            
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Kamera özelliklerini bağlar (Preview, ImageAnalysis)
     */
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return
        
        try {
            // Önceki tüm kullanım durumlarını kaldır
            cameraProvider.unbindAll()
            
            // Preview oluştur - modern API ile
            preview = Preview.Builder()
                .setTargetRotation(binding.cameraPreview.display.rotation)
                .build()
                .also {
                    it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
                }
            
            // Image Analysis için analiz ayarları - modern API ile
            imageAnalyzer = ImageAnalysis.Builder()
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetRotation(binding.cameraPreview.display.rotation)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, qrCodeAnalyzer)
                }
            
            // Kamera seçimi (arka kamera) 
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            // Kamera uygulamalarını bağla
            camera = cameraProvider.bindToLifecycle(
                context as LifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )
            
            // Kamera ayarları
            camera?.let { cam ->
                // Flash'i kapat
                cam.cameraControl.enableTorch(false)
                
                // Sürekli otomatik odaklama
                setupContinuousAutoFocus(cam)
                
                // Zoom durumunu izle
                cam.cameraInfo.zoomState.observe(context as LifecycleOwner) { zoomState ->
                    currentZoomRatio = zoomState.zoomRatio
                    updateZoomUI()
                }
            }
            
        } catch (exc: Exception) {
            Log.e(TAG, "Kamera özellikleri bağlanamadı: ${exc.message}")
            Toast.makeText(context, "Kamera başlatılamadı. Lütfen tekrar deneyin.", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Sürekli otomatik odaklanma ayarları
     */
    private fun setupContinuousAutoFocus(camera: Camera) {
        try {
            // Merkez odak noktası
            val factory = SurfaceOrientedMeteringPointFactory(1.0f, 1.0f)
            val centerPoint = factory.createPoint(0.5f, 0.5f)
            
            // Kamera kontrolü
            val action = FocusMeteringAction.Builder(centerPoint, FocusMeteringAction.FLAG_AF)
                .setAutoCancelDuration(2, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            
            // Sürekli odaklanma başlat
            camera.cameraControl.startFocusAndMetering(action)
            
        } catch (e: Exception) {
            Log.e(TAG, "Otomatik odaklama hatası: ${e.message}")
        }
    }
    
    /**
     * Başarılı QR kod taramasını işler
     */
    private fun handleSuccessfulScan(content: String) {
        // Çift tarama önleme
        val currentTime = System.currentTimeMillis()
        if (!scanningActive || (currentTime - lastScanTime) < 1000) {
            return
        }
        
        // Tarama durumunu güncelle
        scanningActive = false
        lastScanTime = currentTime
        
        // UI güncellemesi
        binding.qrStatusText.text = "QR Kod Algılandı!"
        
        // Titreşimli geri bildirim
        try {
            vibrator?.vibrate(
                VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } catch (e: Exception) {
            // Titreşim hatası sessizce geç
        }
        
        // Sonuç önizlemesini göster
        binding.qrResultPreview.apply {
            visibility = View.VISIBLE
            text = "Bulunan: ${if (content.length > 20) content.take(20) + "..." else content}"
        }
        
        // Kullanıcı arayüzü güncellemesi için gecikme
        binding.root.postDelayed({
            processScannedQR(content)
        }, 800)
    }

    /**
     * Taranan QR kod içeriğini işler ve dialog'u kapatır
     */
    private fun processScannedQR(qrContent: String) {
        // URL formatına dönüştür
        val formattedQrContent = QRScanUtils.formatQrContentToUrl(qrContent)
        
        // Tarama tamamlandı, sonucu bildir
        onQRCodeScanned(formattedQrContent)
        
        // Dialog'u kapat
        dismiss()
    }

    /**
     * Flash aç/kapat
     */
    private fun toggleFlash() {
        camera?.let { cam ->
            if (cam.cameraInfo.hasFlashUnit()) {
                isFlashEnabled = !isFlashEnabled
                cam.cameraControl.enableTorch(isFlashEnabled)
                
                // Buton metnini güncelle
                binding.btnFlash.text = if (isFlashEnabled) "Flash ✓" else "Flash"
            } else {
                Toast.makeText(context, "Bu cihazda flaş bulunmuyor", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * QR tarama modunu değiştirir
     */
    private fun toggleScanMode() {
        // Modu döngüsel olarak değiştir
        currentDetectionMode = when (currentDetectionMode) {
            QRDetectionMode.NORMAL -> QRDetectionMode.LOW_LIGHT
            QRDetectionMode.LOW_LIGHT -> QRDetectionMode.EXTREME_LOW_LIGHT
            QRDetectionMode.EXTREME_LOW_LIGHT -> QRDetectionMode.NORMAL
        }
        
        // Analiz motorunu güncelle
        qrCodeAnalyzer.setScanMode(currentDetectionMode)
        
        // UI güncellemesi
        updateScanModeUI()
    }
    
    /**
     * Tarama modu UI güncellemesi
     */
    private fun updateScanModeUI() {
        val buttonText = when (currentDetectionMode) {
            QRDetectionMode.NORMAL -> "Normal"
            QRDetectionMode.LOW_LIGHT -> "Düşük Işık"
            QRDetectionMode.EXTREME_LOW_LIGHT -> "Gece Modu"
        }
        binding.btnScanMode.text = buttonText
        
        val statusText = when (currentDetectionMode) {
            QRDetectionMode.NORMAL -> "QR kodu tarayıcı içinde hizalayın"
            QRDetectionMode.LOW_LIGHT -> "Düşük ışık modu aktif"
            QRDetectionMode.EXTREME_LOW_LIGHT -> "Gece modu aktif"
        }
        binding.qrStatusText.text = statusText
    }
    
    /**
     * Zoom değerini değiştirir
     */
    private fun toggleZoom() {
        camera?.let { cam ->
            // Zoom değerlerini 1x, 2x, 3x arasında döngüle
            val newZoomRatio = when {
                currentZoomRatio < 1.5f -> 2.0f
                currentZoomRatio < 2.5f -> 3.0f
                else -> 1.0f
            }
            
            // Zoom'u ayarla
            cam.cameraControl.setZoomRatio(newZoomRatio)
        }
    }
    
    /**
     * Zoom UI'sini güncelle
     */
    private fun updateZoomUI() {
        binding.btnZoom.text = "Zoom ${String.format("%.1f", currentZoomRatio)}x"
    }

    /**
     * Dialog kapatıldığında kaynakları temizler
     */
    override fun dismiss() {
        // QR Analyzer'ı kapat
        qrCodeAnalyzer.shutdown()
        
        // Kamera bağlantısını kes
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        
        // Dialog'u kapat
        super.dismiss()
        
        // Callback'i çağır
        onDismiss()
    }

    companion object {
        private const val TAG = "QRScannerDialog"
        
        /**
         * QR Tarama Dialog'unu gösterir
         */
        fun show(
            context: Context,
            onQRCodeScanned: (String) -> Unit,
            onDismiss: () -> Unit = {}
        ): QRScannerDialog {
            val dialog = QRScannerDialog(context, onQRCodeScanned, onDismiss)
            dialog.show()
            return dialog
        }
    }
}