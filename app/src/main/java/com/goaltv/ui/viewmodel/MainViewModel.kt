package com.goaltv.ui.viewmodel

import android.content.Context
import android.net.Uri

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goaltv.data.repository.DataRepository
import com.goaltv.data.repository.DataRepositoryImpl
import com.goaltv.data.repository.DataSourceMode
import com.goaltv.data.repository.SearchResult
import com.goaltv.model.LiveChannel
import com.goaltv.model.VodMovie
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppScreen {
    LOGIN,
    DASHBOARD,
    LIVE_TV,
    MOVIES,
    SERIES,
    EPG,
    FAVORITES,
    SETTINGS,
    SPORTS,
    CATCH_UP
}

data class PlaybackTarget(
    val title: String,
    val streamUrl: String,
    val subTitle: String? = null,
    val isLive: Boolean = false,
    val logoUrl: String? = null,
    val channelId: String? = null,
    val categoryId: String? = null
)

class MainViewModel(
    val repository: DataRepository = DataRepositoryImpl()
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = repository.isLoggedIn

    private val _currentScreen = MutableStateFlow(
        if (repository.isLoggedIn.value) AppScreen.DASHBOARD else AppScreen.LOGIN
    )
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _currentDateTime = MutableStateFlow("")
    val currentDateTime: StateFlow<String> = _currentDateTime.asStateFlow()

    private val _currentTime = MutableStateFlow("")
    val currentTime: StateFlow<String> = _currentTime.asStateFlow()

    private val _currentDate = MutableStateFlow("")
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    private val _connectedUser = MutableStateFlow("Usuario Activo")
    val connectedUser: StateFlow<String> = _connectedUser.asStateFlow()

    private val _accountExpiration = MutableStateFlow("Indefinido")
    val accountExpiration: StateFlow<String> = _accountExpiration.asStateFlow()

    private val _activePlaybackTarget = MutableStateFlow<PlaybackTarget?>(null)
    val activePlaybackTarget: StateFlow<PlaybackTarget?> = _activePlaybackTarget.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow(SearchResult())
    val searchResults: StateFlow<SearchResult> = _searchResults.asStateFlow()

    private val _isReloading = MutableStateFlow(false)
    val isReloading: StateFlow<Boolean> = _isReloading.asStateFlow()

    val activeMode: StateFlow<DataSourceMode> = repository.activeMode
    val isDohEnabled: StateFlow<Boolean> = repository.isDohEnabled

    fun setDohEnabled(enabled: Boolean) {
        repository.setDohEnabled(enabled)
    }

    init {
        startClockTimer()
        observeAccountDetails()
        observeLoginState()
    }

    private fun observeLoginState() {
        viewModelScope.launch {
            repository.isLoggedIn.collect { loggedIn ->
                if (!loggedIn) {
                    _currentScreen.value = AppScreen.LOGIN
                } else if (_currentScreen.value == AppScreen.LOGIN) {
                    _currentScreen.value = AppScreen.DASHBOARD
                }
            }
        }
    }

    private fun observeAccountDetails() {
        viewModelScope.launch {
            repository.activeXtreamAccount.collect { acc ->
                if (acc != null) {
                    val user = acc.authResponse?.userInfo?.username?.ifBlank { acc.username } ?: acc.username
                    _connectedUser.value = if (user.isNotBlank()) user else "Usuario Activo"
                    _accountExpiration.value = formatExpirationDate(acc.authResponse?.userInfo?.expDate)
                } else {
                    _connectedUser.value = "Usuario Activo"
                    _accountExpiration.value = formatExpirationDate(null)
                }
            }
        }
    }

    private fun formatExpirationDate(expDate: String?): String {
        return try {
            if (!expDate.isNullOrBlank() && expDate != "0" && !expDate.equals("null", ignoreCase = true)) {
                val ts = expDate.toLongOrNull()
                if (ts != null && ts > 0) {
                    val millis = if (ts < 100000000000L) ts * 1000L else ts
                    val date = Date(millis)
                    return SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es", "ES")).format(date)
                }
                val datePatterns = listOf(
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy-MM-dd",
                    "dd/MM/yyyy",
                    "MM/dd/yyyy"
                )
                for (pattern in datePatterns) {
                    try {
                        val sdf = SimpleDateFormat(pattern, Locale.US)
                        val parsedDate = sdf.parse(expDate)
                        if (parsedDate != null) {
                            return SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es", "ES")).format(parsedDate)
                        }
                    } catch (_: Exception) {}
                }
            }
            val oneYearAhead = Date(System.currentTimeMillis() + 365L * 24 * 3600 * 1000)
            SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es", "ES")).format(oneYearAhead)
        } catch (_: Exception) {
            val oneYearAhead = Date(System.currentTimeMillis() + 365L * 24 * 3600 * 1000)
            SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es", "ES")).format(oneYearAhead)
        }
    }

    private fun startClockTimer() {
        viewModelScope.launch {
            try {
                val timeFormatter = SimpleDateFormat("HH:mm", Locale.US)
                val dateFormatter = SimpleDateFormat("MMMM d, yyyy", Locale("es", "ES"))
                val dateTimeFormatter = SimpleDateFormat("EEE, d MMM • HH:mm", Locale("es", "ES"))
                while (isActive) {
                    val now = Date()
                    _currentTime.value = timeFormatter.format(now).uppercase()
                    _currentDate.value = dateFormatter.format(now)
                    _currentDateTime.value = dateTimeFormatter.format(now)
                    delay(10000)
                }
            } catch (_: Exception) {}
        }
    }

    private val _lastVisitedScreen = MutableStateFlow(AppScreen.LIVE_TV)
    val lastVisitedScreen: StateFlow<AppScreen> = _lastVisitedScreen.asStateFlow()

    fun navigateTo(screen: AppScreen) {
        if (!isLoggedIn.value && screen != AppScreen.LOGIN) {
            _currentScreen.value = AppScreen.LOGIN
        } else {
            if (screen != AppScreen.DASHBOARD && screen != AppScreen.LOGIN) {
                _lastVisitedScreen.value = screen
            }
            _currentScreen.value = screen
        }
    }

    fun playChannel(channel: LiveChannel) {
        _activePlaybackTarget.value = PlaybackTarget(
            title = channel.name,
            streamUrl = channel.streamUrl,
            subTitle = "TV EN DIRECTO • Canal ${channel.channelNumber ?: ""}",
            isLive = true,
            logoUrl = channel.iconUrl,
            channelId = channel.id,
            categoryId = channel.categoryId
        )
    }

    fun playMovie(movie: VodMovie) {
        _activePlaybackTarget.value = PlaybackTarget(
            title = movie.name,
            streamUrl = movie.streamUrl,
            subTitle = "CINE • ${movie.releaseDate ?: ""}",
            isLive = false,
            logoUrl = movie.posterUrl,
            categoryId = movie.categoryId
        )
    }

    fun playStream(
        title: String,
        streamUrl: String,
        subTitle: String? = null,
        isLive: Boolean = false,
        logoUrl: String? = null,
        channelId: String? = null,
        categoryId: String? = null
    ) {
        _activePlaybackTarget.value = PlaybackTarget(
            title = title,
            streamUrl = streamUrl,
            subTitle = subTitle,
            isLive = isLive,
            logoUrl = logoUrl,
            channelId = channelId,
            categoryId = categoryId
        )
    }

    fun clearPlayback() {
        _activePlaybackTarget.value = null
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = SearchResult()
        } else {
            _searchResults.value = repository.searchContent(query)
        }
    }

    fun reloadPlaylist() {
        viewModelScope.launch {
            _isReloading.value = true
            try {
                repository.refreshData()
            } catch (_: Exception) {}
            _isReloading.value = false
        }
    }

    fun refreshAccountInfo() {
        viewModelScope.launch {
            try {
                repository.refreshAccountInfo()
            } catch (_: Exception) {}
        }
    }

    fun exportBackupToUri(context: Context, uri: Uri): Boolean {
        return try {
            val jsonStr = repository.exportBackupToJson()
            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(jsonStr.toByteArray(Charsets.UTF_8))
                os.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun importBackupFromUri(context: Context, uri: Uri): Boolean {
        return try {
            val jsonStr = context.contentResolver.openInputStream(uri)?.use { isStream ->
                isStream.bufferedReader(Charsets.UTF_8).readText()
            } ?: ""
            repository.importBackupFromJson(jsonStr)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
