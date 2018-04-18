package unit

import org.junit.Test

class UtilTests {

    @Test
    fun`when calling split forEach, it prints each item`() {
        "adlsfkjadf".split(",").map { it.trim() }.forEach {
            println(it)
        }
    }
}



