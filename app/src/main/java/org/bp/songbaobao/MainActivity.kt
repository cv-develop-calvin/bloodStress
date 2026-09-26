package org.bp.songbaobao

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import org.bp.songbaobao.R
import org.bp.songbaobao.reminder.NotificationHelper
import org.bp.songbaobao.ui.components.AppBottomBar
import org.bp.songbaobao.ui.navigation.Screen
import org.bp.songbaobao.ui.screen.about.AboutScreen
import org.bp.songbaobao.ui.screen.about.SettingsScreen
import org.bp.songbaobao.ui.screen.bp.BpAddScreen
import org.bp.songbaobao.ui.screen.bp.BpEditScreen
import org.bp.songbaobao.ui.screen.bp.BpListScreen
import org.bp.songbaobao.ui.screen.bp.BpScanScreen
import org.bp.songbaobao.ui.screen.bp.BpTrendScreen
import org.bp.songbaobao.ui.screen.glucose.GlucoseAddScreen
import org.bp.songbaobao.ui.screen.glucose.GlucoseAllScreen
import org.bp.songbaobao.ui.screen.glucose.GlucoseEditScreen
import org.bp.songbaobao.ui.screen.glucose.GlucoseListScreen
import org.bp.songbaobao.ui.screen.glucose.GlucoseScanScreen
import org.bp.songbaobao.ui.screen.lab.LabAddScreen
import org.bp.songbaobao.ui.screen.lab.LabEditScreen
import org.bp.songbaobao.ui.screen.lab.LabScanScreen
import org.bp.songbaobao.ui.screen.lab.LabScreen
import org.bp.songbaobao.ui.screen.legal.LegalScreen
import org.bp.songbaobao.ui.screen.med.MedAddScreen
import org.bp.songbaobao.ui.screen.med.MedEditScreen
import org.bp.songbaobao.ui.screen.med.MedScreen
import org.bp.songbaobao.ui.screen.note.NoteAddScreen
import org.bp.songbaobao.ui.screen.note.NoteDetailScreen
import org.bp.songbaobao.ui.screen.note.NoteEditScreen
import org.bp.songbaobao.ui.screen.note.NoteListScreen
import org.bp.songbaobao.ui.screen.subscription.SubscriptionScreen
import org.bp.songbaobao.ui.theme.SongBaoBaoTheme
import org.bp.songbaobao.util.LanguageManager
import org.bp.songbaobao.util.UserPrefs
import org.bp.songbaobao.util.PrivacyConsent

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * 注入应用内语言。
     *
     * 必须在 attachBaseContext 而非 onCreate 里处理：
     * 这里是创建 Activity Context 的最早时机，之后所有资源查找
     * （包括 Compose 的 stringResource）都会带上目标 Locale。
     */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.wrap(newBase))
    }

    override fun recreate() {
        // 切换语言后系统会重建 Activity，此处无需额外处理，
        // 保留覆写点便于将来加日志或过渡动画。
        super.recreate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NotificationHelper.createChannel(this)

        // Android 13+ 需要运行时授权通知
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }

        val startMed = intent?.getBooleanExtra("openMed", false) ?: false

        setContent {
            SongBaoBaoTheme {
                val navController = rememberNavController()
                val currentRoute by navController.currentBackStackEntryAsState()
                // 首次启动必须先取得隐私政策同意（健康数据属敏感个人信息），
                // 未同意前不进入功能页面
                var consented by remember {
                    mutableStateOf(PrivacyConsent.isAccepted(this@MainActivity))
                }
                // 点「查看完整政策」后临时让出屏幕，否则弹窗会盖住政策页；
                // 从政策页返回且仍未同意时，弹窗会再次出现
                var viewingPolicy by remember { mutableStateOf(false) }
                val onLegalPage = currentRoute?.destination?.route == Screen.Legal.route
                // 离开政策页后重置标记，使未同意的用户再次看到弹窗
                LaunchedEffect(onLegalPage) {
                    if (!onLegalPage) viewingPolicy = false
                }
                val showConsent = !consented && !(viewingPolicy && onLegalPage)

                if (showConsent) {
                    PrivacyConsentDialog(
                        onAgree = {
                            PrivacyConsent.accept(this@MainActivity)
                            consented = true
                        },
                        onViewPolicy = {
                            viewingPolicy = true
                            navController.navigate(Screen.Legal.route)
                        },
                        onDisagree = { finish() }
                    )
                }

                AppNavHost(
                    navController = navController,
                    startDestination = if (startMed) Screen.Med.route else Screen.Bp.route
                )
            }
        }
    }
}

/**
 * 隐私政策同意弹窗。
 * 未同意时应用不提供任何记录功能；选择「不同意」直接退出，
 * 符合《个人信息保护法》中敏感个人信息需单独同意的要求。
 */
@Composable
private fun PrivacyConsentDialog(
    onAgree: () -> Unit,
    onViewPolicy: () -> Unit,
    onDisagree: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* 必须做出选择，不允许点外部关闭 */ },
        containerColor = org.bp.songbaobao.ui.theme.NavySoft,
        title = {
            Text(
                stringResource(R.string.privacy_dialog_title),
                fontWeight = FontWeight.Bold,
                color = org.bp.songbaobao.ui.theme.GoldBright
            )
        },
        text = {
            Column {
                Text(
                    stringResource(R.string.privacy_dialog_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = org.bp.songbaobao.ui.theme.TextMain
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.privacy_dialog_points),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = org.bp.songbaobao.ui.theme.GoldBright
                )
                listOf(
                    R.string.privacy_point_1,
                    R.string.privacy_point_2,
                    R.string.privacy_point_3,
                    R.string.privacy_point_4
                ).forEach { res ->
                    Text(
                        "· ${stringResource(res)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = org.bp.songbaobao.ui.theme.TextDim,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAgree) {
                Text(
                    stringResource(R.string.privacy_agree),
                    color = org.bp.songbaobao.ui.theme.SuccessGreen
                )
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onViewPolicy) {
                    Text(
                        stringResource(R.string.privacy_view_full),
                        color = org.bp.songbaobao.ui.theme.Gold
                    )
                }
                TextButton(onClick = onDisagree) {
                    Text(
                        stringResource(R.string.privacy_disagree),
                        color = org.bp.songbaobao.ui.theme.DangerRed
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Bp.route
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBar = Screen.barItems.any { it.route == currentRoute }

    Scaffold(
        topBar = {
            if (showBar) {
                val customTitle by UserPrefs.customTitle.collectAsState()
                val ctx = LocalContext.current
                val title = customTitle?.takeIf { it.isNotBlank() } ?: ctx.getString(R.string.app_name)
                TopAppBar(
                    title = {
                        Text(
                            title,
                            color = org.bp.songbaobao.ui.theme.GoldBright,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = org.bp.songbaobao.ui.theme.NavySoft.copy(alpha = 0.92f)
                    )
                )
            }
        },
        bottomBar = {
            if (showBar) {
                AppBottomBar(
                    currentRoute = currentRoute,
                    onSelect = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding)
        ) {
            // ---------- 血压 ----------
            composable(Screen.Bp.route) {
                BpTrendScreen(
                    onAdd = { navController.navigate(Screen.BpAdd.route) },
                    onEdit = { id -> navController.navigate(Screen.bpEdit(id)) },
                    onList = { navController.navigate(Screen.BpList.route) },
                    onScan = { navController.navigate(Screen.BpScan.route) }
                )
            }
            composable(Screen.BpAdd.route) {
                BpAddScreen(onBack = { navController.popBackStack() })
            }
            composable(
                Screen.BpEdit.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) {
                BpEditScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.BpList.route) {
                BpListScreen(
                    onBack = { navController.popBackStack() },
                    onAdd = { navController.navigate(Screen.BpAdd.route) },
                    onEdit = { id -> navController.navigate(Screen.bpEdit(id)) }
                )
            }
            composable(Screen.BpScan.route) {
                BpScanScreen(
                    onBack = { navController.popBackStack() },
                    onConfirm = { navController.navigate(Screen.BpAdd.route) }
                )
            }

            // ---------- 血糖 ----------
            composable(Screen.Glucose.route) {
                GlucoseListScreen(
                    onAdd = { navController.navigate(Screen.GlucoseAdd.route) },
                    onEdit = { id -> navController.navigate(Screen.glucoseEdit(id)) },
                    onList = { navController.navigate(Screen.GlucoseList.route) },
                    onScan = { navController.navigate(Screen.GlucoseScan.route) }
                )
            }
            composable(Screen.GlucoseAdd.route) {
                GlucoseAddScreen(onBack = { navController.popBackStack() })
            }
            composable(
                Screen.GlucoseEdit.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) {
                GlucoseEditScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.GlucoseList.route) {
                GlucoseAllScreen(
                    onBack = { navController.popBackStack() },
                    onAdd = { navController.navigate(Screen.GlucoseAdd.route) },
                    onEdit = { id -> navController.navigate(Screen.glucoseEdit(id)) }
                )
            }
            composable(Screen.GlucoseScan.route) {
                GlucoseScanScreen(
                    onBack = { navController.popBackStack() },
                    onConfirm = { navController.navigate(Screen.GlucoseAdd.route) }
                )
            }

            // ---------- 用药 ----------
            composable(Screen.Med.route) {
                MedScreen(
                    onAdd = { navController.navigate(Screen.MedAdd.route) },
                    onEdit = { id -> navController.navigate(Screen.medEdit(id)) }
                )
            }
            composable(Screen.MedAdd.route) {
                MedAddScreen(onBack = { navController.popBackStack() })
            }
            composable(
                Screen.MedEdit.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) {
                MedEditScreen(onBack = { navController.popBackStack() })
            }

            // ---------- 血常规 ----------
            composable(Screen.Lab.route) {
                LabScreen(
                    onAdd = { navController.navigate(Screen.LabAdd.route) },
                    onEdit = { id -> navController.navigate(Screen.labEdit(id)) },
                    onScan = { navController.navigate(Screen.LabScan.route) }
                )
            }
            composable(Screen.LabAdd.route) {
                LabAddScreen(onBack = { navController.popBackStack() })
            }
            composable(
                Screen.LabEdit.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) {
                LabEditScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.LabScan.route) {
                LabScanScreen(
                    onBack = { navController.popBackStack() },
                    onConfirm = { _, _ ->
                        // OCR 结果已存入 OcrResultHolder，表单页自行读取
                        navController.navigate(Screen.LabAdd.route) {
                            popUpTo(Screen.LabScan.route) { inclusive = true }
                        }
                    }
                )
            }

            // ---------- 笔记本 ----------
            composable(Screen.Notes.route) {
                NoteListScreen(
                    onAdd = { navController.navigate(Screen.NoteAdd.route) },
                    onOpen = { id -> navController.navigate(Screen.noteDetail(id)) }
                )
            }
            composable(Screen.NoteAdd.route) {
                NoteAddScreen(onBack = { navController.popBackStack() })
            }
            composable(
                Screen.NoteEdit.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) {
                NoteEditScreen(onBack = { navController.popBackStack() })
            }
            composable(
                Screen.NoteDetail.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) {
                NoteDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(Screen.noteEdit(id)) }
                )
            }

            // ---------- 会员订阅 ----------
            composable(Screen.Subscription.route) {
                SubscriptionScreen()
            }

            // ---------- 关于 ----------
            composable(Screen.About.route) {
                AboutScreen(
                    onLegal = { navController.navigate(Screen.Legal.route) },
                    onSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            // ---------- 个性化设置 ----------
            composable(Screen.Settings.route) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }

            // ---------- 合规文档 ----------
            composable(Screen.Legal.route) {
                LegalScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
