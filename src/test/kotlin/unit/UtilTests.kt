package unit

import core.isNotGreaterThan
import core.isNotLessThan
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

public class UtilTests {

    @Test
    fun`when calling split forEach, it prints each item`() {
        "adlsfkjadf".split(",").map { it.trim() }.forEach {
            println(it)
        }
    }

    @Test
    fun `when calling isNotGreaterThan for two different numbers, regardless of format, the result should be correct`() {
        assert(60.0.isNotGreaterThan(61.1)== true)
        assert(.00.isNotGreaterThan(61)== true)
        assert(4.isNotGreaterThan(5)== true)
        assert(61.2.isNotGreaterThan(61.1) ==false)
    }

    @Test
    fun `when calling isNotGreaterThan for two identical numbers, regardless of format, the result should be correct`() {
        assert(60.0.isNotGreaterThan(60)== true)
        assert(.00.isNotGreaterThan(0)== true)
        assert(4.isNotGreaterThan(4.0000)== true)
    }

    @Test
    fun `when calling isNotGreaterThan for one number and one null, regardless of format, the result should be correct`() {
        assert(60.0.isNotGreaterThan(null)== true)
        assert(.00.isNotGreaterThan(null)== true)
        assert(4.isNotGreaterThan(null)== true)
    }

    @Test
    fun `when calling isNotLessThan for two different numbers, regardless of format, the result should be correct`() {
        assert(60.0.isNotLessThan(61.1) ==false)
        assert(.00.isNotLessThan(61) ==false)
        assert(4.isNotLessThan(5) ==false)
        assert(61.2.isNotLessThan(61.1)== true)
    }

    @Test
    fun `when calling isNotLessThan for two identical numbers, regardless of format, the result should be correct`() {
        assert(60.0.isNotLessThan(60)== true)
        assert(.00.isNotLessThan(0)== true)
        assert(4.isNotLessThan(4.0000)== true)
    }

    @Test
    fun `when calling isNotLessThan for one number and one null, regardless of format, the result should be correct`() {
        assert(60.0.isNotLessThan(null)== true)
        assert(.00.isNotLessThan(null)== true)
        assert(4.isNotLessThan(null)== true)
    }

    @Rule @JvmField
    var folder = TemporaryFolder()

    @Test
    fun `when calling file delete it returns true if the file exists`() {
        val files = listOf(File("adsfl.txt"), folder.newFile())
        files.forEach { println(it.delete()) }
    }
}




