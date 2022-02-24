package gradle

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.BuildServiceRegistry
import org.gradle.build.event.BuildEventsListenerRegistry
import org.gradle.tooling.events.FailureResult
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.OperationResult

abstract class BuildEventService : BuildService<BuildServiceParameters.None>, OperationCompletionListener, AutoCloseable {
    class Results : HashMap<FinishEvent, OperationResult>() {
        fun hasNoFailures() = filter { it.value is FailureResult }.isEmpty()
    }

    private val results = Results()
    init { buildStart() }
    override fun close() { buildFinsih(results) }

    override fun onFinish(taskFinishEvent: FinishEvent?) {
        taskFinishEvent?.result?.let { results.put(taskFinishEvent, it) }
    }

    companion object {
        private var buildFinsih : (Results) -> Unit = {}
        private var buildStart : () -> Unit = {}

        fun init(
            buildServiceRegistry: BuildServiceRegistry,
            buildEventsListenerRegistry: BuildEventsListenerRegistry,
            onBuildStart : () -> Unit,
            onBuildFinish : (Results) -> Unit
        ) {
            val provider = buildServiceRegistry.registerIfAbsent("BuildEventService", BuildEventService::class.java) {}
            buildEventsListenerRegistry.onTaskCompletion(provider)
            buildStart = onBuildStart
            buildFinsih = onBuildFinish
        }
    }
}
