package core.entity

open class Message(open val string : String = "") {
    override fun equals(other: Any?): Boolean {
        return other is Message && string == other.string
    }

    override fun hashCode(): Int {
        return string.hashCode()
    }
}

class ErrorMessage(override val string: String) : Message(string) {
    override fun toString(): String {
        return "!  $string"
    }
}
class InfoMessage(override val string: String) : Message(string) {
    override fun toString(): String {
        return "✔ $string"
    }
}

fun List<Message>.printAll() = distinct().forEach { println(it.toString()) }