package org.bp.songbaobao

import org.bp.songbaobao.domain.BpOcrParser
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 血压 OCR 解析核心逻辑验证（不依赖网络/相机）。
 */
class BpOcrParserTest {

    @Test
    fun `三行分别一个读数`() {
        val r = BpOcrParser.parse("15:25\n130\n81\n78")
        assertEquals(130, r.systolic)
        assertEquals(81, r.diastolic)
        assertEquals(78, r.pulse)
    }

    @Test
    fun `一行三个读数`() {
        val r = BpOcrParser.parse("130 81 78")
        assertEquals(130, r.systolic)
        assertEquals(81, r.diastolic)
        assertEquals(78, r.pulse)
    }

    @Test
    fun `标签行加数值行`() {
        val r = BpOcrParser.parse("SYS DIA PUL\n130 81 78")
        assertEquals(130, r.systolic)
        assertEquals(81, r.diastolic)
        assertEquals(78, r.pulse)
    }

    @Test
    fun `标签同行`() {
        val r = BpOcrParser.parse("收缩压 130 舒张压 81 心率 78")
        assertEquals(130, r.systolic)
        assertEquals(81, r.diastolic)
        assertEquals(78, r.pulse)
    }

    @Test
    fun `数码管分段空格 1 3 0`() {
        val r = BpOcrParser.parse("1 3 0\n8 1\n7 8")
        assertEquals(130, r.systolic)
        assertEquals(81, r.diastolic)
        assertEquals(78, r.pulse)
    }

    @Test
    fun `分数式 130-81`() {
        val r = BpOcrParser.parse("130/81")
        assertEquals(130, r.systolic)
        assertEquals(81, r.diastolic)
    }

    @Test
    fun `含日期时间戳`() {
        val r = BpOcrParser.parse("2026-09-26 15:25\n130\n81\n78")
        assertEquals(130, r.systolic)
        assertEquals(81, r.diastolic)
        assertEquals(78, r.pulse)
    }

    @Test
    fun `字母误认 O 与 S 修正`() {
        val r = BpOcrParser.parse("1O0\n8l\n7S")
        assertEquals(100, r.systolic)
        assertEquals(81, r.diastolic)
        assertEquals(75, r.pulse)
    }
}
