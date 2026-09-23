package org.bp.songbaobao

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 版本比较与 GitHub Release 解析的核心逻辑验证。
 * 不依赖网络，用固定的 API 响应样本校验解析正确性。
 */
class VersionCompareTest {

    /** 与 VersionRepository.compareVersion 相同的实现，用于回归验证 */
    private fun compareVersion(a: String, b: String): Int {
        val pa = a.split('.', '-').mapNotNull { it.toIntOrNull() }
        val pb = b.split('.', '-').mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val x = pa.getOrElse(i) { 0 }
            val y = pb.getOrElse(i) { 0 }
            if (x != y) return x - y
        }
        return 0
    }

    @Test
    fun newerVersionIsDetected() {
        assertTrue(compareVersion("1.3.0", "1.2.0") > 0)
        assertTrue(compareVersion("2.0.0", "1.9.9") > 0)
        assertTrue(compareVersion("1.2.1", "1.2.0") > 0)
    }

    @Test
    fun sameVersionIsNotAnUpdate() {
        assertEquals(0, compareVersion("1.2.0", "1.2.0"))
        // 位数不同也视为相等
        assertEquals(0, compareVersion("1.2", "1.2.0"))
    }

    @Test
    fun olderVersionIsNotAnUpdate() {
        assertTrue(compareVersion("1.1.0", "1.2.0") < 0)
        assertTrue(compareVersion("0.9.9", "1.0.0") < 0)
    }

    @Test
    fun parseVersionFromApkName() {
        val name = "songbaobao-v1.2.0-build45.apk"
        val version = name
            .substringAfter("songbaobao-v", "")
            .substringBefore("-build")
            .ifBlank { "0.0.0" }
        assertEquals("1.2.0", version)
    }

    @Test
    fun parseBuildNumberFromTag() {
        val tag = "build-45"
        val buildNo = tag.removePrefix("build-").toIntOrNull()
        assertEquals(45, buildNo)
    }

    @Test
    fun rejectNonBuildTag() {
        // CI 的错误日志以 ci-build-log 发布，不应被当成正式版本
        val tag = "ci-build-log"
        assertTrue(!tag.startsWith("build-"))
    }
}
