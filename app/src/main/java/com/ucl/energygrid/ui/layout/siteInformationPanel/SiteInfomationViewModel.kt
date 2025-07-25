package com.ucl.energygrid.ui.layout.siteInformationPanel

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.ucl.energygrid.data.remote.apis.PinApi
import com.ucl.energygrid.data.model.PinRequest
import retrofit2.HttpException
import java.io.IOException


data class SiteInfoUiState(
    val note: String = "",
    val isPinned: Boolean = false,
    val isPosting: Boolean = false,
    val isNoteLoaded: Boolean = false,
    val postResult: String? = null
)

class SiteInformationViewModel(
    private val userId: Int,
    private val mineId: Int,
    private val api: PinApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(SiteInfoUiState())
    val uiState: StateFlow<SiteInfoUiState> = _uiState.asStateFlow()

    init {
        loadNoteAndPinStatus()
    }

    private fun loadNoteAndPinStatus() {
        viewModelScope.launch {
            try {
                val response = api.getPin(userId, mineId)
                if (response.isSuccessful) {
                    val pin = response.body()
                    _uiState.value = _uiState.value.copy(
                        note = pin?.note ?: "",
                        isPinned = pin != null,
                        isNoteLoaded = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isNoteLoaded = true,
                        postResult = "Failed to load note: ${response.code()}"
                    )
                }
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(
                    isNoteLoaded = true,
                    postResult = "Network error: ${e.message}"
                )
            } catch (e: HttpException) {
                _uiState.value = _uiState.value.copy(
                    isNoteLoaded = true,
                    postResult = "Server error: ${e.code()}"
                )
            }
        }
    }

    fun updateNote(newNote: String) {
        _uiState.value = _uiState.value.copy(note = newNote)
    }

    fun saveNote() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPosting = true, postResult = null)
            try {
                val request = PinRequest(mine_id = mineId, note = _uiState.value.note)
                val response = api.create_or_update_pin(userId, request)

                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isPosting = false,
                        isPinned = true,
                        postResult = "Note saved successfully"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isPosting = false,
                        postResult = "Failed to save note: ${response.code()}"
                    )
                }
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(
                    isPosting = false,
                    postResult = "Network error: ${e.message}"
                )
            } catch (e: HttpException) {
                _uiState.value = _uiState.value.copy(
                    isPosting = false,
                    postResult = "Server error: ${e.code()}"
                )
            }
        }
    }

    fun removePin() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPosting = true, postResult = null)
            try {
                val response = api.deletePin(userId, mineId)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isPosting = false,
                        isPinned = false,
                        note = "",
                        postResult = "Pin removed successfully"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isPosting = false,
                        postResult = "Failed to remove pin: ${response.code()}"
                    )
                }
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(
                    isPosting = false,
                    postResult = "Network error: ${e.message}"
                )
            } catch (e: HttpException) {
                _uiState.value = _uiState.value.copy(
                    isPosting = false,
                    postResult = "Server error: ${e.code()}"
                )
            }
        }
    }
}

