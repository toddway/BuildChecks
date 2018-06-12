package core.usecase

class ShowMessageUseCase {
    fun invoke(message : String) {
        println("   |__ $message")
    }
}