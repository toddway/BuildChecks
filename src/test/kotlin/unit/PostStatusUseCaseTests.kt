package unit
import core.entity.BuildConfig
import core.entity.BuildConfigDefault
import core.entity.BuildStatus
import core.entity.Message
import core.usecase.PostStatusUseCase
import data.ConsoleDatasource
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Observable
import org.junit.Test

class PostStatusUseCaseTests {

    val key = "ADfds"

    @Test
    fun `when post is called, pass parameters to datasource`() {
        val datasource : PostStatusUseCase.Datasource = mockk()
        val usecase = PostStatusUseCase(listOf(datasource), mockk(), mockk())
        val message = "build in progress"
        every { datasource.name() } returns "asdf"
        every { datasource.isActive() } returns true
        every { datasource.isRemote() } returns false
        every { datasource.post(BuildStatus.PENDING, message, key) } returns Observable.just(true)
        usecase.post(BuildStatus.PENDING, message, key)
        verify { datasource.post(BuildStatus.PENDING, message, key) }
    }

    @Test
    fun `when post is not activated, then remove remote datasources`() {
        val source1 = ConsoleDatasource()
        val source2 : PostStatusUseCase.Datasource = mockk()
        every { source2.isRemote() } returns true
        val config : BuildConfig = BuildConfigDefault()
        config.isPostActivated = false
        val messageQueue : MutableList<Message> = mutableListOf()

        val list = PostStatusUseCase(listOf(source1, source2), config, messageQueue).removeInvalid()

        assert(list.contains(source2) == false)
        assert(messageQueue.size == 0)
    }

    @Test
    fun `when post is activated and there are no remote datasources, show a message`() {
        val source1 = ConsoleDatasource()
        val config : BuildConfig = BuildConfigDefault()
        config.isPostActivated = true
        val messageQueue : MutableList<Message> = mutableListOf()

        PostStatusUseCase(listOf(source1), config, messageQueue).removeInvalid()

        assert(messageQueue.size == 1)
    }

    @Test
    fun `when post is activated and there are uncommitted changes, then remove remote datasources and show a message`() {
        val source1 = ConsoleDatasource()
        val source2 : PostStatusUseCase.Datasource = mockk()
        every { source2.isRemote() } returns true
        every { source2.isActive() } returns true
        val config : BuildConfig = BuildConfigDefault()
        config.allowUncommittedChanges = false
        config.isPostActivated = true
        config.git.isAllCommitted = false
        val messageQueue : MutableList<Message> = mutableListOf()

        val list = PostStatusUseCase(listOf(source1, source2), config, messageQueue).removeInvalid()

        assert(list.contains(source2) == false)
        assert(messageQueue.size == 1)
    }

    @Test
    fun `when post is activated and there are all changes are committed, then show a message`() {
        val source1 = ConsoleDatasource()
        val source2 : PostStatusUseCase.Datasource = mockk()
        every { source2.isRemote() } returns true
        every { source2.isActive() } returns true
        val config : BuildConfig = BuildConfigDefault()
        config.isPostActivated = true
        config.git.isAllCommitted = true
        val messageQueue : MutableList<Message> = mutableListOf()

        val list = PostStatusUseCase(listOf(source1, source2), config, messageQueue).removeInvalid()

        assert(list.contains(source2) == true)
        assert(messageQueue.size == 1)
    }
}

