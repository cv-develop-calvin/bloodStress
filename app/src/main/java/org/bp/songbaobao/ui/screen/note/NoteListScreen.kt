package org.bp.songbaobao.ui.screen.note

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import org.bp.songbaobao.R
import org.bp.songbaobao.ui.components.*
import org.bp.songbaobao.ui.theme.*
import org.bp.songbaobao.util.parseTags

@Composable
fun NoteListScreen(
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    vm: NoteViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsState()
    val tags by vm.tags.collectAsState()
    val keyword by vm.keyword.collectAsState()
    val tag by vm.tag.collectAsState()

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd, containerColor = Gold, contentColor = Navy) {
                Text("＋", fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
      AppBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(Modifier.height(12.dp))
                HeroCard {
                    ZodiacChip(stringResource(R.string.note_chip))
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${state.stats.notes}", style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold, color = GoldBright)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.note_count_notes), color = TextDim)
                        Spacer(Modifier.width(12.dp))
                        Text("${state.stats.photos}", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold, color = GoldBright)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.note_count_photos), color = TextDim)
                    }
                    if (state.stats.since.isNotBlank()) {
                        Text(
                            stringResource(R.string.note_since, state.stats.since),
                            style = MaterialTheme.typography.labelSmall, color = TextDim
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // 搜索
            item {
                AppTextField(
                    value = keyword,
                    onValueChange = vm::setKeyword,
                    label = stringResource(R.string.note_search_hint)
                )
                Spacer(Modifier.height(8.dp))
            }

            // 标签
            if (tags.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (tag.isNotBlank() || keyword.isNotBlank()) {
                            FilterChip(
                                selected = false,
                                onClick = vm::clearFilter,
                                label = {
                                    Text(
                                        stringResource(R.string.note_filter_all),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                        tags.take(6).forEach { (name, count) ->
                            FilterChip(
                                selected = tag == name,
                                onClick = { vm.setTag(if (tag == name) "" else name) },
                                label = { Text("#$name $count", style = MaterialTheme.typography.labelSmall) },
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
                }
            }

            // 照片墙
            if (state.recentPhotos.isNotEmpty()) {
                item {
                    PanelCard {
                        Column(modifier = Modifier.padding(12.dp)) {
                            SectionTitle(stringResource(R.string.note_photo_wall))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                state.recentPhotos.take(4).forEach { p ->
                                    val file = vm.photoFile(p.thumb.ifBlank { p.filename })
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f),
                                        onClick = { onOpen(p.noteId) }
                                    ) {
                                        AsyncImage(
                                            model = file,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            if (state.notes.isEmpty()) {
                item {
                    EmptyHint(
                        if (keyword.isNotBlank() || tag.isNotBlank()) {
                            stringResource(R.string.note_no_match)
                        } else {
                            stringResource(R.string.note_empty_title)
                        },
                        stringResource(R.string.note_empty_hint)
                    )
                }
            } else {
                items(state.notes, key = { it.id }) { n ->
                    PanelCard(modifier = Modifier.padding(bottom = 10.dp)) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (n.mood.isNotBlank()) {
                                    Text(n.mood, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(
                                    n.title.ifBlank { stringResource(R.string.note_untitled) },
                                    fontWeight = FontWeight.Bold,
                                    color = GoldBright,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Text("${n.date} ${n.time}", style = MaterialTheme.typography.labelSmall, color = TextDim)

                            if (n.content.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    n.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (n.tags.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    parseTags(n.tags).forEach { t ->
                                        AssistChip(
                                            onClick = { vm.setTag(t) },
                                            label = { Text("#$t", style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                            }

                            if (n.photoCount > 0) {
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    n.cover?.let { cover ->
                                        Card(
                                            modifier = Modifier.size(56.dp),
                                            onClick = { onOpen(n.id) }
                                        ) {
                                            AsyncImage(
                                                model = vm.photoFile(cover),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        stringResource(R.string.note_photo_count, n.photoCount),
                                        style = MaterialTheme.typography.bodySmall, color = TextDim
                                    )
                                }
                            }

                            Spacer(Modifier.height(4.dp))
                            TextButton(onClick = { onOpen(n.id) }) {
                                Text(stringResource(R.string.note_view_detail), color = Gold)
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
      }
    }
}
