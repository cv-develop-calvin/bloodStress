package org.bp.songbaobao

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import org.bp.songbaobao.reminder.NotificationHelper
import org.bp.songbaobao.ui.components.AppBottomBar
import org.bp.songbaobao.ui.navigation.Screen
import org.bp.songbaobao.ui.screen.about.AboutScreen
import org.bp.songbaobao.ui.screen.bp.BpAddScreen
import org.bp.songbaobao.ui.screen.bp.BpEditScreen
import org.bp.songbaobao.ui.screen.bp.BpListScreen
import org.bp.songbaobao.ui.screen.bp.BpTrendScreen
import org.bp.songbaobao.ui.screen.lab.LabAddScreen
import org.bp.songbaobao.ui.screen.lab.LabEditScreen
import org.bp.songbaobao.ui.screen.lab.LabScanScreen
import org.bp.songbaobao.ui.screen.lab.LabScreen
import org.bp.songbaobao.ui.screen.med.MedAddScreen
import org.bp.songbaobao.ui.screen.med.MedEditScreen
import org.bp.songbaobao.ui.screen.med.MedScreen
import org.bp.songbaobao.ui.screen.note.NoteAddScreen
import org.bp.songbaobao.ui.screen.note.NoteDetailScreen
import org.bp.songbaobao.ui.screen.note.NoteEditScreen
import org.bp.songbaobao.ui.screen.note.NoteListScreen
import org.bp.songbaobao.ui.theme.SongBaoBaoTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

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
                AppNavHost(startDestination = if (startMed) Screen.Med.route else Screen.Bp.route)
            }
        }
    }
}

@Composable
fun AppNavHost(startDestination: String = Screen.Bp.route) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBar = Screen.barItems.any { it.route == currentRoute }

    Scaffold(
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
                    onList = { navController.navigate(Screen.BpList.route) }
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

            // ---------- 关于 ----------
            composable(Screen.About.route) { AboutScreen() }
        }
    }
}
