package org.bp.songbaobao.ui.screen.note

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import org.bp.songbaobao.data.local.entity.Note
import org.bp.songbaobao.data.local.entity.NotePhoto
import org.bp.songbaobao.ui.components.AppTextField
import org.bp.songbaobao.ui.components.GoldButton
import org.bp.songbaobao.ui.components.PanelCard
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
    val pickMultiple = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(9)
    ) { uris -> pickedUris = pickedUris + uris }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            if (noteId == null) "写留言" else "编辑留言",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, color = GoldBright
        )
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppTextField(value = note.date, onValueChange = { note = note.copy(date = it) },
                label = "日期", modifier = Modifier.weight(1f))
            AppTextField(value = note.time, onValueChange = { note = note.copy(time = it) },
                label = "时间", modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))

        AppTextField(value = note.title, onValueChange = { note = note.copy(title = it) },
            label = "标题", placeholder = "如：第一次翻身")
        Spacer(Modifier.height(10.dp))

        AppTextField(
            value = note.content,
            onValueChange = { note = note.copy(content = it) },
            label = "留言内容",
            singleLine = false,
            modifier = Modifier.heightIn(min = 140.dp)
        )
        Text("${note.content.length} 字 · 最多 5000 字",
            style = MaterialTheme.typography.labelSmall, color = TextDim)
        Spacer(Modifier.height(12.dp))

        // 心情
        Text("心情", style = MaterialTheme.typography.labelMedium, color = TextDim)
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
            label = "标签", placeholder = "用逗号分隔，如：成长,第一次,体检")
        Spacer(Modifier.height(12.dp))

        // 照片
        PanelCard {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("照片（最多 9 张）", style = MaterialTheme.typography.labelMedium, color = TextDim)
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
                    ) { Text("📸 拍照") }
                    OutlinedButton(
                        onClick = {
                            pickMultiple.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDim)
                    ) { Text("🖼️ 相册") }
                }
                Text("已选 ${pickedUris.size} 张", style = MaterialTheme.typography.labelSmall, color = TextDim)

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
                    Text("已有照片（点击标记删除）", style = MaterialTheme.typography.labelSmall, color = TextDim)
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

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GoldButton(
                onClick = {
                    if (note.title.isBlank() && note.content.isBlank() && pickedUris.isEmpty()) {
                        error = "请至少填写标题或留言内容"
                        return@GoldButton
                    }
                    if (note.content.length > 5000) {
                        error = "留言内容过长（最多 5000 字）"
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
            ) { Text("保存", fontWeight = FontWeight.Bold) }
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDim)
            ) { Text("取消") }
        }

        Spacer(Modifier.height(24.dp))
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
