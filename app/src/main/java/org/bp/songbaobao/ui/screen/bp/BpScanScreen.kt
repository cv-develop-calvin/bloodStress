package org.bp.songbaobao.ui.screen.bp

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
import org.bp.songbaobao.domain.BpOcrParser
import org.bp.songbaobao.domain.BpOcrResultHolder
import org.bp.songbaobao.ui.components.AppBackground
import org.bp.songbaobao.ui.components.GoldButton
import org.bp.songbaobao.ui.components.PanelCard
import org.bp.songbaobao.ui.theme.*

/**
 * 拍照 / 选图 → ML Kit 识别 → 解析收缩压/舒张压/心率 → 用户核对 → 交给表单确认。
 * 逻辑与化验单 LabScanScreen 一致，区别在识别结果落地为血压三项而非化验指标。
 */
@Composable
fun BpScanScreen(
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    vm: BpViewModel = hiltViewModel()
) {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val ocrState by vm.ocrState.collectAsState()

    // 识别结果里可被用户改写的三个值（初始来自 OCR 解析）
    var sysText by remember { mutableStateOf("") }
    var diaText by remember { mutableStateOf("") }
    var pulseText by remember { mutableStateOf("") }

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) { imageUri = uri; vm.recognize(uri) } }

    val takePhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success && imageUri != null) vm.recognize(imageUri!!) }

    // 识别完成时把解析结果灌入可编辑字段
    LaunchedEffect(ocrState) {
        if (ocrState is BpViewModel.OcrState.Done) {
            val r = (ocrState as BpViewModel.OcrState.Done).result
            sysText = r.systolic?.toString() ?: ""
            diaText = r.diastolic?.toString() ?: ""
            pulseText = r.pulse?.toString() ?: ""
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
                stringResource(R.string.bp_scan_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = GoldBright
            )
            Spacer(Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = ChartPulse.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.bp_scan_tip),
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
                is BpViewModel.OcrState.Idle -> {
                    if (imageUri == null) {
                        org.bp.songbaobao.ui.components.EmptyHint(
                            stringResource(R.string.scan_idle_hint)
                        )
                    }
                }
                is BpViewModel.OcrState.Loading -> {
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
                is BpViewModel.OcrState.Error -> {
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
                is BpViewModel.OcrState.Done -> {
                    PanelCard {
                        Column(modifier = Modifier.padding(12.dp)) {
                            val cnt = s.result.recognizedCount
                            Text(
                                if (cnt == 0) stringResource(R.string.scan_none)
                                else stringResource(R.string.bp_scan_found, cnt),
                                fontWeight = FontWeight.Bold,
                                color = if (cnt == 0) WarnAmber else SuccessGreen
                            )
                            Spacer(Modifier.height(10.dp))

                            BpScanField(stringResource(R.string.bp_form_sys), sysText) { sysText = it }
                            BpScanField(stringResource(R.string.bp_form_dia), diaText) { diaText = it }
                            BpScanField(stringResource(R.string.bp_form_pulse), pulseText) { pulseText = it }

                            Spacer(Modifier.height(10.dp))
                            GoldButton(
                                onClick = {
                                    val sys = sysText.toIntOrNull()
                                    val dia = diaText.toIntOrNull()
                                    val pulse = pulseText.toIntOrNull()
                                    // 至少要识别到收缩压或舒张压之一才允许确认
                                    if (sys != null || dia != null) {
                                        BpOcrResultHolder.set(
                                            BpOcrParser.BpOcrResult(sys, dia, pulse, s.result.rawText)
                                        )
                                        onConfirm()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = sysText.toIntOrNull() != null || diaText.toIntOrNull() != null
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
                                s.result.rawText.ifBlank { stringResource(R.string.scan_no_text) }.take(600),
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
private fun BpScanField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { c -> c.isDigit() }.take(3)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Gold,
            unfocusedBorderColor = TextDim,
            focusedLabelColor = Gold,
            cursorColor = Gold
        )
    )
}
