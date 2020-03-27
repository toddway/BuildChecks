package core.usecase

import core.entity.*
import io.reactivex.Observable


open class PostStatusUseCase(
        val sources : List<Datasource>,
        val config: BuildConfig,
        private val messages: MutableList<Message>) {
    private val validSources by lazy { removeInvalid() }

    open fun post(status : BuildStatus, message: String, key: String) {
        validSources.forEach { source ->
            source.post(status, message, key).subscribe { isSuccess ->
                if (!isSuccess) {
                    val error = "${source.name().toUpperCase()} post failed, add -i for full logs"
                    messages.add(ErrorMessage(error))
                }
            }
        }
    }

    fun removeInvalid() : List<Datasource> {
        var list = sources.filter { it.isActive() }
        if (config.isPostActivated) {
            val remote = list.firstOrNull { it.isRemote() }
            if (remote == null) {
                messages.add(ErrorMessage("No recognized post config was found"))
            } else if (!config.isAllCommitted()) {
                messages.add(ErrorMessage("You must commit all changes before posting checks to ${remote.name()}"))
                list = list.filter { !it.isRemote() }
            } else {
                messages.add(InfoMessage("Posting to ${remote.name()}"))
            }
        } else {
            list = list.filter { !it.isRemote() }
        }
        return list
    }

    interface Datasource {
        fun post(status : BuildStatus, message: String, key: String) : Observable<Boolean>
        fun name() : String
        fun isActive() : Boolean
        fun isRemote() : Boolean
        companion object
    }
}
