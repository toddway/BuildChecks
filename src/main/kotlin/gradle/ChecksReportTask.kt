package gradle

import com.jakewharton.picnic.table
import core.run
import data.GithubDatasource
import data.Registry
import data.retrofit
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class ChecksReportTask : DefaultTask() {
    var registry : Registry? = null

    @TaskAction
    fun taskAction() {
        val v = "git --no-pager log --pretty=format:%H".run() ?: "error"
        val a = v.lines()

        ifLet(registry?.config?.baseUrl, registry?.config?.authorization) { (baseUrl, authorization) ->
            val service = retrofit(baseUrl, authorization).create(GithubDatasource.Service::class.java)
            val list : List<Observable<List<GithubDatasource.GithubBuildStatusBody>>> = a.map { service.getBuildStatus(it) }

            Observable.fromIterable(list)
                    .flatMap { it.observeOn(Schedulers.computation()) } //execute in parallel
                    .toList()
                    .subscribe({ show(it) }, {})
        }
    }

    fun show(it : List<List<GithubDatasource.GithubBuildStatusBody>>) {
        table {
            cellStyle {
                border = true
                paddingLeft = 1
                paddingRight = 1
            }

            row("Date", "Coverage", "Lint")

            it.map { item ->
                var cov : String? = null
                var date : String? = null
                var lint : String? = null

                item.firstOrNull { it.context == "test" }?.let {
                    cov = it.description.substring(0, it.description.indexOf("%")) + "%"
                    date = it.created_at
                }

                item.firstOrNull { it.context == "lint" }?.let {
                    lint = it.description.substring(0, it.description.indexOf(" ")) + " violations"
                }

                ifLet(date, cov) { (date, cov) -> row(date, cov, lint ?: "") }
            }
        }.apply { println(this) }
    }
}

inline fun <T: Any> ifLet(vararg elements: T?, closure: (List<T>) -> Unit) {
    if (elements.all { it != null }) {
        closure(elements.filterNotNull())
    }
}
