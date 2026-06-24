package de.maengelmelder.mainmodule

import de.maengelmelder.mainmodule.objects.Attribute
import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.objects.builder.MessageBuilder
import org.junit.Test

class MessageBuilderTest {

    @Test
    fun newMessage() {
        val m = MessageBuilder()
        assert(!m.isCategoryValid())
        assert(m.message.state == "")
        assert(m.category.name == "")
        assert(m.description == "")
    }

    @Test
    fun attributesCompletion() {
        val m = MessageBuilder()
        m.category = Category().apply {
            typeId = 10L
            domainId = "32"
        }

        val attr1 = Attribute()
        attr1.localId = "104"
        attr1.code = "firstname"
        attr1.required = true
        val attr2 = Attribute()
        attr2.localId = "105"
        attr2.code = "lastname"
        attr2.required = false
        val attr3 = Attribute()
        attr3.localId = "106"
        attr3.code = "email"
        attr3.required = true
        val attr4 = Attribute()
        attr4.localId = "107"
        attr4.code = "phone"
        attr4.required = true

        m.category.addAllAttributes(arrayOf(attr1, attr2, attr3, attr4))

        m.addAttributeValue("104", "John") // Partial completion
        assert(!m.areAttributeValuesFilled())

        m.addAttributeValue("105", null) // putting empty values on required attribute
        m.addAttributeValue("106", "johnsmith@mail.com")
        m.addAttributeValue("107", "")
        assert(!m.areAttributeValuesFilled())

        m.addAttributeValue("107", "123456") // putting values on required attribute, marking completion
        assert(m.areAttributeValuesFilled())

        attr2.required = true // attribute is required but no value present
        assert(!m.areAttributeValuesFilled())
    }

    @Test
    fun imagesList() {
        val mb = MessageBuilder()

        mb.addImagePath("testimages/image01.jpg")
        mb.addImagePath("testimages/image02.jpg")
        mb.addImagePath("testimages/image03.jpg")
        mb.addImagePath("testimages/image04.jpg")

        assert(mb.getNumOfImages() == 4)

        mb.removeImagePath(2)
        mb.removeImagePaths(arrayOf("testimages/image04.jpg", "testimages/image05.jpg"))

        assert(mb.hasImage())
        assert(mb.getImagePath(1) == "testimages/image02.jpg")
    }

    @Test
    fun imagePathsStringify() {
        val mb = MessageBuilder()
        mb.addImagePath("testimages/image01.jpg")
        mb.addImagePath("testimages/image02.jpg")
        mb.addImagePath("testimages/image03.jpg")
        assert(mb.getPhotoPathsAsString() == "testimages/image01.jpg;testimages/image02.jpg;testimages/image03.jpg")
    }

    @Test
    fun locationValidity() {
        val mb = MessageBuilder()
        mb.setLocation(Double.MAX_VALUE,25.454565)
        assert(!mb.isLocationValid())

        mb.setLocation(54.23456, 8.45898)
        assert(mb.isLocationValid())
    }
}