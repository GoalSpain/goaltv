package com.goaltv.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.goaltv.model.AuthResponse
import com.goaltv.model.Category
import com.goaltv.model.CategoryType
import com.goaltv.model.Episode
import com.goaltv.model.EpgProgram
import com.goaltv.model.LiveChannel
import com.goaltv.model.Series
import com.goaltv.model.SportsEvent
import com.goaltv.model.VodMovie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

enum class DataSourceMode {
    MOCK,
    XTREAM,
    M3U
}

data class XtreamServerAccount(
    val serverUrl: String,
    val username: String,
    val password: String,
    val authResponse: AuthResponse? = null
)

data class SearchResult(
    val channels: List<LiveChannel> = emptyList(),
    val movies: List<VodMovie> = emptyList(),
    val series: List<Series> = emptyList()
)

interface DataRepository {
    val isLoggedIn: StateFlow<Boolean>
    val activeMode: StateFlow<DataSourceMode>
    val activeXtreamAccount: StateFlow<XtreamServerAccount?>
    val isDohEnabled: StateFlow<Boolean>

    fun setDohEnabled(enabled: Boolean)
    fun getOkHttpClient(): okhttp3.OkHttpClient
    fun getPlayerOkHttpClient(): okhttp3.OkHttpClient

    fun saveSession(serverUrl: String, username: String, pass: String, authResponse: AuthResponse? = null)
    fun clearSession()

    suspend fun setMode(mode: DataSourceMode)
    suspend fun connectXtreamServer(serverUrl: String, username: String, password: String): Result<AuthResponse>
    suspend fun loadM3uFromUrl(url: String): Result<Unit>
    suspend fun loadM3uFromText(content: String): Result<Unit>

    suspend fun getLiveCategories(): List<Category>
    suspend fun getLiveChannels(categoryId: String? = null): List<LiveChannel>
    suspend fun getVodCategories(): List<Category>
    suspend fun getVodMovies(categoryId: String? = null): List<VodMovie>
    suspend fun getSeriesCategories(): List<Category>
    suspend fun getSeriesList(categoryId: String? = null): List<Series>
    suspend fun getSeriesEpisodes(seriesId: String): List<Episode>
    suspend fun getEpgForChannel(channelId: String, streamId: String? = null): List<EpgProgram>
    suspend fun getSportsEvents(): List<SportsEvent>

    fun searchContent(query: String): SearchResult
    fun toggleFavoriteChannel(channelId: String)
    fun toggleFavoriteMovie(movieId: String)
    fun toggleFavoriteSeries(seriesId: String)
    fun getFavoriteChannels(): List<LiveChannel>
    fun getFavoriteMovies(): List<VodMovie>
    fun getFavoriteSeries(): List<Series>

    suspend fun refreshData()
    fun getHiddenCategoryIds(): Set<String>
    fun toggleCategoryHidden(categoryId: String)
    suspend fun getAllLiveCategories(): List<Category>
    fun getCategoryOrder(): List<String>
    fun saveCategoryOrder(order: List<String>)
    fun exportLocalBackup(): Boolean
    fun importLocalBackup(): Boolean
    fun exportBackupToJson(): String
    fun importBackupFromJson(jsonStr: String): Boolean
}

class DataRepositoryImpl(
    context: Context? = null,
    private val xtreamRepository: XtreamRepository = XtreamRepositoryImpl(),
    private val m3uRepository: M3URepository = M3URepositoryImpl(),
    private val sportsRepository: SportsRepository = SportsRepositoryImpl()
) : DataRepository {

    private val appContext: Context? = context ?: run {
        try { com.goaltv.GoalTVApplication.instance } catch (_: Exception) { null }
    }

    private val prefs: SharedPreferences? by lazy {
        appContext?.getSharedPreferences("goaltv_session", Context.MODE_PRIVATE)
    }

    private val _isLoggedIn = MutableStateFlow(
        (!prefs?.getString("username", "").isNullOrBlank() && !prefs?.getString("password", "").isNullOrBlank()) ||
        prefs?.getBoolean("is_logged_in", false) == true
    )
    override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _activeMode = MutableStateFlow(if (_isLoggedIn.value) DataSourceMode.XTREAM else DataSourceMode.MOCK)
    override val activeMode: StateFlow<DataSourceMode> = _activeMode.asStateFlow()

    private val _activeXtreamAccount = MutableStateFlow<XtreamServerAccount?>(
        if (_isLoggedIn.value) {
            val savedServer = prefs?.getString("server_url", "http://zeta.proxy-pass.xyz:80") ?: "http://zeta.proxy-pass.xyz:80"
            val savedUser = prefs?.getString("username", "") ?: ""
            val savedPass = prefs?.getString("password", "") ?: ""
            XtreamServerAccount(savedServer, savedUser, savedPass)
        } else null
    )
    override val activeXtreamAccount: StateFlow<XtreamServerAccount?> = _activeXtreamAccount.asStateFlow()

    override val isDohEnabled: StateFlow<Boolean> = com.goaltv.data.network.NetworkModule.isDohEnabled

    override fun setDohEnabled(enabled: Boolean) {
        com.goaltv.data.network.NetworkModule.setDohEnabled(enabled)
    }

    override fun getOkHttpClient(): okhttp3.OkHttpClient {
        return com.goaltv.data.network.NetworkModule.getOkHttpClient()
    }

    override fun getPlayerOkHttpClient(): okhttp3.OkHttpClient {
        return com.goaltv.data.network.NetworkModule.getPlayerOkHttpClient()
    }

    private var cachedM3uResult: com.goaltv.data.parser.M3UParseResult? = null

    private val favoriteChannelIds = mutableSetOf<String>()
    private val favoriteMovieIds = mutableSetOf<String>()
    private val favoriteSeriesIds = mutableSetOf<String>()

    // In-memory cache for loaded real data
    private var xtreamLiveCategories = listOf<Category>()
    private var xtreamLiveChannels = listOf<LiveChannel>()
    private var xtreamVodCategories = listOf<Category>()
    private var xtreamVodMovies = listOf<VodMovie>()
    private var xtreamSeriesCategories = listOf<Category>()
    private var xtreamSeriesList = listOf<Series>()

    override fun saveSession(serverUrl: String, username: String, pass: String, authResponse: AuthResponse?) {
        _isLoggedIn.value = true
        val acc = XtreamServerAccount(serverUrl, username, pass, authResponse)
        _activeXtreamAccount.value = acc
        _activeMode.value = DataSourceMode.XTREAM
        prefs?.edit()?.apply {
            putBoolean("is_logged_in", true)
            putString("server_url", serverUrl)
            putString("username", username)
            putString("password", pass)
            apply()
        }
    }

    override fun clearSession() {
        _isLoggedIn.value = false
        _activeXtreamAccount.value = null
        _activeMode.value = DataSourceMode.MOCK
        xtreamLiveCategories = emptyList()
        xtreamLiveChannels = emptyList()
        xtreamVodCategories = emptyList()
        xtreamVodMovies = emptyList()
        xtreamSeriesCategories = emptyList()
        xtreamSeriesList = emptyList()
        favoriteChannelIds.clear()
        favoriteMovieIds.clear()
        favoriteSeriesIds.clear()
        prefs?.edit()?.apply {
            putBoolean("is_logged_in", false)
            putString("username", "")
            putString("password", "")
            apply()
        }
    }

    override suspend fun setMode(mode: DataSourceMode) {
        _activeMode.value = mode
        if (mode == DataSourceMode.MOCK) {
            clearSession()
        }
    }

    override suspend fun connectXtreamServer(
        serverUrl: String,
        username: String,
        password: String
    ): Result<AuthResponse> {
        val result = xtreamRepository.authenticate(serverUrl, username, password)
        result.onSuccess { auth ->
            saveSession(serverUrl, username, password, auth)

            // Pre-fetch categories and lists in background if authenticated
            runCatching {
                xtreamLiveCategories = xtreamRepository.getLiveCategories(serverUrl, username, password).getOrDefault(emptyList())
                xtreamLiveChannels = xtreamRepository.getLiveChannels(serverUrl, username, password).getOrDefault(emptyList())
                xtreamVodCategories = xtreamRepository.getVodCategories(serverUrl, username, password).getOrDefault(emptyList())
                xtreamVodMovies = xtreamRepository.getVodMovies(serverUrl, username, password).getOrDefault(emptyList())
                xtreamSeriesCategories = xtreamRepository.getSeriesCategories(serverUrl, username, password).getOrDefault(emptyList())
                xtreamSeriesList = xtreamRepository.getSeriesList(serverUrl, username, password).getOrDefault(emptyList())
            }
        }
        return result
    }

    override suspend fun loadM3uFromUrl(url: String): Result<Unit> {
        val result = m3uRepository.fetchAndParseFromUrl(url)
        return result.map { parseResult ->
            cachedM3uResult = parseResult
            _activeMode.value = DataSourceMode.M3U
        }
    }

    override suspend fun loadM3uFromText(content: String): Result<Unit> {
        val result = m3uRepository.parseFromText(content)
        return result.map { parseResult ->
            cachedM3uResult = parseResult
            _activeMode.value = DataSourceMode.M3U
        }
    }

    override fun getHiddenCategoryIds(): Set<String> {
        return prefs?.getStringSet("hidden_category_ids", emptySet()) ?: emptySet()
    }

    override fun exportBackupToJson(): String {
        val json = org.json.JSONObject().apply {
            put("category_order", prefs?.getString("category_order", ""))

            val hiddenSet = prefs?.getStringSet("hidden_category_ids", emptySet()) ?: emptySet()
            put("hidden_category_ids", org.json.JSONArray(hiddenSet))

            val favChannels = favoriteChannelIds
            put("favorite_channels", org.json.JSONArray(favChannels))

            val favMovies = favoriteMovieIds
            put("favorite_movies", org.json.JSONArray(favMovies))

            val favSeries = favoriteSeriesIds
            put("favorite_series", org.json.JSONArray(favSeries))
        }
        return json.toString()
    }

    override fun importBackupFromJson(jsonStr: String): Boolean {
        return try {
            if (jsonStr.isBlank()) return false
            val json = org.json.JSONObject(jsonStr)

            val categoryOrder = json.optString("category_order", "")

            val hiddenArray = json.optJSONArray("hidden_category_ids")
            val hiddenSet = mutableSetOf<String>()
            if (hiddenArray != null) {
                for (i in 0 until hiddenArray.length()) {
                    hiddenSet.add(hiddenArray.getString(i))
                }
            }

            val favChannelsArray = json.optJSONArray("favorite_channels")
            if (favChannelsArray != null) {
                favoriteChannelIds.clear()
                for (i in 0 until favChannelsArray.length()) {
                    favoriteChannelIds.add(favChannelsArray.getString(i))
                }
            }

            val favMoviesArray = json.optJSONArray("favorite_movies")
            if (favMoviesArray != null) {
                favoriteMovieIds.clear()
                for (i in 0 until favMoviesArray.length()) {
                    favoriteMovieIds.add(favMoviesArray.getString(i))
                }
            }

            val favSeriesArray = json.optJSONArray("favorite_series")
            if (favSeriesArray != null) {
                favoriteSeriesIds.clear()
                for (i in 0 until favSeriesArray.length()) {
                    favoriteSeriesIds.add(favSeriesArray.getString(i))
                }
            }

            val p = prefs
            if (p != null) {
                val editor = p.edit()
                if (categoryOrder.isNotBlank()) editor.putString("category_order", categoryOrder)
                if (hiddenSet.isNotEmpty()) editor.putStringSet("hidden_category_ids", hiddenSet)
                editor.commit()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun exportLocalBackup(): Boolean {
        return try {
            val jsonStr = exportBackupToJson()
            var savedAtLeastOne = false

            val p = prefs
            if (p != null) {
                p.edit().putString("goaltv_local_backup", jsonStr).commit()
                savedAtLeastOne = true
            }

            // 1. Try Internal Files Dir
            try {
                appContext?.let { ctx ->
                    val file = java.io.File(ctx.filesDir, "goaltv_backup.json")
                    if (file.exists()) file.delete()
                    file.writeText(jsonStr)
                    savedAtLeastOne = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Try App External Files Dir
            try {
                appContext?.let { ctx ->
                    val extDir = ctx.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) ?: ctx.getExternalFilesDir(null)
                    if (extDir != null) {
                        if (!extDir.exists()) extDir.mkdirs()
                        val file = java.io.File(extDir, "goaltv_backup.json")
                        if (file.exists()) file.delete()
                        file.writeText(jsonStr)
                        savedAtLeastOne = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 3. Try Public Downloads
            try {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (downloadsDir != null) {
                    if (!downloadsDir.exists()) downloadsDir.mkdirs()
                    val file = java.io.File(downloadsDir, "goaltv_backup.json")
                    if (file.exists()) file.delete()
                    file.writeText(jsonStr)
                    savedAtLeastOne = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            savedAtLeastOne
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun importLocalBackup(): Boolean {
        return try {
            var rawJson = ""

            // 1. Check SharedPreferences Backup
            rawJson = prefs?.getString("goaltv_local_backup", "") ?: ""

            // 2. Check Internal Files Dir
            if (rawJson.isBlank()) {
                appContext?.let { ctx ->
                    val intFile = java.io.File(ctx.filesDir, "goaltv_backup.json")
                    if (intFile.exists() && intFile.length() > 0) {
                        rawJson = intFile.readText()
                    }
                }
            }

            // 3. Check App External Files Dir
            if (rawJson.isBlank()) {
                appContext?.let { ctx ->
                    val extDir = ctx.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) ?: ctx.getExternalFilesDir(null)
                    val extFile = java.io.File(extDir, "goaltv_backup.json")
                    if (extFile.exists() && extFile.length() > 0) {
                        rawJson = extFile.readText()
                    }
                }
            }

            // 4. Check Public Downloads
            if (rawJson.isBlank()) {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val pubFile = java.io.File(downloadsDir, "goaltv_backup.json")
                if (pubFile.exists() && pubFile.length() > 0) {
                    rawJson = pubFile.readText()
                }
            }

            if (rawJson.isBlank()) return false
            importBackupFromJson(rawJson)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun toggleCategoryHidden(categoryId: String) {
        val current = getHiddenCategoryIds().toMutableSet()
        if (current.contains(categoryId)) {
            current.remove(categoryId)
        } else {
            current.add(categoryId)
        }
        prefs?.edit()?.putStringSet("hidden_category_ids", current)?.apply()
    }

    override suspend fun refreshData() {
        val acc = _activeXtreamAccount.value ?: run {
            val savedServer = prefs?.getString("server_url", "") ?: ""
            val savedUser = prefs?.getString("username", "") ?: ""
            val savedPass = prefs?.getString("password", "") ?: ""
            if (savedServer.isNotBlank() && savedUser.isNotBlank() && savedPass.isNotBlank()) {
                val newAcc = XtreamServerAccount(savedServer, savedUser, savedPass)
                _activeXtreamAccount.value = newAcc
                newAcc
            } else null
        } ?: return

        withContext(Dispatchers.IO) {
            runCatching {
                // Obligatory real HTTP network authentication handshake on launch
                val authResult = xtreamRepository.authenticate(acc.serverUrl, acc.username, acc.password)
                val authRes = authResult.getOrNull()

                if (authRes != null && authRes.userInfo?.auth == 1) {
                    _activeXtreamAccount.value = acc.copy(authResponse = authRes)
                    _isLoggedIn.value = true
                    prefs?.edit()?.putBoolean("is_logged_in", true)?.apply()
                } else if (authRes != null && authRes.userInfo?.auth == 0) {
                    _isLoggedIn.value = false
                    prefs?.edit()?.putBoolean("is_logged_in", false)?.apply()
                    return@runCatching
                } else if (authRes != null) {
                    _activeXtreamAccount.value = acc.copy(authResponse = authRes)
                }

                xtreamLiveCategories = xtreamRepository.getLiveCategories(acc.serverUrl, acc.username, acc.password).getOrDefault(emptyList())
                xtreamLiveChannels = xtreamRepository.getLiveChannels(acc.serverUrl, acc.username, acc.password).getOrDefault(emptyList())
                xtreamVodCategories = xtreamRepository.getVodCategories(acc.serverUrl, acc.username, acc.password).getOrDefault(emptyList())
                xtreamVodMovies = xtreamRepository.getVodMovies(acc.serverUrl, acc.username, acc.password).getOrDefault(emptyList())
                xtreamSeriesCategories = xtreamRepository.getSeriesCategories(acc.serverUrl, acc.username, acc.password).getOrDefault(emptyList())
                xtreamSeriesList = xtreamRepository.getSeriesList(acc.serverUrl, acc.username, acc.password).getOrDefault(emptyList())
            }
        }
    }

    override fun getCategoryOrder(): List<String> {
        val raw = prefs?.getString("category_order", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(",")
    }

    override fun saveCategoryOrder(order: List<String>) {
        prefs?.edit()?.putString("category_order", order.joinToString(","))?.apply()
    }

    override suspend fun getAllLiveCategories(): List<Category> {
        val rawCats = when (_activeMode.value) {
            DataSourceMode.MOCK -> emptyList()
            DataSourceMode.XTREAM -> {
                if (xtreamLiveCategories.isNotEmpty()) xtreamLiveCategories
                else {
                    val acc = _activeXtreamAccount.value ?: return emptyList()
                    xtreamRepository.getLiveCategories(acc.serverUrl, acc.username, acc.password)
                        .getOrDefault(emptyList())
                        .also { xtreamLiveCategories = it }
                }
            }
            DataSourceMode.M3U -> {
                cachedM3uResult?.categories?.filter { it.type == CategoryType.LIVE } ?: emptyList()
            }
        }
        val order = getCategoryOrder()
        return if (order.isNotEmpty()) {
            rawCats.sortedBy { cat ->
                val idx = order.indexOf(cat.id)
                if (idx >= 0) idx else 999
            }
        } else {
            rawCats
        }
    }

    override suspend fun getLiveCategories(): List<Category> {
        val hidden = getHiddenCategoryIds()
        val allCats = getAllLiveCategories()
        return allCats.filter { !hidden.contains(it.id) }
    }

    override suspend fun getLiveChannels(categoryId: String?): List<LiveChannel> {
        val hidden = getHiddenCategoryIds()
        val rawChannels = when (_activeMode.value) {
            DataSourceMode.MOCK -> emptyList()
            DataSourceMode.XTREAM -> {
                val acc = _activeXtreamAccount.value
                if (acc != null && xtreamLiveChannels.isEmpty()) {
                    xtreamLiveChannels = xtreamRepository.getLiveChannels(acc.serverUrl, acc.username, acc.password)
                        .getOrDefault(emptyList())
                }
                xtreamLiveChannels
            }
            DataSourceMode.M3U -> {
                cachedM3uResult?.liveChannels ?: emptyList()
            }
        }

        val visibleChannels = rawChannels.filter { !hidden.contains(it.categoryId) }

        val filtered = if (categoryId.isNullOrBlank()) {
            visibleChannels
        } else {
            visibleChannels.filter { it.categoryId == categoryId }
        }

        return filtered.map { ch ->
            ch.copy(
                isFavorite = favoriteChannelIds.contains(ch.id)
            )
        }
    }

    override suspend fun getVodCategories(): List<Category> {
        return when (_activeMode.value) {
            DataSourceMode.MOCK -> emptyList()
            DataSourceMode.XTREAM -> {
                if (xtreamVodCategories.isNotEmpty()) xtreamVodCategories
                else {
                    val acc = _activeXtreamAccount.value ?: return emptyList()
                    xtreamRepository.getVodCategories(acc.serverUrl, acc.username, acc.password)
                        .getOrDefault(emptyList())
                        .also { xtreamVodCategories = it }
                }
            }
            DataSourceMode.M3U -> {
                cachedM3uResult?.categories?.filter { it.type == CategoryType.VOD } ?: emptyList()
            }
        }
    }

    override suspend fun getVodMovies(categoryId: String?): List<VodMovie> {
        val rawMovies = when (_activeMode.value) {
            DataSourceMode.MOCK -> emptyList()
            DataSourceMode.XTREAM -> {
                val acc = _activeXtreamAccount.value
                if (acc != null && xtreamVodMovies.isEmpty()) {
                    xtreamVodMovies = xtreamRepository.getVodMovies(acc.serverUrl, acc.username, acc.password)
                        .getOrDefault(emptyList())
                }
                xtreamVodMovies
            }
            DataSourceMode.M3U -> {
                cachedM3uResult?.vodMovies ?: emptyList()
            }
        }

        val filtered = if (categoryId.isNullOrBlank()) {
            rawMovies
        } else {
            rawMovies.filter { it.categoryId == categoryId }
        }

        return filtered.map { m ->
            m.copy(isFavorite = favoriteMovieIds.contains(m.id))
        }
    }

    override suspend fun getSeriesCategories(): List<Category> {
        return when (_activeMode.value) {
            DataSourceMode.MOCK -> emptyList()
            DataSourceMode.XTREAM -> {
                if (xtreamSeriesCategories.isNotEmpty()) xtreamSeriesCategories
                else {
                    val acc = _activeXtreamAccount.value ?: return emptyList()
                    xtreamRepository.getSeriesCategories(acc.serverUrl, acc.username, acc.password)
                        .getOrDefault(emptyList())
                        .also { xtreamSeriesCategories = it }
                }
            }
            DataSourceMode.M3U -> {
                cachedM3uResult?.categories?.filter { it.type == CategoryType.SERIES } ?: emptyList()
            }
        }
    }

    override suspend fun getSeriesList(categoryId: String?): List<Series> {
        val rawSeries = when (_activeMode.value) {
            DataSourceMode.MOCK -> emptyList()
            DataSourceMode.XTREAM -> {
                val acc = _activeXtreamAccount.value
                if (acc != null && xtreamSeriesList.isEmpty()) {
                    xtreamSeriesList = xtreamRepository.getSeriesList(acc.serverUrl, acc.username, acc.password)
                        .getOrDefault(emptyList())
                }
                xtreamSeriesList
            }
            DataSourceMode.M3U -> {
                cachedM3uResult?.seriesList ?: emptyList()
            }
        }

        val filtered = if (categoryId.isNullOrBlank()) {
            rawSeries
        } else {
            rawSeries.filter { it.categoryId == categoryId }
        }

        return filtered.map { s ->
            s.copy(isFavorite = favoriteSeriesIds.contains(s.id))
        }
    }

    override suspend fun getSeriesEpisodes(seriesId: String): List<Episode> {
        if (_activeMode.value == DataSourceMode.XTREAM) {
            val acc = _activeXtreamAccount.value
            if (acc != null) {
                val remote = xtreamRepository.getSeriesEpisodes(acc.serverUrl, acc.username, acc.password, seriesId)
                if (remote.isSuccess) {
                    val list = remote.getOrDefault(emptyList())
                    if (list.isNotEmpty()) return list
                }
            }
        }
        return emptyList()
    }

    private fun cleanChannelNameForEpg(rawName: String): String {
        var clean = rawName
            .replace(Regex("""(?i)^\s*(ES\b|ES\s*4k-?|ES\s*fhd-?|ES\s*hd-?|ES\||ES:|-|\[ES]|\bHD\b|\bFHD\b|\b4K\b|\bSD\b)\s*"""), "")
            .replace(Regex("""(?i)\s*(HD|FHD|4K|SD|\d+k|\.tv)$"""), "")
            .replace(Regex("""(?i)\bmovistar\b"""), "m+")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .lowercase()

        if (clean == "tve 1" || clean == "tve1" || clean == "la1" || clean == "la 1") clean = "la 1"
        if (clean == "tve 2" || clean == "tve2" || clean == "la2" || clean == "la 2") clean = "la 2"
        return clean
    }

    private fun isChannelMatch(cleanTarget: String, cleanXmlName: String): Boolean {
        if (cleanTarget.isBlank() || cleanXmlName.isBlank()) return false
        val t = cleanTarget.replace(" ", "")
        val x = cleanXmlName.replace(" ", "")
        if (t == x) return true
        if (cleanTarget == "la 1" && x.startsWith("la1")) return true
        if (cleanTarget == "la 2" && x.startsWith("la2")) return true
        return false
    }

    override suspend fun getEpgForChannel(channelId: String, streamId: String?): List<EpgProgram> = withContext(Dispatchers.IO) {
        try {
            val xmlTvResult = sportsRepository.getXmlTvData()
            if (xmlTvResult.programs.isEmpty()) return@withContext emptyList()

            if (xtreamLiveChannels.isEmpty()) {
                getLiveChannels()
            }

            val targetChannel = xtreamLiveChannels.firstOrNull { it.id == channelId || it.epgId == channelId || (streamId != null && it.epgId == streamId) }
            val rawName = targetChannel?.name ?: channelId
            val cleanedName = cleanChannelNameForEpg(rawName)
            val tvgId = targetChannel?.epgId

            val matchedXmlChannelIds = xmlTvResult.channels.entries.filter { (id, ch) ->
                id.equals(tvgId, ignoreCase = true) ||
                id.equals(channelId, ignoreCase = true) ||
                ch.displayNames.any { displayName ->
                    val cleanDisplay = cleanChannelNameForEpg(displayName)
                    isChannelMatch(cleanedName, cleanDisplay)
                }
            }.map { it.key }.toSet()

            val matchingProgs = xmlTvResult.programs.filter { p ->
                p.channelId == channelId || (tvgId != null && p.channelId == tvgId) || matchedXmlChannelIds.contains(p.channelId)
            }

            if (matchingProgs.isNotEmpty()) {
                val nowSec = System.currentTimeMillis() / 1000L
                return@withContext matchingProgs.map { p ->
                    val live = nowSec in p.startTimeSeconds..p.stopTimeSeconds
                    EpgProgram(
                        id = "${p.channelId}_${p.startTimeSeconds}",
                        channelId = channelId,
                        title = p.title,
                        description = p.description ?: "",
                        startTimestamp = p.startTimeSeconds,
                        endTimestamp = p.stopTimeSeconds,
                        isLive = live
                    )
                }.sortedBy { it.startTimestamp }
            }
            emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getSportsEvents(): List<SportsEvent> = withContext(Dispatchers.IO) {
        val channels = getLiveChannels()
        sportsRepository.getSportsEvents(channels)
    }

    override fun searchContent(query: String): SearchResult {
        if (query.isBlank()) return SearchResult()
        val q = query.trim().lowercase()

        val matchingChannels = xtreamLiveChannels.filter { it.name.lowercase().contains(q) }
        val matchingMovies = xtreamVodMovies.filter { it.name.lowercase().contains(q) || it.description?.lowercase()?.contains(q) == true }
        val matchingSeries = xtreamSeriesList.filter { it.name.lowercase().contains(q) || it.overview?.lowercase()?.contains(q) == true }

        return SearchResult(
            channels = matchingChannels,
            movies = matchingMovies,
            series = matchingSeries
        )
    }

    override fun toggleFavoriteChannel(channelId: String) {
        if (favoriteChannelIds.contains(channelId)) {
            favoriteChannelIds.remove(channelId)
        } else {
            favoriteChannelIds.add(channelId)
        }
    }

    override fun toggleFavoriteMovie(movieId: String) {
        if (favoriteMovieIds.contains(movieId)) {
            favoriteMovieIds.remove(movieId)
        } else {
            favoriteMovieIds.add(movieId)
        }
    }

    override fun toggleFavoriteSeries(seriesId: String) {
        if (favoriteSeriesIds.contains(seriesId)) {
            favoriteSeriesIds.remove(seriesId)
        } else {
            favoriteSeriesIds.add(seriesId)
        }
    }

    override fun getFavoriteChannels(): List<LiveChannel> {
        return xtreamLiveChannels.filter { favoriteChannelIds.contains(it.id) }
            .map { it.copy(isFavorite = true) }
    }

    override fun getFavoriteMovies(): List<VodMovie> {
        return xtreamVodMovies.filter { favoriteMovieIds.contains(it.id) }
            .map { it.copy(isFavorite = true) }
    }

    override fun getFavoriteSeries(): List<Series> {
        return xtreamSeriesList.filter { favoriteSeriesIds.contains(it.id) }
            .map { it.copy(isFavorite = true) }
    }
}
