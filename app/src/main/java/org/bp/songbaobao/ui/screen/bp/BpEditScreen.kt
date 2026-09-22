package org.bp.songbaobao.ui.screen.bp

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

/** 编辑血压记录：id 由导航参数传入，复用 BpFormScreen */
@Composable
fun BpEditScreen(
    onBack: () -> Unit,
    vm: BpViewModel = hiltViewModel()
) {
    BpFormScreen(recordId = vm.editId, onBack = onBack)
}
