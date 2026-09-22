package org.bp.songbaobao.ui.screen.note

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
import org.bp.songbaobao.ui.components.EmptyHint
import org.bp.songbaobao.ui.components.PanelCard
import org.bp.songbaobao.ui.theme.*
import org.bp.songbaobao.util.parseTags

@Composable
fun NoteDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    vm: NoteViewModel = hiltViewModel()
) {
    val noteId = vm.editId
    var note by remember { mutableStateOf<org.bp.songbaobao.data.local.entity.Note?>(null) }
    var loaded by remember { mutableStateOf(false) }
    val photos by (if (noteId != null) vm.photosOf(noteId)
                   else kotlinx.coroutines.flow.flowOf(emptyList<org.bp.songbaobao.data.local.entity.NotePhoto>()))
        .collectAsState(initial = emptyList())

    LaunchedEffect(noteId) {
        if (noteId != null) {
            note = vm.load(noteId)
            loaded = true
        } else {
            loaded = true
        }
    }

    if (!loaded) {
        Box(Modifier.fillMaxSize().background(Navy)) { CircularProgressIndicator() }
        return
    }

    val n = note
    if (n == null) {
        Box(Modifier.fillMaxSize().background(Navy)) {
            EmptyHint("留言不存在")
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ 返回", color = Gold) }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { onEdit(n.id) }) { Text("编辑", color = Gold) }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (n.mood.isNotBlank()) {
                Text(n.mood, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                n.title.ifBlank { "无标题" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = GoldBright
            )
        }
        Text("${n.date} ${n.time}", style = MaterialTheme.typography.labelMedium, color = TextDim)

        if (n.tags.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                parseTags(n.tags).forEach { t ->
                    AssistChip(onClick = {}, label = { Text("#$t", style = MaterialTheme.typography.labelSmall) })
                }
            }
        }

        if (n.content.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            PanelCard {
                Text(
                    n.content,
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        if (photos.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            PanelCard {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("📷 照片（${photos.size}）", fontWeight = FontWeight.Bold, color = GoldBright)
                    Spacer(Modifier.height(8.dp))
                    // 每行 3 张
                    photos.chunkedSafe(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            row.forEach { p ->
                                var confirm by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.weight(1f)) {
                                    Card(modifier = Modifier.aspectRatio(1f)) {
                                        AsyncImage(
                                            model = vm.photoFile(p.filename),
                                            contentDescription = p.caption.ifBlank { null },
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    IconButton(
                                        onClick = { confirm = true },
                                        modifier = Modifier.align(Alignment.TopEnd).size(28.dp)
                                    ) {
                                        Text("×", color = DangerRed)
                                    }
                                    if (confirm) {
                                        org.bp.songbaobao.ui.screen.bp.ConfirmDelete(
                                            text = "删除这张照片？",
                                            onDismiss = { confirm = false },
                                            onConfirm = { vm.deletePhoto(p.id); confirm = false }
                                        )
                                    }
                                }
                            }
                            if (row.size < 3) {
                                Spacer(Modifier.weight((3 - row.size).toFloat()))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

private fun <T> List<T>.chunkedSafe(size: Int): List<List<T>> {
    val out = mutableListOf<List<T>>()
    var i = 0
    while (i < this.size) {
        out.add(subList(i, minOf(i + size, this.size)))
        i += size
    }
    return out
}
