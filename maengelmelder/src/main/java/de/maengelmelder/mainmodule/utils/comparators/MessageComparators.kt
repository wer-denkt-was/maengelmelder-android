package de.maengelmelder.mainmodule.utils.comparators

import de.maengelmelder.mainmodule.objects.builder.MessageBuilder

class SortBy(p: Param = Param.DESC): Comparator<MessageBuilder> {

    enum class Param {
        DESC, CATEGORY, STATUS
    }

    private var mParam = p

    var param
        get() = mParam
        set(value) { mParam = value }

    override fun compare(o1: MessageBuilder?, o2: MessageBuilder?): Int =
        when (mParam) {
            Param.CATEGORY -> {
                o1?.category?.name?.compareTo(o2?.category?.name?: "")?: 0
            }
            Param.STATUS -> {
                o1?.message?.state?.compareTo(o2?.message?.state?: "")?: 0
            }
            else -> {
                o1?.description?.compareTo(o2?.description?: "")?: 0
            }
        }
}
