package de.maengelmelder.mainmodule

import de.maengelmelder.mainmodule.objects.Attribute
import de.maengelmelder.mainmodule.objects.Category
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

class CategoryTest {

    @Test
    fun extractActualName() {
        val cat = Category()
        cat.name = "Infrastruktur>Sonstiges"
        assert(cat.getActualName() == "Sonstiges")
    }

    @Test
    fun containsEmail() {
        val cat = Category()
        val attr1 = Attribute()
        attr1.code = "firstname"
        val attr2 = Attribute()
        attr2.code = "lastname"
        val attr3 = Attribute()
        attr3.code = "maßnummer"
        cat.addAttribute(attr1)
        cat.addAttribute(attr2)
        cat.addAttribute(attr3)

        assert(!cat.hasEmailField())
    }

    @Test
    fun equal() {
        val obj1 = Any()
        val obj2 = Category()
        obj2.name = "Infrastruktur>Sonstiges"
        obj2.domainId = "110"
        obj2.typeId = 150
        obj2.isPrivate = true
        val obj3 = Category()
        obj3.name = "Sonstiges"
        obj3.domainId = "110"
        obj3.typeId = 150
        obj3.isPrivate = true
        val obj4 = Category()
        obj4.name = "Infrastruktur>Sonstiges"
        obj4.domainId = "110"
        obj4.typeId = 150
        obj4.isPrivate = false
        val obj5 = Category()
        obj5.name = "Infrastruktur>Sonstiges"
        obj5.domainId = "25"
        obj5.typeId = 149
        obj5.isPrivate = true

        assert(obj1 != obj2)
        assert(obj2 == obj4)
        assert(obj3 != obj2)
        assert(obj5 != obj4)
    }

    @Test
    fun attributeIdsForUpdate() {
        // The attribute ids for update are delivered by the server ("attributeids_update")
        // and are not derived from "attributeids" and "attributeids_message".
        val cat = Category.fromJSON(JSONObject("""
            {
              "id": 149,
              "domain": { "id": "25", "title": "Test" },
              "attributeids": [1, 2, 3, 4, 5, 6, 7],
              "attributeids_message": [2, 3, 4, 5],
              "attributeids_update": [3, 0, 6]
            }
        """.trimIndent()))

        Assert.assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), cat.attrIds)
        Assert.assertEquals(listOf(2, 3, 4, 5), cat.attrIdsMessage)
        Assert.assertEquals(
                "attribute ids for update are taken from the server, invalid ids are skipped",
                listOf(3, 6), cat.getAttributeIdsForUpdate())
    }

    @Test
    fun attributeIdsForUpdateEmptyWithoutServerList() {
        val cat = Category().apply {
            attrIds = arrayListOf(1, 2, 3)
            attrIdsMessage = arrayListOf(2)
        }
        Assert.assertTrue(cat.getAttributeIdsForUpdate().isEmpty())
    }

    @Test
    fun checkAttributes() {
        var count = 0
        val sampleAttrs = listOf("firstname", "lastname", "email").map { c ->
            Attribute().apply { code = c; localId = (count++).toString() }
        }
        val cat = Category().apply {
            addAllAttributes(sampleAttrs.toTypedArray())
        }
        Assert.assertTrue("category has an email field", cat.hasEmailField())
        Assert.assertTrue("category has attribute with given ID", cat.hasAttribute("1"))
    }

}