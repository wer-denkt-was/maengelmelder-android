package de.maengelmelder.mainmodule.objects

class MessageFilterParam {
    var text: String = ""
    var category: String = ""
    var statuses: ArrayList<String> = arrayListOf()
    var favoriteOnly: Boolean = false
}