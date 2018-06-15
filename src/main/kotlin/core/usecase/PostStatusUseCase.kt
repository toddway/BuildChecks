package core.usecase

import core.datasource.StatusDatasource
import core.entity.ErrorMessage
import core.entity.Message
import io.reactivex.Observable


open class PostStatusUseCase(open val datasources: List<StatusDatasource>, val messageQueue: MutableList<Message>) {

    open fun pending(message: String, key: String) {
        datasources.forEach { subscribe(it.postPendingStatus(message, key), it) }
    }

    open fun failure(message: String, key: String) {
        datasources.forEach { subscribe(it.postFailureStatus(message, key), it) }
    }

    open fun success(message: String, key: String) {
        datasources.forEach { subscribe(it.postSuccessStatus(message, key), it) }
    }

    private fun subscribe(o: Observable<Boolean>, source: StatusDatasource) {
        o.subscribe { handle(it, source) }
    }

    private fun handle(isSuccess: Boolean, source: StatusDatasource) {
        if (!isSuccess) {
            val message = "${source.name().toUpperCase()} post failed, add -i for full logs"
            messageQueue.add(ErrorMessage(message))
        }
    }
}
