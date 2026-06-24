package de.maengelmelder.mainmodule

import androidx.test.platform.app.InstrumentationRegistry
import de.maengelmelder.mainmodule.database.MMDB
import de.maengelmelder.mainmodule.objects.Attribute
import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.objects.Domain
import de.maengelmelder.mainmodule.objects.builder.MessageBuilder
import de.maengelmelder.mainmodule.service.tasks.CategoriesAndAttributesThread
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DatabaseTest {

    private val ctx = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun messageUpdate() {
        val db = MMDB.instance(ctx)
        val newMsg = MessageBuilder()
        newMsg.description = "test message"

        val newCat = Category()
        newCat.name = "Müll"
        newCat.typeId = 122
        newCat.markerId = "25"
        newMsg.category = newCat

        val msgId = newMsg.messageId

        db.addMessageFromServer(newMsg.message)
        db.updateMessage(msgId,
                Pair(db.constants.COL_LAT, "49.00505"),
                Pair(db.constants.COL_LON, "8.56565"),
                Pair(db.constants.COL_PHOTO_PATH, "image001.png"))

        val queried = db.getMessage(msgId)
        Assert.assertEquals(queried?.lat, 49.00505)
        Assert.assertEquals(queried?.imagePaths?.size, 1)
    }

    @Test
    fun relationalCategoryAttributes() {
        val domain = Domain().apply {
            id = "32"
            systemId = "1"
        }
        val cat1 = Category()
        cat1.typeId = 50L
        cat1.name = "Infrastruktur"
        val cat2 = Category()
        cat2.name = "Illegaler Müll"
        cat2.typeId = 51L
        val cat3 = Category()
        cat3.name = "Unfall"
        cat3.typeId = 52L
        domain.addCategories(cat1, cat2, cat3)

        val attr1 = Attribute()
        attr1.id = "25"
        attr1.name = "firstname"
        val attr2 = Attribute()
        attr2.id = "26"
        attr2.name = "lastname"
        val attr3 = Attribute()
        attr3.id = "27"
        attr3.name = "phone"
        cat1.addAllAttributes(arrayOf(attr1, attr2))
        cat2.addAllAttributes(arrayOf(attr1, attr2, attr3))
        cat3.addAllAttributes(arrayOf(attr3))

        val onDone: () -> Unit = {
            val db = MMDB.instance(ctx)
            val cat = db.getCategory(cat1.typeId.toString(), true)
            cat?.let {
                Assert.assertTrue(it.hasAttribute("25"))
                Assert.assertTrue(!it.hasAttribute("27"))
            }

            val attrs = db.getAttributesByCategoryId(cat2.typeId.toString())
            cat?.let {
                Assert.assertEquals(attrs.size, 3)
            }
        }

        CategoriesAndAttributesThread(ctx, listOf(domain), onDone).run()
    }

    @Test
    fun messagesWithExtraJSON() {
        val db = MMDB.instance(ctx)

        val msg = MessageBuilder()
        val msgid = msg.messageId

        val extraJSON = "[ {'id': '345', 'value': 'john'}, {'id': '346', 'value': 'smith'}, {'id': '347', 'value': '234567'}]"

        db.addMessageFromServer(msg.message)
        db.updateMessage(msgid, Pair(db.constants.COL_EXTRASJSON, extraJSON))

        val jsonArr = db.getExtrasJSON(msgid)
        msg.attributeValuesFromJson(jsonArr)

        Assert.assertEquals(msg.attributes["345"], "john")
        Assert.assertEquals(msg.attributes["346"], "smith")
        Assert.assertEquals(msg.attributes["347"], "234567")
    }

    // TODO more tests
}