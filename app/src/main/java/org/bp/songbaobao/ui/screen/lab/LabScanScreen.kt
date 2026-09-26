package org.bp.songbaobao.ui.screen.lab

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import org.bp.songbaobao.R
import org.bp.songbaobao.domain.CbcItems
import org.bp.songbaobao.ui.components.AppBackground
import org.bp.songbaobao.ui.components.GoldButton
import org.bp.songbaobao.ui.components.PanelCard
import org.bp.songbaobao.ui.components.rememberGalleryPicker
import org.bp.songbaobao.ui.theme.*

/**
 * 拍照 / 选图 → ML Kit 识别 → 展示识别文本与解析结果 → 交给表单确认。
 * 使用 Photo Picker（Android 13+ 无需存储权限），拍照走系统相机。
 */
@Composable
fun LabScanScreen(
    onBack: () -> Unit,
    onConfirm: (Map<String, Double>, String?) -> Unit,
    vm: LabViewModel = hiltViewModel()
) {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val ocrState by vm.ocrState.collectAsState()

    val openGallery = rememberGalleryPicker { uri ->
        imageUri = uri
        vm.recognize(uri)
    }

    val takePhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && imageUri != null) {
            vm.recognize(imageUri!!)
        }
    }

    AppBackground {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.scan_back), color = Gold)
            }
            Spacer(Modifier.weight(1f))
        }

        Text(
            stringResource(R.string.scan_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, color = GoldBright
        )
        Spacer(Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = ChartPulse.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                stringResource(R.string.scan_tip),
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = TextMain
            )
        }

        Spacer(Modifier.height(12.dp))

        // 拍照 / 相册
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GoldButton(
                onClick = {
                    // 先创建临时文件，再交由系统相机写入
                    val uri = vm.createTempImageUri()
                    if (uri != null) {
                        imageUri = uri
                        takePhoto.launch(uri)
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.scan_open_camera)) }

            OutlinedButton(
                onClick = openGallery,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDim)
            ) { Text(stringResource(R.string.scan_from_gallery)) }
        }

        Spacer(Modifier.height(12.dp))

        // 预览
        if (imageUri != null) {
            Card(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = stringResource(R.string.scan_photo_desc),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // 识别状态
        when (val s = ocrState) {
            is LabViewModel.OcrState.Idle -> {
                if (imageUri == null) {
                    org.bp.songbaobao.ui.components.EmptyHint(
                        stringResource(R.string.scan_idle_hint)
                    )
                }
            }
            is LabViewModel.OcrState.Loading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Gold, strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.scan_loading),
                        color = TextDim, style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            is LabViewModel.OcrState.Error -> {
                Text(
                    stringResource(R.string.scan_error, s.message),
                    color = DangerRed,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.scan_error_hint),
                    color = TextDim,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            is LabViewModel.OcrState.Done -> {
                PanelCard {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            if (s.values.isEmpty()) {
                                stringResource(R.string.scan_none)
                            } else {
                                stringResource(R.string.scan_found, s.values.size)
                            },
                            fontWeight = FontWeight.Bold,
                            color = if (s.values.isEmpty()) WarnAmber else SuccessGreen
                        )
                        Spacer(Modifier.height(8.dp))

                        if (s.values.isNotEmpty()) {
                            s.values.forEach { (key, v) ->
                                val item = CbcItems.get(key)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(stringResource(item.labelRes), modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        "${fmt(v)} ${item.unit}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            GoldButton(
                                onClick = {
                                    // 存入进程内持有者，表单页读取后清空
                                    org.bp.songbaobao.domain.OcrResultHolder.set(s.text, s.values)
                                    onConfirm(s.values, s.text)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    stringResource(R.string.scan_confirm),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.scan_raw_text),
                            style = MaterialTheme.typography.labelSmall, color = TextDim
                        )
                        Text(
                            s.text.ifBlank { stringResource(R.string.scan_no_text) }.take(600),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextDim,
                            maxLines = 12
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
    }
}

private fun fmt(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else "%.1f".format(v)
