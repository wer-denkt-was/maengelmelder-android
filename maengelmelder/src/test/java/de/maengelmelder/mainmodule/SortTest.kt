package de.maengelmelder.mainmodule

import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.objects.builder.MessageBuilder
import de.maengelmelder.mainmodule.utils.comparators.SortBy
import org.junit.Test
import java.util.*

class SortTest {

     private fun getSampleMessages(): List<MessageBuilder> {
          val mb01 = MessageBuilder()
          mb01.category = Category()
          mb01.category?.name = "Infrastruktur"
          mb01.message.state = "Ungeprüft"
          val mb02 = MessageBuilder()
          mb02.category = Category()
          mb02.category?.name = "Anregungen & Lob"
          mb02.message.state = "Ungeprüft"
          val mb03 = MessageBuilder()
          mb03.category = Category()
          mb03.category?.name = "Graffiti"
          mb03.message.state = "Erledigt"
          val mb04 = MessageBuilder()
          mb04.category = Category()
          mb04.category?.name = "Fahrrad"
          mb04.message.state = "Verschoben"
          val mb05 = MessageBuilder()
          mb05.category = Category()
          mb05.category?.name = "Illegaler Müll"
          mb05.message.state = "Ungültig"
          return listOf(mb01, mb02, mb03, mb04, mb05)
     }

     @Test
     fun sortMessagesByCategory() {
          val list = getSampleMessages()
          Collections.sort(list, SortBy(SortBy.Param.CATEGORY))

          assert(list[0].category?.name == "Anregungen & Lob")
          assert(list[1].category?.name == "Fahrrad")
          assert(list[2].category?.name == "Graffiti")
          assert(list[3].category?.name == "Illegaler Müll")
          assert(list[4].category?.name == "Infrastruktur")
     }

     @Test
     fun sortMessagesByStatus() {
          val list = getSampleMessages()
          Collections.sort(list, SortBy(SortBy.Param.STATUS))

          assert(list[0].category?.name == "Graffiti")
          assert(list[1].message.state == list[2].message.state)
          assert(list.last().category?.name == "Fahrrad")
     }

}