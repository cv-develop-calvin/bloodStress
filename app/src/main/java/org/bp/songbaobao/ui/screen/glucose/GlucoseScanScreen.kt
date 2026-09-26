package org.bp.songbaobao.ui.screen.glucose

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import org.bp.songbaobao.R
import org.bp.songbaobao.domain.GlucoseOcrParser
import org.bp.songbaobao.domain.GlucoseOcrResultHolder
import org.bp.songbaobao.ui.components.*
import org.bp.songbaobao.ui.theme.*

/**
 * 拍照 / 选图 → ML Kit 识别 → 解析血糖值 → 用户核对 → 交给表单确认。
 * 逻辑与化验单 LabScanScreen、血压 BpScanScreen 一致，区别在识别落地为血糖单值。
 */
@Composable
fun GlucoseScanScreen(
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    vm: GlucoseViewModel = hiltViewModel()
) {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val ocrState by vm.ocrState.collectAsState()

    // 识别结果里可被用户改写的值（初始来自 OCR 解析）
    var valueText by remember { mutableStateOf("") }

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) { imageUri = uri; vm.recognize(uri) } }

    val takePhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success && imageUri != null) vm.recognize(imageUri!!) }

    // 识别完成时把解析结果灌入可编辑字段
    LaunchedEffect(ocrState) {
        if (ocrState is GlucoseViewModel.OcrState.Done) {
            val r = (ocrState as GlucoseViewModel.OcrState.Done).result
            valueText = r.value?.let { "%.1f".format(it) } ?: ""
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
                stringResource(R.string.glucose_scan_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = GoldBright
            )
            Spacer(Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = ChartPulse.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.glucose_scan_tip),
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMain
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GoldButton(
                    onClick = {
                        val uri = vm.createTempImageUri()
                        if (uri != null) {
                            imageUri = uri
                            takePhoto.launch(uri)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.scan_open_camera)) }

                OutlinedButton(
                    onClick = {
                        pickMedia.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDim)
                ) { Text(stringResource(R.string.scan_from_gallery)) }
            }

            Spacer(Modifier.height(12.dp))

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

            when (val s = ocrState) {
                is GlucoseViewModel.OcrState.Idle -> {
                    if (imageUri == null) {
                        EmptyHint(stringResource(R.string.scan_idle_hint))
                    }
                }
                is GlucoseViewModel.OcrState.Loading -> {
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
                is GlucoseViewModel.OcrState.Error -> {
                    Text(
                        stringResource(R.string.scan_error, s.message),
                        color = DangerRed, style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.scan_error_hint),
                        color = TextDim, style = MaterialTheme.typography.bodySmall
                    )
                }
                is GlucoseViewModel.OcrState.Done -> {
                    PanelCard {
                        Column(modifier = Modifier.padding(12.dp)) {
                            val cnt = s.result.recognizedCount
                            Text(
                                if (cnt == 0) stringResource(R.string.scan_none)
                                else stringResource(R.string.glucose_scan_found, cnt),
                                fontWeight = FontWeight.Bold,
                                color = if (cnt == 0) WarnAmber else SuccessGreen
                            )
                            Spacer(Modifier.height(10.dp))

                            GlucoseScanField(
                                stringResource(R.string.glucose_form_value),
                                valueText
                            ) { valueText = it }

                            Spacer(Modifier.height(10.dp))
                            GoldButton(
                                onClick = {
                                    val v = valueText.toFloatOrNull()
                                    if (v != null) {
                                        GlucoseOcrResultHolder.set(
                                            GlucoseOcrParser.GlucoseOcrResult(
                                                v,
                                                s.result.unit,
                                                s.result.rawText
                                            )
                                        )
                                        onConfirm()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = valueText.toFloatOrNull() != null
                            ) {
                                Text(
                                    stringResource(R.string.scan_confirm),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.scan_raw_text),
                                style = MaterialTheme.typography.labelSmall, color = TextDim
                            )
                            Text(
                                s.result.rawText.ifBlank { stringResource(R.string.scan_no_text) }
                                    .take(600),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextDim, maxLines = 12
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GlucoseScanField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { c -> c.isDigit() || c == '.' }.take(6)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Gold,
            unfocusedBorderColor = TextDim,
            focusedLabelColor = Gold,
            cursorColor = Gold
        )
    )
}
