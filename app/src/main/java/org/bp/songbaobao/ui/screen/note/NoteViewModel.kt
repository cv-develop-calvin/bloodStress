package org.bp.songbaobao.ui.screen.note

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.bp.songbaobao.data.local.dao.NotePhotoWithNote
import org.bp.songbaobao.data.local.dao.NoteWithMeta
import org.bp.songbaobao.data.local.entity.Note
import org.bp.songbaobao.data.repository.NoteRepository
import org.bp.songbaobao.data.repository.NoteStats
import org.bp.songbaobao.util.nowStamp
import org.bp.songbaobao.util.nowTimeStr
import org.bp.songbaobao.util.todayStr
import javax.inject.Inject

data class NoteUiState(
    val notes: List<NoteWithMeta> = emptyList(),
    val stats: NoteStats = NoteStats(0, 0, ""),
    val tags: List<Pair<String, Int>> = emptyList(),
    val recentPhotos: List<NotePhotoWithNote> = emptyList()
)

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val repo: NoteRepository,
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val editId: Long? = savedStateHandle.get<Long>("id")?.takeIf { it != 0L }

    private val _keyword = MutableStateFlow("")
    private val _tag = MutableStateFlow("")

    val keyword: StateFlow<String> = _keyword.asStateFlow()
    val tag: StateFlow<String> = _tag.asStateFlow()

    val uiState: StateFlow<NoteUiState> = combine(
        _keyword, _tag
    ) { kw, tg -> kw to tg }.flatMapLatest { (kw, tg) ->
        combine(repo.query(kw, tg), repo.statsFlow(), repo.recentPhotos(12)) { notes, stats, photos ->
            NoteUiState(notes, stats, emptyList(), photos)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NoteUiState())

    private val _tags = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val tags: StateFlow<List<Pair<String, Int>>> = _tags.asStateFlow()

    init {
        viewModelScope.launch { _tags.value = repo.allTags() }
    }

    fun setKeyword(kw: String) { _keyword.value = kw }
    fun setTag(tg: String) { _tag.value = tg }
    fun clearFilter() {
        _keyword.value = ""
        _tag.value = ""
    }

    // ---------------- 详情 ----------------
    fun photosOf(noteId: Long): Flow<List<org.bp.songbaobao.data.local.entity.NotePhoto>> =
        repo.photosOf(noteId)

    suspend fun load(id: Long): Note? = repo.get(id)

    fun save(note: Note, uris: List<Uri> = emptyList(), onDone: () -> Unit) {
        viewModelScope.launch {
            val id = repo.save(note)
            uris.forEach { uri ->
                runCatching { repo.addPhoto(id, uri) }
            }
            _tags.value = repo.allTags()
            onDone()
        }
    }

    fun delete(noteId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.delete(noteId)
            _tags.value = repo.allTags()
            onDone()
        }
    }

    fun deletePhoto(photoId: Long) {
        viewModelScope.launch { repo.deletePhoto(photoId) }
    }

    fun photoFile(name: String): java.io.File = repo.photoFile(name)

    fun newNote(): Note = Note(
        date = todayStr(),
        time = nowTimeStr(),
        title = "",
        content = "",
        mood = "😊",
        tags = "",
        createdAt = nowStamp(),
        updatedAt = nowStamp()
    )

    /** 拍照用临时 Uri */
    fun createTempImageUri(): Uri? = try {
        val dir = java.io.File(appContext.cacheDir, "images").apply { mkdirs() }
        val file = java.io.File(dir, "note_${System.currentTimeMillis()}.jpg")
        androidx.core.content.FileProvider.getUriForFile(
            appContext, "${appContext.packageName}.fileprovider", file
        )
    } catch (e: Exception) {
        null
    }
}
