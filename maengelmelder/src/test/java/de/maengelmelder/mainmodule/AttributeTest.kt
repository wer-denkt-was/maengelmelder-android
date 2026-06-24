package de.maengelmelder.mainmodule

import de.maengelmelder.mainmodule.objects.Attribute
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

class AttributeTest {

    @Test
    fun jsonSettings() {
        val json = "[{\"id\": \"1\", \"value\": \"test\"}, {\"id\": \"2\", \"value\": \"test2\"}]"
        val attr = Attribute().apply {
            choicesFromJson(json)
        }
        Assert.assertEquals("JSON array correctly loaded to Attribute's map",
            2, attr.choices.size)
    }

    @Test
    fun generatedId() {
        val attr = Attribute().apply {
            systemId = "1"
            domainId = "32"
            localId = "1456"
        }
        Assert.assertEquals("generated ID for attribute has systemid, domainid, and attributeid in it",
                "1-32-1456", attr.generateId())
    }

    @Test
    fun fromJSON() {
        val json = "{\n" +
                "                        \"cached\": 1,\n" +
                "                        \"required\": 0,\n" +
                "                        \"ordering\": 3,\n" +
                "                        \"type\": \"text\",\n" +
                "                        \"help\": \"Die Angabe Ihres Nachnamens ist freiwillig.\",\n" +
                "                        \"required_admin\": 0,\n" +
                "                        \"group\": \"Kontaktangaben\",\n" +
                "                        \"visible_if_value\": null,\n" +
                "                        \"values\": [],\n" +
                "                        \"links\": {\n" +
                "                            \"self\": {\n" +
                "                                \"href\": \"/api/v1/domain/32/attribute/19\"\n" +
                "                            }\n" +
                "                        },\n" +
                "                        \"visible_if_code\": null,\n" +
                "                        \"required_if_value\": null,\n" +
                "                        \"public\": 0,\n" +
                "                        \"visible_to\": \"both\",\n" +
                "                        \"code\": \"last_name\",\n" +
                "                        \"error\": \"Fehlermeldung\",\n" +
                "                        \"default_value\": \"\",\n" +
                "                        \"required_for_user\": false,\n" +
                "                        \"multiselect\": 0,\n" +
                "                        \"required_if_code\": null,\n" +
                "                        \"regex\": null,\n" +
                "                        \"relation_data\": {},\n" +
                "                        \"name\": \"Nachname\",\n" +
                "                        \"id\": 19\n" +
                "                    }"
        val attr = Attribute.fromJSON(JSONObject(json), 32)
        Assert.assertEquals("attribute's local id is correctly parsed",
            "19", attr.localId)
        Assert.assertEquals("attribute's code is correctly parsed",
                "last_name", attr.code)
        Assert.assertEquals("attribute's type is correctly parsed",
                "text", attr.type)
        Assert.assertEquals("attribute's ordering is correctly parsed",
                3, attr.ordering)
        Assert.assertEquals("attribute's name is correctly parsed",
                "Nachname", attr.name)
        Assert.assertTrue("attribute's required (boolean) is correctly parsed",
                !attr.required)
        Assert.assertTrue("attribute's cached (boolean) is correctly parsed",
                attr.shouldCache)
    }

}