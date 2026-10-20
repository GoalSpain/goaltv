package com.goaltv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goaltv.data.repository.DataRepository
import com.goaltv.model.Category
import com.goaltv.model.Episode
import com.goaltv.model.Series
import com.goaltv.model.VodMovie
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VodUiState(
    val vodCategories: List<Category> = emptyList(),
    val selectedVodCategory: Category? = null,
    val isMovieFavoritesSelected: Boolean = false,
    val vodMovies: List<VodMovie> = emptyList(),
    val selectedMovie: VodMovie? = null,
    val seriesCategories: List<Category> = emptyList(),
    val selectedSeriesCategory: Category? = null,
    val isSeriesFavoritesSelected: Boolean = false,
    val seriesList: List<Series> = emptyList(),
    val selectedSeries: Series? = null,
    val selectedSeasonNum: Int = 1,
    val seriesEpisodes: List<Episode> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

class VodViewModel(
    private val repository: DataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VodUiState())
    val uiState: StateFlow<VodUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val vodCats = repository.getVodCategories()
                val targetVodCat = _uiState.value.selectedVodCategory ?: vodCats.firstOrNull()
                val vodMovies = if (_uiState.value.isMovieFavoritesSelected) {
                    repository.getFavoriteMovies()
                } else if (targetVodCat != null) {
                    repository.getVodMovies(targetVodCat.id)
                } else {
                    emptyList()
                }

                val serCats = repository.getSeriesCategories()
                val targetSerCat = _uiState.value.selectedSeriesCategory ?: serCats.firstOrNull()
                val series = if (_uiState.value.isSeriesFavoritesSelected) {
                    repository.getFavoriteSeries()
                } else if (targetSerCat != null) {
                    repository.getSeriesList(targetSerCat.id)
                } else {
                    emptyList()
                }

                _uiState.value = _uiState.value.copy(
                    vodCategories = vodCats,
                    selectedVodCategory = targetVodCat,
                    vodMovies = vodMovies,
                    seriesCategories = serCats,
                    selectedSeriesCategory = targetSerCat,
                    seriesList = series,
                    isLoading = false
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun selectVodCategory(category: Category?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedVodCategory = category, isMovieFavoritesSelected = false, isLoading = true)
            try {
                val movies = repository.getVodMovies(category?.id)
                _uiState.value = _uiState.value.copy(vodMovies = movies, isLoading = false)
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun selectMovieFavorites() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedVodCategory = null, isMovieFavoritesSelected = true, isLoading = true)
            try {
                val favs = repository.getFavoriteMovies()
                _uiState.value = _uiState.value.copy(vodMovies = favs, isLoading = false)
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun selectMovie(movie: VodMovie?) {
        _uiState.value = _uiState.value.copy(selectedMovie = movie)
        if (movie != null) {
            viewModelScope.launch {
                val enriched = com.goaltv.data.network.TmdbHelper.enrichMovie(movie)
                _uiState.value = _uiState.value.copy(
                    selectedMovie = enriched,
                    vodMovies = _uiState.value.vodMovies.map { if (it.id == enriched.id) enriched else it }
                )
            }
        }
    }

    fun toggleMovieFavorite(movieId: String) {
        repository.toggleFavoriteMovie(movieId)
        viewModelScope.launch {
            try {
                val updated = repository.getVodMovies(_uiState.value.selectedVodCategory?.id)
                val currentSelected = _uiState.value.selectedMovie
                _uiState.value = _uiState.value.copy(
                    vodMovies = updated,
                    selectedMovie = if (currentSelected?.id == movieId) {
                        currentSelected.copy(isFavorite = !currentSelected.isFavorite)
                    } else currentSelected
                )
            } catch (_: Exception) {}
        }
    }

    fun selectSeriesCategory(category: Category?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedSeriesCategory = category, isSeriesFavoritesSelected = false, isLoading = true)
            try {
                val series = repository.getSeriesList(category?.id)
                _uiState.value = _uiState.value.copy(seriesList = series, isLoading = false)
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun selectSeriesFavorites() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedSeriesCategory = null, isSeriesFavoritesSelected = true, isLoading = true)
            try {
                val favs = repository.getFavoriteSeries()
                _uiState.value = _uiState.value.copy(seriesList = favs, isLoading = false)
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun selectSeries(series: Series?) {
        viewModelScope.launch {
            if (series == null) {
                _uiState.value = _uiState.value.copy(selectedSeries = null, seriesEpisodes = emptyList())
                return@launch
            }
            _uiState.value = _uiState.value.copy(selectedSeries = series, isLoading = true)
            try {
                val enriched = com.goaltv.data.network.TmdbHelper.enrichSeries(series)
                val episodes = repository.getSeriesEpisodes(series.id)
                _uiState.value = _uiState.value.copy(
                    selectedSeries = enriched,
                    selectedSeasonNum = 1,
                    seriesEpisodes = episodes,
                    seriesList = _uiState.value.seriesList.map { if (it.id == enriched.id) enriched else it },
                    isLoading = false
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun selectSeason(seasonNum: Int) {
        _uiState.value = _uiState.value.copy(selectedSeasonNum = seasonNum)
    }

    fun toggleSeriesFavorite(seriesId: String) {
        repository.toggleFavoriteSeries(seriesId)
        viewModelScope.launch {
            try {
                val updated = repository.getSeriesList(_uiState.value.selectedSeriesCategory?.id)
                val currentSelected = _uiState.value.selectedSeries
                _uiState.value = _uiState.value.copy(
                    seriesList = updated,
                    selectedSeries = if (currentSelected?.id == seriesId) {
                        currentSelected.copy(isFavorite = !currentSelected.isFavorite)
                    } else currentSelected
                )
            } catch (_: Exception) {}
        }
    }
}
