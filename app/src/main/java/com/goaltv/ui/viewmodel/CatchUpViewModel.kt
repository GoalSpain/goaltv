package com.goaltv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goaltv.data.repository.DataRepository
import com.goaltv.model.EpgProgram
import com.goaltv.model.LiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CatchUpUiState(
    val channels: List<LiveChannel> = emptyList(),
    val filteredChannels: List<LiveChannel> = emptyList(),
    val selectedChannel: LiveChannel? = null,
    val epgPrograms: List<EpgProgram> = emptyList(),
    val isLoadingChannels: Boolean = false,
    val isLoadingEpg: Boolean = false,
    val searchQuery: String = "",
    val errorMessage: String? = null
)

class CatchUpViewModel(
    private val repository: DataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatchUpUiState())
    val uiState: StateFlow<CatchUpUiState> = _uiState.asStateFlow()

    init {
        loadCatchUpData()
    }

    fun loadCatchUpData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingChannels = true, errorMessage = null)
            try {
                val channels = repository.getCatchupChannels()
                val initialSelected = channels.firstOrNull()
                _uiState.value = _uiState.value.copy(
                    channels = channels,
                    filteredChannels = filterChannels(channels, _uiState.value.searchQuery),
                    selectedChannel = initialSelected,
                    isLoadingChannels = false
                )
                if (initialSelected != null) {
                    loadEpgForChannel(initialSelected)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingChannels = false,
                    errorMessage = "Error al cargar canales con Catch-up"
                )
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        val filtered = filterChannels(_uiState.value.channels, query)
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredChannels = filtered
        )
    }

    private fun filterChannels(list: List<LiveChannel>, query: String): List<LiveChannel> {
        if (query.isBlank()) return list
        return list.filter { it.name.contains(query, ignoreCase = true) }
    }

    fun selectChannel(channel: LiveChannel) {
        if (_uiState.value.selectedChannel?.id == channel.id) return
        _uiState.value = _uiState.value.copy(
            selectedChannel = channel,
            epgPrograms = emptyList()
        )
        loadEpgForChannel(channel)
    }

    private fun loadEpgForChannel(channel: LiveChannel) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingEpg = true)
            try {
                val epg = repository.getCatchupEpg(channel.id)
                _uiState.value = _uiState.value.copy(
                    epgPrograms = epg,
                    isLoadingEpg = false
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    epgPrograms = emptyList(),
                    isLoadingEpg = false
                )
            }
        }
    }

    fun buildCatchupStreamUrl(channel: LiveChannel, program: EpgProgram): String {
        val durationMinutes = ((program.endTimestamp - program.startTimestamp) / 60).toInt().coerceAtLeast(1)
        return repository.buildCatchupStreamUrl(
            streamId = channel.id,
            startTimestampSec = program.startTimestamp,
            durationMinutes = durationMinutes
        )
    }
}
