package org.bp.songbaobao.ui.screen.report

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import org.bp.songbaobao.ui.components.renderBpLineChart
import org.bp.songbaobao.ui.components.renderGlucoseLineChart
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bp.songbaobao.R
import org.bp.songbaobao.ui.components.AppBackground
import org.bp.songbaobao.ui.components.EmptyHint
import org.bp.songbaobao.ui.components.GoldButton
import org.bp.songbaobao.ui.components.PanelCard
import org.bp.songbaobao.ui.components.SectionTitle
import org.bp.songbaobao.ui.components.StatCard
import org.bp.songbaobao.ui.theme.TextDim
import org.bp.songbaobao.ui.theme.TextMain
import org.bp.songbaobao.ui.theme.SongBaoBaoTheme
import java.io.File
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    vm: ReportViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val data by vm.data.collectAsState()
    val loading by vm.loading.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }

    SongBaoBaoTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.report_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, stringResource(R.string.back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = TextMain,
                        navigationIconContentColor = TextMain
                    )
                )
            }
        ) { inner ->
            Box(Modifier.fillMaxSize()) {
                AppBackground(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.fillMaxSize().padding(inner)
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(Modifier.height(8.dp))
                        SectionTitle(stringResource(R.string.report_title))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(
                                R.string.report_summary,
                                "${data?.rangeFrom ?: ""} ~ ${data?.rangeTo ?: ""}"
                            ),
                            color = TextDim,
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(Modifier.height(12.dp))
                        data?.let { d ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatCard(
                                    stringResource(R.string.nav_bp),
                                    "${d.bp.size}",
                                    stringResource(R.string.title_all_records),
                                    modifier = Modifier.weight(1f)
                                )
                                StatCard(
                                    stringResource(R.string.nav_glucose),
                                    "${d.glucose.size}",
                                    stringResource(R.string.title_all_records),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            d.adherence?.let { adh ->
                                PanelCard {
                                    Column(Modifier.padding(12.dp)) {
                                        Text(
                                            stringResource(R.string.report_compliance) + "：${adh.rate}%",
                                            color = TextMain,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            stringResource(R.string.med_today_progress, adh.taken, adh.expected),
                                            color = TextDim,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        } ?: run {
                            if (!loading) EmptyHint(stringResource(R.string.report_empty))
                        }

                        Spacer(Modifier.height(20.dp))
                        GoldButton(
                            onClick = {
                                val d = data ?: return@GoldButton
                                busy = true
                                scope.launch {
                                    val appCtx = context.applicationContext
                                    // 图表渲染需在主线程（MPAndroidChart 离屏绘制），PDF 组装在 IO
                                    val bpBmp = withContext(Dispatchers.Main) {
                                        runCatching { renderBpLineChart(appCtx, 523, 240, d.bp) }.getOrNull()
                                    }
                                    val gluBmp = withContext(Dispatchers.Main) {
                                        runCatching { renderGlucoseLineChart(appCtx, 523, 220, d.glucose) }.getOrNull()
                                    }
                                    val file = withContext(Dispatchers.IO) {
                                        runCatching { ReportPdfBuilder.build(appCtx, d, bpBmp, gluBmp) }
                                            .getOrNull()
                                    }
                                    busy = false
                                    if (file != null) {
                                        sharePdf(context, file)
                                    } else {
                                        android.widget.Toast.makeText(
                                            context,
                                            context.getString(R.string.report_failed, "PDF"),
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            enabled = data != null && !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (busy) {
                                CircularProgressIndicator(
                                    Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(stringResource(R.string.report_generate))
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            stringResource(R.string.report_disclaimer),
                            color = TextDim,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

private fun sharePdf(context: Context, file: File) {
    val uri: Uri = FileProvider.getUriForFile(
        context,
        context.packageName + ".fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.report_share)))
}
