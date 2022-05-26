package gradle

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.BuildServiceRegistry
import org.gradle.build.event.BuildEventsListenerRegistry
import org.gradle.tooling.events.FailureResult
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.OperationResult

abstract class BuildEventService : BuildService<BuildEventService.Params>, OperationCompletionListener, AutoCloseable {
    interface Params : BuildServiceParameters {
        val idProperty : Property<Int>
    }

    class Results : HashMap<FinishEvent, OperationResult>() {
        fun hasNoFailures() = filter { it.value is FailureResult }.isEmpty()
    }

    private val results = Results()
    private val config = configs.find(this)
    init { config?.buildStart?.invoke() }
    override fun close() { config?.buildFinish?.invoke(results) }

    override fun onFinish(taskFinishEvent: FinishEvent?) {
        taskFinishEvent?.result?.let { results.put(taskFinishEvent, it) }
    }

    companion object {
        private var configs = Configs()
        fun create(
            buildServiceRegistry: BuildServiceRegistry,
            buildEventsListenerRegistry: BuildEventsListenerRegistry,
            onBuildStart : () -> Unit,
            onBuildFinish : (Results) -> Unit
        ) { configs.create(buildServiceRegistry, buildEventsListenerRegistry, onBuildStart, onBuildFinish) }
    }

    private class Configs {
        private val list = mutableListOf<Config>()
        fun find(service : BuildEventService) = list.getOrNull(service.parameters.idProperty.get())
        fun create(
            buildServiceRegistry: BuildServiceRegistry,
            buildEventsListenerRegistry: BuildEventsListenerRegistry,
            onBuildStart : () -> Unit,
            onBuildFinish : (Results) -> Unit
        ) {
            buildServiceRegistry.registerIfAbsent("BuildEventService", BuildEventService::class.java) {
                val nextIndex = list.lastIndex + 1
                list.add(nextIndex, Config(onBuildStart, onBuildFinish))
                it.parameters.idProperty.set(nextIndex)
            }.also {
                buildEventsListenerRegistry.onTaskCompletion(it)
            }
        }
    }

    private data class Config(
        val buildStart : () -> Unit,
        val buildFinish : (Results) -> Unit
    )
}

