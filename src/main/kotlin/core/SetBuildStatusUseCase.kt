package core

class SetBuildStatusUseCase(val datasources: List<BuildStatusDatasource>) {
    fun pending(message: String, key: String) {
        datasources.forEach {
            it.postPendingStatus(message, key).subscribe()
        }
    }

    fun failure(message: String, key: String) {
        datasources.forEach {
            it.postFailureStatus(message, key).subscribe()
        }
    }

    fun success(message: String, key: String) {
        datasources.forEach {
            it.postSuccessStatus(message, key).subscribe()
        }
    }
}