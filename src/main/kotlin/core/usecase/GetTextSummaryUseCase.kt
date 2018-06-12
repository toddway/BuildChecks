package core.usecase

import java.io.File

class GetTextSummaryUseCase(val file : File) : GetSummaryUseCase {
    override fun summaryString(): String? {
        return file.readText()
    }

    override fun keyString(): String {
        return file.nameWithoutExtension
    }
}