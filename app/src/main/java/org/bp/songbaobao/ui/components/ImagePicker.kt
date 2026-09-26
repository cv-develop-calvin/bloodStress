package org.bp.songbaobao.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * 打开系统「图库/相册」选择一张图片。
 *
 * 相比 PickVisualMedia（Photo Picker），ACTION_PICK 在所有设备上都能稳定拉起相册，
 * 且不会在无 GMS 的环境下退化为「文件选择器」。若系统没有图库应用，则退回 ACTION_GET_CONTENT。
 *
 * 返回一个无参 lambda，调用即拉起选图；选中图片后回调 onImagePicked(uri)。
 */
@Composable
fun rememberGalleryPicker(onImagePicked: (Uri) -> Unit): () -> Unit {
    val context = LocalContext.current
    val callback = remember { mutableStateOf(onImagePicked) }
    callback.value = onImagePicked

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) callback.value(uri)
        }
    }

    return {
        val pickIntent = Intent(Intent.ACTION_PICK).apply {
            setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        }
        val target: Intent = if (pickIntent.resolveActivity(context.packageManager) != null) {
            pickIntent
        } else {
            Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
        }
        launcher.launch(target)
    }
}

/**
 * 多图选择版本（笔记配图使用）。同样优先拉起相册（支持 EXTRA_ALLOW_MULTIPLE），
 * 无图库时退回 ACTION_GET_CONTENT。最多返回 max 张。
 */
@Composable
fun rememberGalleryPickerMultiple(max: Int = 9, onPicked: (List<Uri>) -> Unit): () -> Unit {
    val context = LocalContext.current
    val callback = remember { mutableStateOf(onPicked) }
    callback.value = onPicked

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data ?: return@rememberLauncherForActivityResult
            val uris = mutableListOf<Uri>()
            val clip = data.clipData
            if (clip != null) {
                for (i in 0 until clip.itemCount) {
                    clip.getItemAt(i).uri?.let { uris.add(it) }
                }
            } else {
                data.data?.let { uris.add(it) }
            }
            if (max > 0 && uris.size > max) uris.subList(max, uris.size).clear()
            if (uris.isNotEmpty()) callback.value(uris)
        }
    }

    return {
        val pickIntent = Intent(Intent.ACTION_PICK).apply {
            setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        val target: Intent = if (pickIntent.resolveActivity(context.packageManager) != null) {
            pickIntent
        } else {
            Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
        }
        launcher.launch(target)
    }
}
