package com.asforce.asforcebrowser.presentation.browser

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asforce.asforcebrowser.domain.repository.TabHistoryRepository
import com.asforce.asforcebrowser.domain.repository.TabRepository
import com.asforce.asforcebrowser.util.FaviconManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * WebViewViewModel - WebViewFragment için ViewModel
 *
 * WebView ile ilgili durumları ve verileri yönetir.
 */
@HiltViewModel
class WebViewViewModel @Inject constructor(
    private val tabRepository: TabRepository,
    private val tabHistoryRepository: TabHistoryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _currentUrl = MutableStateFlow<String?>(null)
    val currentUrl: StateFlow<String?> = _currentUrl

    /**
     * Mevcut URL'i günceller
     */
    fun updateCurrentUrl(url: String) {
        _currentUrl.value = url
    }

    /**
     * Sekme verisini günceller ve geçmişe ekler
     */
    suspend fun updateTab(tabId: Long, url: String, title: String, favicon: Bitmap?) {
        // Sekme verilerini güncelle
        val tab = tabRepository.getTabById(tabId) ?: return

        // Favicon olmadan hızlıca güncelle
        val updatedTab = tab.copy(
            title = title,
            url = url
        )
        tabRepository.updateTab(updatedTab)

        // Geçmiş kaydını ekle
        tabHistoryRepository.addHistory(tabId, url, title)

        // Eğer favicon varsa, ayrı bir işlem olarak işle
        if (favicon != null) {
            viewModelScope.launch {
                try {
                    // Favicon'u kaydet ve yolunu al
                    val faviconPath = FaviconManager.downloadAndSaveFavicon(context, url, tabId)

                    // Eğer favicon yolu alındıysa Tab nesnesini tekrar güncelle
                    if (faviconPath != null) {
                        val updatedTabWithFavicon = tab.copy(
                            title = title,
                            url = url,
                            faviconUrl = faviconPath
                        )
                        tabRepository.updateTab(updatedTabWithFavicon)
                    }
                } catch (e: Exception) {
                    // Production için sessiz catch - hataları gizle
                }
            }
        }
    }
}