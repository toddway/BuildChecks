package unit

import core.isNotGreaterThan
import core.isNotLessThan
import org.amshove.kluent.shouldBe
import org.junit.Test

class UtilTests {

    @Test
    fun`when calling split forEach, it prints each item`() {
        "adlsfkjadf".split(",").map { it.trim() }.forEach {
            println(it)
        }
    }

    @Test
    fun `when calling isNotGreaterThan for two different numbers, regardless of format, the result should be correct`() {
        60.0.isNotGreaterThan(61.1) shouldBe true
        .00.isNotGreaterThan(61) shouldBe true
        4.isNotGreaterThan(5) shouldBe true
        61.2.isNotGreaterThan(61.1) shouldBe false
    }

    @Test
    fun `when calling isNotGreaterThan for two identical numbers, regardless of format, the result should be correct`() {
        60.0.isNotGreaterThan(60) shouldBe true
        .00.isNotGreaterThan(0) shouldBe true
        4.isNotGreaterThan(4.0000) shouldBe true
    }

    @Test
    fun `when calling isNotGreaterThan for one number and one null, regardless of format, the result should be correct`() {
        60.0.isNotGreaterThan(null) shouldBe true
        .00.isNotGreaterThan(null) shouldBe true
        4.isNotGreaterThan(null) shouldBe true
    }

    @Test
    fun `when calling isNotLessThan for two different numbers, regardless of format, the result should be correct`() {
        60.0.isNotLessThan(61.1) shouldBe false
        .00.isNotLessThan(61) shouldBe false
        4.isNotLessThan(5) shouldBe false
        61.2.isNotLessThan(61.1) shouldBe true
    }

    @Test
    fun `when calling isNotLessThan for two identical numbers, regardless of format, the result should be correct`() {
        60.0.isNotLessThan(60) shouldBe true
        .00.isNotLessThan(0) shouldBe true
        4.isNotLessThan(4.0000) shouldBe true
    }

    @Test
    fun `when calling isNotLessThan for one number and one null, regardless of format, the result should be correct`() {
        60.0.isNotLessThan(null) shouldBe true
        .00.isNotLessThan(null) shouldBe true
        4.isNotLessThan(null) shouldBe true
    }
}



