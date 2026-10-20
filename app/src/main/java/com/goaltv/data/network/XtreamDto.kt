package com.goaltv.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class XtreamAuthDto(
    @field:Json(name = "user_info") val userInfo: XtreamUserInfoDto? = null,
    @field:Json(name = "server_info") val serverInfo: XtreamServerInfoDto? = null
)

@JsonClass(generateAdapter = true)
data class XtreamUserInfoDto(
    @field:Json(name = "username") val username: String? = null,
    @field:Json(name = "password") val password: String? = null,
    @field:Json(name = "message") val message: String? = null,
    @field:Json(name = "auth") val auth: Int? = null,
    @field:Json(name = "status") val status: String? = null,
    @field:Json(name = "exp_date") val expDate: String? = null,
    @field:Json(name = "is_trial") val isTrial: String? = null,
    @field:Json(name = "active_cons") val activeCons: String? = null,
    @field:Json(name = "created_at") val createdAt: String? = null,
    @field:Json(name = "max_connections") val maxConnections: String? = null,
    @field:Json(name = "allowed_output_formats") val allowedOutputFormats: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class XtreamServerInfoDto(
    @field:Json(name = "url") val url: String? = null,
    @field:Json(name = "port") val port: String? = null,
    @field:Json(name = "https_port") val httpsPort: String? = null,
    @field:Json(name = "server_protocol") val serverProtocol: String? = null,
    @field:Json(name = "rtmp_port") val rtmpPort: String? = null,
    @field:Json(name = "timezone") val timezone: String? = null,
    @field:Json(name = "timestamp") val timestamp: Long? = null
)

@JsonClass(generateAdapter = true)
data class XtreamCategoryDto(
    @field:Json(name = "category_id") val categoryId: String? = null,
    @field:Json(name = "category_name") val categoryName: String? = null,
    @field:Json(name = "parent_id") val parentId: Int? = null
)

@JsonClass(generateAdapter = true)
data class XtreamLiveStreamDto(
    @field:Json(name = "num") val num: Int? = null,
    @field:Json(name = "name") val name: String? = null,
    @field:Json(name = "stream_id") val streamId: Any? = null,
    @field:Json(name = "stream_icon") val streamIcon: String? = null,
    @field:Json(name = "epg_channel_id") val epgChannelId: String? = null,
    @field:Json(name = "category_id") val categoryId: String? = null,
    @field:Json(name = "stream_type") val streamType: String? = null,
    @field:Json(name = "custom_sid") val customSid: String? = null,
    @field:Json(name = "direct_source") val directSource: String? = null,
    @field:Json(name = "tv_archive") val tvArchive: Any? = null,
    @field:Json(name = "tv_archive_duration") val tvArchiveDuration: Any? = null
)

@JsonClass(generateAdapter = true)
data class XtreamVodStreamDto(
    @field:Json(name = "num") val num: Int? = null,
    @field:Json(name = "name") val name: String? = null,
    @field:Json(name = "stream_id") val streamId: Any? = null,
    @field:Json(name = "stream_icon") val streamIcon: String? = null,
    @field:Json(name = "rating") val rating: Any? = null,
    @field:Json(name = "category_id") val categoryId: String? = null,
    @field:Json(name = "container_extension") val containerExtension: String? = null,
    @field:Json(name = "custom_sid") val customSid: String? = null,
    @field:Json(name = "direct_source") val directSource: String? = null,
    @field:Json(name = "added") val added: String? = null
)

@JsonClass(generateAdapter = true)
data class XtreamSeriesDto(
    @field:Json(name = "num") val num: Int? = null,
    @field:Json(name = "name") val name: String? = null,
    @field:Json(name = "series_id") val seriesId: Any? = null,
    @field:Json(name = "cover") val cover: String? = null,
    @field:Json(name = "category_id") val categoryId: String? = null,
    @field:Json(name = "rating") val rating: Any? = null,
    @field:Json(name = "overview") val overview: String? = null
)

@JsonClass(generateAdapter = true)
data class XtreamSeriesInfoResponseDto(
    @field:Json(name = "seasons") val seasons: List<XtreamSeasonDto>? = null,
    @field:Json(name = "episodes") val episodes: Map<String, List<XtreamEpisodeDto>>? = null
)

@JsonClass(generateAdapter = true)
data class XtreamSeasonDto(
    @field:Json(name = "air_date") val airDate: String? = null,
    @field:Json(name = "episode_count") val episodeCount: Int? = null,
    @field:Json(name = "id") val id: Int? = null,
    @field:Json(name = "name") val name: String? = null,
    @field:Json(name = "overview") val overview: String? = null,
    @field:Json(name = "season_number") val seasonNumber: Int? = null,
    @field:Json(name = "cover") val cover: String? = null
)

@JsonClass(generateAdapter = true)
data class XtreamEpisodeDto(
    @field:Json(name = "id") val id: Any? = null,
    @field:Json(name = "title") val title: String? = null,
    @field:Json(name = "episode_num") val episodeNum: Any? = null,
    @field:Json(name = "season") val season: Any? = null,
    @field:Json(name = "container_extension") val containerExtension: String? = null,
    @field:Json(name = "duration") val duration: String? = null,
    @field:Json(name = "overview") val overview: String? = null,
    @field:Json(name = "direct_source") val directSource: String? = null
)

@JsonClass(generateAdapter = true)
data class XtreamEpgListingDto(
    @field:Json(name = "id") val id: String? = null,
    @field:Json(name = "epg_id") val epgId: String? = null,
    @field:Json(name = "title") val title: String? = null,
    @field:Json(name = "lang") val lang: String? = null,
    @field:Json(name = "start") val start: String? = null,
    @field:Json(name = "end") val end: String? = null,
    @field:Json(name = "description") val description: String? = null,
    @field:Json(name = "start_timestamp") val startTimestamp: Any? = null,
    @field:Json(name = "stop_timestamp") val stopTimestamp: Any? = null
)

@JsonClass(generateAdapter = true)
data class XtreamEpgResponseDto(
    @field:Json(name = "epg_listings") val epgListings: List<XtreamEpgListingDto>? = null
)
