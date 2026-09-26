package org.bp.songbaobao.ui.screen.note

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import org.bp.songbaobao.R
import org.bp.songbaobao.data.local.entity.Note
import org.bp.songbaobao.data.local.entity.NotePhoto
import org.bp.songbaobao.ui.components.AppBackground
import org.bp.songbaobao.ui.components.AppTextField
import org.bp.songbaobao.ui.components.GoldButton
import org.bp.songbaobao.ui.components.PanelCard
import org.bp.songbaobao.ui.components.rememberGalleryPickerMultiple
import org.bp.songbaobao.ui.theme.*
import org.bp.songbaobao.util.normalizeTags
import org.bp.songbaobao.util.nowStamp

/** 留言新增 / 编辑：支持心情、标签、多图（最多 9 张） */
@Composable
fun NoteFormScreen(
    noteId: Long? = null,
    onBack: () -> Unit,
    vm: NoteViewModel = hiltViewModel()
) {
    var note by remember { mutableStateOf(vm.newNote()) }
    var loaded by remember { mutableStateOf(noteId == null) }
    var error by remember { mutableStateOf<String?>(null) }
    var pickedUris by remember { mutableStateOf(listOf<Uri>()) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    val existingPhotos by (if (noteId != null) vm.photosOf(noteId)
                           else kotlinx.coroutines.flow.flowOf(emptyList<NotePhoto>()))
        .collectAsState(initial = emptyList())
    var removeIds by remember { mutableStateOf(setOf<Long>()) }

    LaunchedEffect(noteId) {
        if (noteId != null) {
            val n = vm.load(noteId)
            if (n != null) note = n
            loaded = true
        }
    }

    // 图片选择（多选）
    val openGallery = rememberGalleryPickerMultiple(9) { uris -> pickedUris = pickedUris + uris }

    // 拍照
    val takePhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            pickedUris = pickedUris + tempCameraUri!!
        }
    }

    if (!loaded) {
        Box(Modifier.fillMaxSize().background(Navy)) { CircularProgressIndicator() }
        return
    }

    val moods = listOf("😊", "😄", "🥰", "😌", "😐", "😟", "😢", "🤒", "😴", "🎉")

    AppBackground {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            stringResource(
                if (noteId == null) R.string.note_form_title_add else R.string.note_form_title_edit
            ),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, color = GoldBright
        )
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppTextField(value = note.date, onValueChange = { note = note.copy(date = it) },
                label = stringResource(R.string.note_form_date),
                modifier = Modifier.weight(1f))
            AppTextField(value = note.time, onValueChange = { note = note.copy(time = it) },
                label = stringResource(R.string.note_form_time),
                modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))

        AppTextField(value = note.title, onValueChange = { note = note.copy(title = it) },
            label = stringResource(R.string.note_form_title),
            placeholder = stringResource(R.string.note_form_title_hint))
        Spacer(Modifier.height(10.dp))

        AppTextField(
            value = note.content,
            onValueChange = { note = note.copy(content = it) },
            label = stringResource(R.string.note_form_content),
            singleLine = false,
            modifier = Modifier.heightIn(min = 140.dp)
        )
        Text(
            stringResource(R.string.note_char_count, note.content.length),
            style = MaterialTheme.typography.labelSmall, color = TextDim
        )
        Spacer(Modifier.height(12.dp))

        // 心情
        Text(
            stringResource(R.string.note_form_mood),
            style = MaterialTheme.typography.labelMedium, color = TextDim
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            moods.forEach { m ->
                FilterChip(
                    selected = note.mood == m,
                    onClick = { note = note.copy(mood = if (note.mood == m) "" else m) },
                    label = { Text(m) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Gold.copy(alpha = 0.25f),
                        selectedLabelColor = GoldBright,
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        labelColor = TextDim
                    )
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        AppTextField(value = note.tags, onValueChange = { note = note.copy(tags = it) },
            label = stringResource(R.string.note_form_tags),
            placeholder = stringResource(R.string.note_form_tags_hint))
        Spacer(Modifier.height(12.dp))

        // 照片
        PanelCard {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    stringResource(R.string.note_photos_max),
                    style = MaterialTheme.typography.labelMedium, color = TextDim
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoldButton(
                        onClick = {
                            val uri = vm.createTempImageUri()
                            if (uri != null) {
                                tempCameraUri = uri
                                takePhoto.launch(uri)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.note_take_photo)) }
                    OutlinedButton(
                        onClick = openGallery,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDim)
                    ) { Text(stringResource(R.string.note_gallery)) }
                }
                Text(
                    stringResource(R.string.note_selected_count, pickedUris.size),
                    style = MaterialTheme.typography.labelSmall, color = TextDim
                )

                if (pickedUris.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        pickedUris.forEach { uri ->
                            Box {
                                Card(modifier = Modifier.size(64.dp)) {
                                    AsyncImage(
                                        model = uri, contentDescription = null,
                                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                                    )
                                }
                                IconButton(
                                    onClick = { pickedUris = pickedUris - uri },
                                    modifier = Modifier.size(24.dp).align(Alignment.TopEnd)
                                ) {
                                    Text("×", color = DangerRed,
                                        style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }

                // 已有照片（编辑时）
                if (existingPhotos.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.note_existing_photos),
                        style = MaterialTheme.typography.labelSmall, color = TextDim
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        existingPhotos.forEach { p ->
                            val marked = p.id in removeIds
                            Box {
                                Card(
                                    modifier = Modifier.size(64.dp),
                                    onClick = {
                                        removeIds = if (marked) removeIds - p.id else removeIds + p.id
                                    },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (marked) DangerRed.copy(alpha = 0.3f)
                                        else PanelBg
                                    )
                                ) {
                                    AsyncImage(
                                        model = vm.photoFile(p.thumb.ifBlank { p.filename }),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                if (marked) {
                                    Text("×", color = DangerRed, modifier = Modifier.align(Alignment.Center),
                                        style = MaterialTheme.typography.titleLarge)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = DangerRed, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(20.dp))

        val context = LocalContext.current
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GoldButton(
                onClick = {
                    if (note.title.isBlank() && note.content.isBlank() && pickedUris.isEmpty()) {
                        error = context.getString(R.string.note_err_need_content)
                        return@GoldButton
                    }
                    if (note.content.length > 5000) {
                        error = context.getString(R.string.note_err_too_long)
                        return@GoldButton
                    }
                    // 标记删除的已有照片
                    removeIds.forEach { id -> vm.deletePhoto(id) }
                    val final = note.copy(
                        tags = normalizeTags(note.tags),
                        updatedAt = nowStamp()
                    )
                    error = null
                    vm.save(final, pickedUris, onBack)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.action_save), fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDim)
            ) { Text(stringResource(R.string.action_cancel)) }
        }

        Spacer(Modifier.height(24.dp))
    }
    }
}

@Composable
fun NoteAddScreen(onBack: () -> Unit) {
    NoteFormScreen(noteId = null, onBack = onBack)
}

@Composable
fun NoteEditScreen(onBack: () -> Unit, vm: NoteViewModel = hiltViewModel()) {
    NoteFormScreen(noteId = vm.editId, onBack = onBack, vm = vm)
}
