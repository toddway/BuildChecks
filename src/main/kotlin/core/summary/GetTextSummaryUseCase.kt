package core.summary

import java.io.File

class GetTextSummaryUseCase(val file : File) : GetSummaryUseCase {
    override fun isSuccessful(): Boolean {
        return true
    }

    override fun summary(): String? {
        return file.readText()
    }

    override fun key(): String {
        return file.nameWithoutExtension
    }
}
