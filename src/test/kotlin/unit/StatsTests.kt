package unit

import core.entity.Stats
import org.junit.Test

class StatsTests {

    @Test
    fun `when`(){
        val metrics = Stats(
                1.1,
                1,
                1.1,
                1,
                2343434,
                "ADfsdf",
                "asdfdf"
        )

        val metrics2 = Stats(
                1.1,
                1,
                1.1,
                1,
                2343434,
                "ADfsdf",
                "asdfdf"
        )

        assert(metrics == metrics2)
    }
}