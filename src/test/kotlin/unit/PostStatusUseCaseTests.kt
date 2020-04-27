package unit
import core.entity.BuildConfig
import core.entity.BuildConfigDefault
import core.entity.BuildStatus
import core.entity.Message
import core.usecase.PostStatusUseCase
import data.ConsoleDatasource
import io.reactivex.Observable
import org.amshove.kluent.*
import org.junit.Test

class PostStatusUseCaseTests {

    val key = "ADfds"

    @Test
    fun `when post is called, pass parameters to datasource`() {
        val datasource : PostStatusUseCase.Datasource = mock()
        val usecase = PostStatusUseCase(listOf(datasource), mock(), mock())
        val message = "build in progress"
        When calling datasource.name() itReturns "asdf"
        When calling datasource.isActive() itReturns true
        When calling datasource.isRemote() itReturns false
        When calling datasource.post(BuildStatus.PENDING, message, key) itReturns Observable.just(true)
        usecase.post(BuildStatus.PENDING, message, key)
        Verify on datasource that datasource.post(BuildStatus.PENDING, message, key) was called
    }

    @Test
    fun `when post is not activated, then remove remote datasources`() {
        val source1 = ConsoleDatasource()
        val source2 : PostStatusUseCase.Datasource = mock()
        When calling source2.isRemote() itReturns true
        val config : BuildConfig = BuildConfigDefault()
        config.isPostActivated = false
        val messageQueue : MutableList<Message> = mutableListOf()

        val list = PostStatusUseCase(listOf(source1, source2), config, messageQueue).removeInvalid()

        list.contains(source2) shouldBe false
        messageQueue.size shouldBe 0
    }

    @Test
    fun `when post is activated and there are no remote datasources, show a message`() {
        val source1 = ConsoleDatasource()
        val config : BuildConfig = BuildConfigDefault()
        config.isPostActivated = true
        val messageQueue : MutableList<Message> = mutableListOf()

        PostStatusUseCase(listOf(source1), config, messageQueue).removeInvalid()

        messageQueue.size shouldBe 1
    }

    @Test
    fun `when post is activated and there are uncommitted changes, then remove remote datasources and show a message`() {
        val source1 = ConsoleDatasource()
        val source2 : PostStatusUseCase.Datasource = mock()
        When calling source2.isRemote() itReturns true
        When calling source2.isActive() itReturns true
        val config : BuildConfig = BuildConfigDefault()
        config.allowUncommittedChanges = false
        config.isPostActivated = true
        config.git.isAllCommitted = false
        val messageQueue : MutableList<Message> = mutableListOf()

        val list = PostStatusUseCase(listOf(source1, source2), config, messageQueue).removeInvalid()

        list.contains(source2) shouldBe false
        messageQueue.size shouldBe 1
    }

    @Test
    fun `when post is activated and there are all changes are committed, then show a message`() {
        val source1 = ConsoleDatasource()
        val source2 : PostStatusUseCase.Datasource = mock()
        When calling source2.isRemote() itReturns true
        When calling source2.isActive() itReturns true
        val config : BuildConfig = BuildConfigDefault()
        config.isPostActivated = true
        config.git.isAllCommitted = true
        val messageQueue : MutableList<Message> = mutableListOf()

        val list = PostStatusUseCase(listOf(source1, source2), config, messageQueue).removeInvalid()

        list.contains(source2) shouldBe true
        messageQueue.size shouldBe 1
    }
}

