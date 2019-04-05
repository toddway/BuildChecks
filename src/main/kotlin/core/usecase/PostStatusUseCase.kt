package core.usecase

import core.entity.*
import io.reactivex.Observable


open class PostStatusUseCase(
        val all : List<PostStatusUseCase.Datasource>,
        val config: BuildConfig,
        val messages: MutableList<Message>) {
    val sources by lazy { filterInvalidSources() }

    open fun post(status : BuildStatus, message: String, key: String) {
        sources.forEach { source ->
            source.post(status, message, key).subscribe { isSuccess ->
                if (!isSuccess) {
                    val error = "${source.name().toUpperCase()} post failed, add -i for full logs"
                    messages.add(ErrorMessage(error))
                }
            }
        }
    }

    fun filterInvalidSources() : List<PostStatusUseCase.Datasource> {
        return if (config.isPostActivated) {
            val remote : PostStatusUseCase.Datasource? = all.firstOrNull { it.isRemote() }
            if (remote == null) {
                messages.add(ErrorMessage("No recognized post config was found"))
            } else if (!config.isAllCommitted()) {
                messages.add(ErrorMessage("You must commit all changes before posting checks to ${remote.name()}"))
                return all.filter { !it.isRemote() }
            } else {
                messages.add(InfoMessage("Posting to ${remote.name()}"))
            }
            all
        } else {
            all.filter { !it.isRemote() }
        }
    }

    interface Datasource {
        fun post(status : BuildStatus, message: String, key: String) : Observable<Boolean>
        fun name() : String
        fun isActive() : Boolean
        fun isRemote() : Boolean
    }
}
