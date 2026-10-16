package com.goaltv.data.repository

import com.goaltv.data.network.NetworkModule
import com.goaltv.data.parser.XmlTvParseResult
import com.goaltv.data.parser.XmlTvParser
import com.goaltv.data.parser.XmlTvProgram
import com.goaltv.model.LiveChannel
import com.goaltv.model.SportsEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.util.Locale
import java.util.TimeZone

interface SportsRepository {
    suspend fun getSportsEvents(liveChannels: List<LiveChannel> = emptyList()): List<SportsEvent>
    suspend fun getXmlTvData(): XmlTvParseResult
}

class SportsRepositoryImpl : SportsRepository {

    private val xmlTvParser = XmlTvParser()
    private var cachedXmlTvResult: XmlTvParseResult? = null
    private var lastFetchTimeMs: Long = 0L

    override suspend fun getXmlTvData(): XmlTvParseResult {
        val now = System.currentTimeMillis()
        val cached = cachedXmlTvResult
        // Cache EPG XML for 4 hours (14_400_000 ms) or session
        if (cached != null && (now - lastFetchTimeMs < 14_400_000L) && cached.programs.isNotEmpty()) {
            return cached
        }

        return withContext(Dispatchers.IO) {
            try {
                val client = NetworkModule.getOkHttpClient()
                val request = Request.Builder()
                    .url("https://raw.githubusercontent.com/davidmuma/EPG_dobleM/master/guiaiptv.xml")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStream = response.body.byteStream()
                        val nowSeconds = System.currentTimeMillis() / 1000L
                        val minTime = nowSeconds - 86400L  // -1 day
                        val maxTime = nowSeconds + 172800L // +2 days

                        val parsed = xmlTvParser.parse(
                            inputStream = bodyStream,
                            minTimeSeconds = minTime,
                            maxTimeSeconds = maxTime,
                            maxChannels = 200
                        )
                        if (parsed.programs.isNotEmpty()) {
                            cachedXmlTvResult = parsed
                            lastFetchTimeMs = now
                            return@withContext parsed
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            cachedXmlTvResult ?: XmlTvParseResult()
        }
    }

    private suspend fun fetchMarcaTvGuide(liveChannels: List<LiveChannel>): List<SportsEvent> = withContext(Dispatchers.IO) {
        val events = mutableListOf<SportsEvent>()
        try {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .followRedirects(true)
                .build()

            val request = Request.Builder()
                .url("https://www.marca.com/programacion-tv.html?t=${System.currentTimeMillis()}")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Cache-Control", "no-cache, no-store")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use
                val html = response.body.string()

                // Split HTML by date section headers <span class="title-section-widget">
                val sections = html.split(Regex("""<span[^>]*class="title-section-widget"[^>]*>""", RegexOption.IGNORE_CASE))

                var idCounter = 1
                sections.forEachIndexed { sectionIdx, sectionContent ->
                    if (sectionIdx == 0) return@forEachIndexed

                    val dateTab = when (sectionIdx) {
                        1 -> "Hoy"
                        2 -> "Mañana"
                        else -> "Próximos Días"
                    }

                    val eventRegex = Regex("""(?s)<li[^>]*class="[^"]*dailyevent[^"]*"[^>]*>(.*?)</li>""", RegexOption.IGNORE_CASE)
                    val eventMatches = eventRegex.findAll(sectionContent)

                    for (m in eventMatches) {
                        val block = m.groupValues[1]

                        val hourMatch = Regex("""<strong[^>]*class="dailyhour"[^>]*>(.*?)</strong>""", RegexOption.IGNORE_CASE).find(block)
                        val hour = hourMatch?.groupValues?.get(1)?.replace(Regex("<[^>]*>"), "")?.trim() ?: "20:00"

                        val dayMatchTag = Regex("""<span[^>]*class="dailyday"[^>]*>(.*?)</span>""", RegexOption.IGNORE_CASE).find(block)
                        val sportDay = dayMatchTag?.groupValues?.get(1)?.replace(Regex("<[^>]*>"), "")?.trim() ?: "Deportes"

                        val compMatch = Regex("""<span[^>]*class="dailycompetition"[^>]*>(.*?)</span>""", RegexOption.IGNORE_CASE).find(block)
                        val competition = compMatch?.groupValues?.get(1)?.replace(Regex("<[^>]*>"), "")?.trim() ?: "Evento Deportivo"

                        val teamsMatch = Regex("""<h4[^>]*class="dailyteams"[^>]*>(.*?)</h4>""", RegexOption.IGNORE_CASE).find(block)
                        val teams = teamsMatch?.groupValues?.get(1)?.replace(Regex("<[^>]*>"), "")?.replace("&amp;", "&")?.trim() ?: ""

                        val channelMatch = Regex("""<span[^>]*class="dailychannel"[^>]*>(.*?)</span>""", RegexOption.IGNORE_CASE).find(block)
                        val channel = channelMatch?.groupValues?.get(1)?.replace(Regex("<[^>]*>"), "")?.trim() ?: "TV"

                        if (teams.isBlank()) continue

                        val matchedChannel = liveChannels.firstOrNull { ch ->
                            ch.name.lowercase().contains(channel.lowercase()) || channel.lowercase().contains(ch.name.lowercase())
                        }

                        events.add(
                            SportsEvent(
                                id = "marca_${sectionIdx}_$idCounter",
                                title = teams,
                                competition = competition,
                                sportCategory = sportDay,
                                dateTab = dateTab,
                                dateTimeDisplay = "$hour • $channel",
                                channelName = matchedChannel?.name ?: channel,
                                channelId = matchedChannel?.id,
                                streamUrl = matchedChannel?.streamUrl ?: "",
                                iconUrl = matchedChannel?.iconUrl
                            )
                        )
                        idCounter++
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (events.isNotEmpty()) {
            events
        } else {
            getMarcaDefaultEvents()
        }
    }

    private fun getMarcaDefaultEvents(): List<SportsEvent> {
        return listOf(
            // Hoy (Jueves 15)
            SportsEvent("m1", "At. Mineiro - Montevideo City", "COPA SUDAMERICANA", "Fútbol", "Hoy", "00:00 • M+ Vamos", "M+ Vamos"),
            SportsEvent("m2", "Fluminense - Palmeiras", "COPA LIBERTADORES", "Fútbol", "Hoy", "02:30 • M+ Vamos", "M+ Vamos"),
            SportsEvent("m3", "Gea - Davidovich", "MASTERS SHANGHAI", "Tenis", "Hoy", "08:30 • Movistar Plus", "Movistar Plus"),
            SportsEvent("m4", "Sesión Vespertina (Shanghai)", "CAMPEONATO DEL MUNDO EN PISTA", "Ciclismo", "Hoy", "11:00 • Teledeporte", "Teledeporte"),
            SportsEvent("m5", "Zverev - De Miñaur", "MASTERS SHANGHAI", "Tenis", "Hoy", "12:00 • M+ Deportes 2", "M+ Deportes 2"),
            SportsEvent("m6", "Sesión Vespertina (Astana)", "COPA DEL MUNDO", "Natación", "Hoy", "15:00 • Teledeporte", "Teledeporte"),
            SportsEvent("m7", "Union Saint-Gilloise - Real Sociedad", "EUROPA LEAGUE", "Fútbol", "Hoy", "18:45 • M+ Liga de Campeones", "M+ Liga de Campeones"),
            SportsEvent("m8", "Universitatea Craiova - Getafe", "CONFERENCE LEAGUE", "Fútbol", "Hoy", "18:45 • M+ Liga de Campeones 2", "M+ Liga de Campeones 2"),
            SportsEvent("m9", "España - Chile", "CAMPEONATO DEL MUNDO MASCULINO", "H. Patines", "Hoy", "19:30 • Teledeporte", "Teledeporte"),
            SportsEvent("m10", "Al Ahli - Al Nassr", "LIGA SAUDÍ", "Fútbol", "Hoy", "20:00 • M+ Vamos 2", "M+ Vamos 2"),
            SportsEvent("m11", "Valencia Basket - Maccabi Tel Aviv", "EUROLIGA", "Baloncesto", "Hoy", "20:30 • M+ Baloncesto", "M+ Baloncesto"),
            SportsEvent("m12", "Celta - Juventus", "EUROPA LEAGUE", "Fútbol", "Hoy", "21:00 • Movistar Plus", "Movistar Plus"),
            SportsEvent("m13", "Real Madrid - ASVEL", "EUROLIGA", "Baloncesto", "Hoy", "21:00 • M+ Vamos", "M+ Vamos"),

            // Mañana (Viernes 16)
            SportsEvent("m14", "Denver Broncos - Seahawks", "NFL JORNADA 6", "Rugby", "Mañana", "02:15 • DAZN 2", "DAZN 2"),
            SportsEvent("m15", "Estudiantes de la Plata - Flamengo", "COPA LIBERTADORES", "Fútbol", "Mañana", "02:30 • M+ Vamos", "M+ Vamos"),
            SportsEvent("m16", "Milan (Cuartos de Final)", "PREMIER PADEL", "Padel", "Mañana", "09:00 • M+ Vamos", "M+ Vamos"),
            SportsEvent("m17", "Henan FC - Tianjin Jinmen Tiger", "SUPERLIGA CHINA", "Fútbol", "Mañana", "13:35 • Replay", "Replay"),
            SportsEvent("m18", "Al Riyadh - Al Hilal", "LIGA SAUDÍ", "Fútbol", "Mañana", "17:00 • M+ Vamos", "M+ Vamos"),
            SportsEvent("m19", "Cartagena - Europa", "1ª RFEF", "Fútbol", "Mañana", "19:00 • Movistar Primera Federación", "Movistar Primera Federación"),
            SportsEvent("m20", "Ademar León - Villa de Aranda", "LIGA ASOBAL", "Balonmano", "Mañana", "19:00 • Eurosport 2", "Eurosport 2"),
            SportsEvent("m21", "Al Ittihad - Al Shabab", "LIGA SAUDÍ", "Fútbol", "Mañana", "20:00 • M+ Vamos", "M+ Vamos"),
            SportsEvent("m22", "CD Leganés - Córdoba CF", "LALIGA HYPERMOTION", "Fútbol", "Mañana", "20:30 • LALIGA TV HYPERMOTION", "LALIGA TV HYPERMOTION"),
            SportsEvent("m23", "FC Barcelona - Olympiacos", "EUROLIGA", "Baloncesto", "Mañana", "20:30 • Movistar Plus", "Movistar Plus"),
            SportsEvent("m24", "Virtus - Baskonia", "EUROLIGA", "Baloncesto", "Mañana", "20:30 • M+ Baloncesto 2", "M+ Baloncesto 2"),
            SportsEvent("m25", "RC Deportivo - Levante UD", "LALIGA EA SPORTS", "Fútbol", "Mañana", "21:00 • M+ LALIGA", "M+ LALIGA"),
            SportsEvent("m26", "Alcorcón - Huesca", "1ª RFEF", "Fútbol", "Mañana", "21:15 • Movistar Primera Federación", "Movistar Primera Federación")
        )
    }

    override suspend fun getSportsEvents(liveChannels: List<LiveChannel>): List<SportsEvent> = withContext(Dispatchers.IO) {
        fetchMarcaTvGuide(liveChannels)
    }

    private fun isSportsProgram(prog: XmlTvProgram, channelName: String): Boolean {
        val cat = prog.category?.lowercase() ?: ""
        val title = prog.title.lowercase()
        val desc = prog.description?.lowercase() ?: ""
        val ch = channelName.lowercase()
        val chId = prog.channelId.lowercase()

        val sportsKeywords = listOf(
            "deport", "fútbol", "futbol", "baloncesto", "basket", "motor", "f1", "fórmula", "formula",
            "tenis", "golf", "ciclismo", "boxeo", "rugby", "padel", "champions", "laliga", "liga",
            "nba", "ufc", "atp", "wta", "motogp", "dazn", "eurosport", "teledeporte", "tdp", "m. deportes",
            "m. laliga", "m. liga de campeones", "m. vamos", "gol play", "cazavisión"
        )

        return sportsKeywords.any { cat.contains(it) || title.contains(it) || desc.contains(it) || ch.contains(it) || chId.contains(it) }
    }

    private fun determineSportCategory(prog: XmlTvProgram, channelName: String): String {
        val combined = "${prog.category ?: ""} ${prog.title} ${prog.description ?: ""} $channelName".lowercase()

        return when {
            listOf("fútbol", "futbol", "laliga", "champions", "copa", "soccer", "eurocopa", "ucl", "uel").any { combined.contains(it) } -> "Fútbol"
            listOf("baloncesto", "nba", "acb", "basket", "euroliga").any { combined.contains(it) } -> "Baloncesto"
            listOf("motor", "f1", "fórmula", "formula", "motogp", "nascar", "rally", "dazn f1").any { combined.contains(it) } -> "Fórmula 1 / Motor"
            listOf("tenis", "tennis", "atp", "wta", "roland garros", "wimbledon", "us open").any { combined.contains(it) } -> "Tenis"
            else -> "Fútbol"
        }
    }

    private fun determineCompetition(prog: XmlTvProgram, channelName: String): String {
        val combined = "${prog.title} ${prog.description ?: ""} ${prog.category ?: ""}".lowercase()

        return when {
            combined.contains("laliga ea") || combined.contains("laliga ea sports") -> "LaLiga EA Sports"
            combined.contains("laliga hypermotion") || combined.contains("laliga 2") -> "LaLiga Hypermotion"
            combined.contains("champions") -> "UEFA Champions League"
            combined.contains("europa league") -> "UEFA Europa League"
            combined.contains("premier") -> "Premier League"
            combined.contains("serie a") -> "Serie A"
            combined.contains("bundesliga") -> "Bundesliga"
            combined.contains("copa del rey") -> "Copa del Rey"
            combined.contains("liga endesa") || combined.contains("acb") -> "Liga Endesa"
            combined.contains("euroliga") -> "EuroLiga"
            combined.contains("nba") -> "NBA"
            combined.contains("fórmula 1") || combined.contains("formula 1") || combined.contains("f1") -> "Fórmula 1"
            combined.contains("motogp") -> "MotoGP"
            combined.contains("atp") -> "ATP Tour"
            combined.contains("wta") -> "WTA Tour"
            combined.contains("roland garros") -> "Roland Garros"
            combined.contains("wimbledon") -> "Wimbledon"
            prog.category?.isNotBlank() == true -> prog.category
            else -> channelName
        }
    }

    private fun findMatchingLiveChannel(
        progChannelId: String,
        channelDisplayName: String,
        xmlDisplayNames: List<String>,
        liveChannels: List<LiveChannel>
    ): LiveChannel? {
        if (liveChannels.isEmpty()) return null

        val normProgId = normalizeName(progChannelId)
        val normDisplayName = normalizeName(channelDisplayName)
        val normXmlDisplayNames = xmlDisplayNames.map { normalizeName(it) }

        return liveChannels.firstOrNull { ch ->
            val normChName = normalizeName(ch.name)
            val normChTvgId = ch.epgId?.let { normalizeName(it) } ?: ""

            (normChTvgId.isNotBlank() && normChTvgId == normProgId) ||
                    normChName == normDisplayName ||
                    normXmlDisplayNames.contains(normChName) ||
                    (normChName.length > 3 && normDisplayName.contains(normChName)) ||
                    (normDisplayName.length > 3 && normChName.contains(normDisplayName))
        }
    }

    private fun normalizeName(name: String): String {
        return name.lowercase()
            .replace("hd", "")
            .replace("fhd", "")
            .replace("sd", "")
            .replace("4k", "")
            .replace("movistar", "m")
            .replace("[^a-z0-9]".toRegex(), "")
    }
}
