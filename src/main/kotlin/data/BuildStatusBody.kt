package data

data class BuildStatusBody(
        val state : String = "",
        val key : String = "",
        val name : String = "",
        var url : String? = null,
        var description : String? = null
)