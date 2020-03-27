package unit

import core.usecase.GetSummaryUseCase
import core.usecase.HandleUnsuccessfulSummariesUseCase
import org.amshove.kluent.When
import org.amshove.kluent.calling
import org.amshove.kluent.itReturns
import org.amshove.kluent.mock
import org.gradle.api.GradleException
import org.junit.Assert
import org.junit.Test

class HandleUnsuccessfulSummariesUseCaseTests {
    @Test fun `when usecase is invoked and a summary is not successful, then throw a Gradle Exception with the summary message`() {
        val summary : GetSummaryUseCase = mock()
        val message = "ASdfd"
        When calling summary.isSuccessful() itReturns false
        When calling summary.value() itReturns message

        val summaries = listOf(summary)

        try {
            HandleUnsuccessfulSummariesUseCase(summaries).invoke()
            Assert.assertTrue(false)
        } catch (e : Exception) {
            Assert.assertTrue(e is GradleException)
            Assert.assertEquals(summary.value(), (e as GradleException).message)
        }
    }

    @Test fun `when usecase is invoked and a summary is successful, then do nothing`() {
        val summary : GetSummaryUseCase = mock()
        val message = "ASdfd"
        When calling summary.isSuccessful() itReturns true
        When calling summary.value() itReturns message

        val summaries = listOf(summary)

        try {
            HandleUnsuccessfulSummariesUseCase(summaries).invoke()
        } catch (e : Exception) {
            Assert.assertFalse(e is GradleException)
            Assert.assertNotEquals(summary.value(), (e as GradleException).message)
        }
    }
}

