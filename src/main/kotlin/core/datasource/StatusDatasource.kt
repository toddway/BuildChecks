package core.datasource

//fun StatusDatasource.Companion.buildList(
//        config: BuildConfig,
//        messages : MutableList<Message>,
//        remoteDatasource: StatusDatasource?,
//        localDatasource: StatusDatasource) : List<StatusDatasource> {
//    val list = mutableListOf(localDatasource)
//
//    if (config.isPostActivated) {
//        remoteDatasource?.let {
//            val name = it.name().toUpperCase()
//            if (config.allowUncommittedChanges || config.git.isAllCommitted) {
//                list.add(it)
//                messages.add(InfoMessage("Posting to $name"))
//            } else {
//                messages.add(ErrorMessage("You must commit all changes before posting checks to $name"))
//            }
//        } ?: messages.add(ErrorMessage("No recognized post config was found"))
//    }
//
//    return list
//}
