package unit

import core.entity.BuildConfig
import core.usecase.GetSummaryUseCase
import core.usecase.throwIfUnsuccessful
import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.When
import org.amshove.kluent.calling
import org.amshove.kluent.itReturns
import org.amshove.kluent.mock
import org.gradle.api.GradleException
import org.junit.Assert
import org.junit.Test

class HandleUnsuccessfulSummariesUseCaseTests {
    val config = mockk<BuildConfig>(relaxed = true).apply {
        every { isSuccess } returns true
    }

    @Test fun `when handleUnsuccessful is called and a summary is not successful, then throw a Gradle Exception with the summary message`() {
        val summary : GetSummaryUseCase = mock()
        val message = "ASdfd"
        When calling summary.isSuccessful() itReturns false
        When calling summary.value() itReturns message

        val summaries = listOf(summary)

        try {
            summaries.throwIfUnsuccessful(config)
            Assert.assertTrue(false)
        } catch (e : Exception) {
            Assert.assertTrue(e is GradleException)
            Assert.assertEquals(summary.value(), (e as GradleException).message)
        }
    }

    @Test fun `when handleUnsuccessful is called and all summaries are successful, then do nothing`() {
        val summary : GetSummaryUseCase = mock()
        val message = "ASdfd"
        When calling summary.isSuccessful() itReturns true
        When calling summary.value() itReturns message

        val summaries = listOf(summary)

        try {
            summaries.throwIfUnsuccessful(config)
        } catch (e : Exception) {
            Assert.assertFalse(e is GradleException)
            Assert.assertNotEquals(summary.value(), (e as GradleException).message)
        }
    }
}

