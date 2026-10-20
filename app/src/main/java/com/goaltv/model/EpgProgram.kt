package com.goaltv.model

data class EpgProgram(
    val id: String,
    val channelId: String,
    val title: String,
    val description: String? = null,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val isLive: Boolean = false
) {
    val isCurrentlyLive: Boolean
        get() {
            val now = System.currentTimeMillis() / 1000
            return now in startTimestamp..endTimestamp
        }

    val progressPercent: Float
        get() {
            val now = System.currentTimeMillis() / 1000
            if (now < startTimestamp) return 0f
            if (now > endTimestamp) return 1f
            val total = endTimestamp - startTimestamp
            if (total <= 0) return 0f
            return ((now - startTimestamp).toFloat() / total.toFloat()).coerceIn(0f, 1f)
        }
}
