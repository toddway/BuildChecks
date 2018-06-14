package core.usecase

import core.entity.ConfigEntity
import data.BitBucketDatasource
import data.GithubDatasource

class HandleBuildStartedUseCase(
        val postStatusUseCase: PostStatusUseCase,
        val config: ConfigEntity,
        val showMessageUseCase: ShowMessageUseCase
) {
    fun invoke() {
        if (config.isPluginActivated) {
            postStatusUseCase.pending(config.startedMessage(), "g")

            if (config.isPostActivated) {
                val remoteDatasource = postStatusUseCase.datasources
                        .find { it is GithubDatasource || it is BitBucketDatasource }
                val message = remoteDatasource?.let { "${it.name().toUpperCase()} CONFIG IS ACTIVE" }
                        ?: "NO RECOGNIZED POST CONFIG WAS FOUND"
                showMessageUseCase.invoke(message)
            }
        }
    }
}
