package org.bp.songbaobao.ui.screen.glucose

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

/** 编辑血糖记录：id 由导航参数传入，复用 GlucoseFormScreen */
@Composable
fun GlucoseEditScreen(
    onBack: () -> Unit,
    vm: GlucoseViewModel = hiltViewModel()
) {
    GlucoseFormScreen(recordId = vm.editId, onBack = onBack)
}
