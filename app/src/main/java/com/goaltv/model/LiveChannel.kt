package com.goaltv.model

data class LiveChannel(
    val id: String,
    val name: String,
    val iconUrl: String? = null,
    val streamUrl: String,
    val categoryId: String,
    val epgId: String? = null,
    val streamType: String? = "live",
    val channelNumber: Int? = null,
    val isFavorite: Boolean = false,
    val hasCatchup: Boolean = false,
    val catchupDays: Int = 0
)
