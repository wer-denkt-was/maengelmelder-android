package de.maengelmelder.mainmodule

import de.maengelmelder.mainmodule.objects.MessageDetail
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

class MessageDetailTest {

    private val json = "{\n" +
            "        \"address\": \"Musterweg 10, 99999 Musterstadt, Germany\",\n" +
            "        \"allow_comment\": true,\n" +
            "        \"location\": [\n" +
            "            0.0,\n" +
            "            0.0\n" +
            "        ],\n" +
            "        \"state\": \"solved\",\n" +
            "        \"number\": 9100001,\n" +
            "        \"links\": {\n" +
            "            \"message_type\": {\n" +
            "                \"href\": \"/api/v1/domain/2/type/15\"\n" +
            "            },\n" +
            "            \"responsible\": {\n" +
            "                \"href\": \"/api/v1/domain/2/user/9000001\"\n" +
            "            },\n" +
            "            \"domain\": {\n" +
            "                \"href\": \"/api/v1/domain/2\"\n" +
            "            },\n" +
            "            \"self\": {\n" +
            "                \"href\": \"/api/v1/domain/2/message/9100001\"\n" +
            "            }\n" +
            "        },\n" +
            "        \"attribute_values\": [],\n" +
            "        \"message_type\": {\n" +
            "            \"ordering\": 1,\n" +
            "            \"name\": \"Radverkehr > Defekte Verkehrsschilder und Wegweiser\",\n" +
            "            \"id\": 15,\n" +
            "            \"description\": \"<p>Hier können Sie beschädigte, verschmutzte oder verschwundene Verkehrsschilder und Wegweiser melden. Bitte schicken Sie uns ein Foto von dem Schaden. Dann können wir das betroffene Schild schneller finden und den Schaden beheben.</p>\\r\\n\\r\\n<p>Dieses Problem wird vom Mängelmelder <b>nicht</b> bearbeitet:</p>\\r\\n<ul>\\r\\n<li>Änderungen an der Verkehrsführung</li>\\r\\n<li>Änderungen an der Beschilderung</li>\\r\\n</ul>\\r\\n\"\n" +
            "        },\n" +
            "        \"text\": \"vandalismus an der beschilderung\",\n" +
            "        \"id\": 9100001,\n" +
            "        \"created\": \"2022-05-10T16:37:25\",\n" +
            "        \"state_german\": \"Gelöst\",\n" +
            "        \"marker_id\": 204,\n" +
            "        \"updates\": [],\n" +
            "        \"lon\": 0.0,\n" +
            "        \"attachments\": [\n" +
            "            {\n" +
            "                \"url\": \"https://example-bucket.s3-de.example.com/bmspicture/z/q/exampleImageId987654321098765432.jpg\",\n" +
            "                \"id\": 9200001,\n" +
            "                \"thumbnails\": {\n" +
            "                    \"res800\": \"https://imbo.example.com/users/example-env/images/exampleImageId123456789012345678.jpg?t%5B%5D=strip&t%5B%5D=resize%3Awidth%3D800%2Cheight%3D800&publicKey=fakeKeyDoNotUse00000000000000AA&accessToken=0000000000000000000000000000000000000000000000000000000000aa01\",\n" +
            "                    \"sq256\": \"https://imbo.example.com/users/example-env/images/exampleImageId123456789012345678.jpg?t%5B%5D=strip&t%5B%5D=thumbnail%3Awidth%3D256%2Cheight%3D256&publicKey=fakeKeyDoNotUse00000000000000AA&accessToken=0000000000000000000000000000000000000000000000000000000000aa02\",\n" +
            "                    \"w256\": \"https://imbo.example.com/users/example-env/images/exampleImageId123456789012345678.jpg?t%5B%5D=strip&t%5B%5D=resize%3Awidth%3D256&publicKey=fakeKeyDoNotUse00000000000000AA&accessToken=0000000000000000000000000000000000000000000000000000000000aa03\",\n" +
            "                    \"sq128\": \"https://imbo.example.com/users/example-env/images/exampleImageId123456789012345678.jpg?t%5B%5D=strip&t%5B%5D=thumbnail%3Awidth%3D128%2Cheight%3D128&publicKey=fakeKeyDoNotUse00000000000000AA&accessToken=0000000000000000000000000000000000000000000000000000000000aa04\",\n" +
            "                    \"res400\": \"https://imbo.example.com/users/example-env/images/exampleImageId123456789012345678.jpg?t%5B%5D=strip&t%5B%5D=resize%3Awidth%3D400%2Cheight%3D400&publicKey=fakeKeyDoNotUse00000000000000AA&accessToken=0000000000000000000000000000000000000000000000000000000000aa05\"\n" +
            "                },\n" +
            "                \"content_type\": \"image/jpeg\",\n" +
            "                \"type\": \"file\",\n" +
            "                \"public\": 1\n" +
            "            }\n" +
            "        ],\n" +
            "        \"marker_uri\": \"https://bms.example-city.de/static/r/646/static/img/marker/classic/marker-solved-204.png\",\n" +
            "        \"responsible\": {\n" +
            "            \"id\": 9000001,\n" +
            "            \"avatar_uri\": \"https://bms.example-city.de/static/r/646/static/img/avatars/default_avatar_4-256.png\",\n" +
            "            \"public_name\": \"Musteramt - Beispielverwaltung\",\n" +
            "            \"links\": {\n" +
            "                \"self\": {\n" +
            "                    \"href\": \"/api/v1/domain/2/user/9000001\"\n" +
            "                }\n" +
            "            }\n" +
            "        },\n" +
            "        \"domain\": {\n" +
            "            \"title\": \"Bürgerbeteiligung Musterstadt\",\n" +
            "            \"links\": {\n" +
            "                \"self\": {\n" +
            "                    \"href\": \"/api/v1/domain/2\"\n" +
            "                }\n" +
            "            },\n" +
            "            \"name\": \"bms.example-city.de\",\n" +
            "            \"id\": 2\n" +
            "        },\n" +
            "        \"marker_color\": \"green\",\n" +
            "        \"lat\": 0.0,\n" +
            "        \"title\": null,\n" +
            "        \"history\": [\n" +
            "            {\n" +
            "                \"text\": \"Die Meldung wurde erfolgreich bearbeitet.\",\n" +
            "                \"id\": 9300001,\n" +
            "                \"created\": \"2022-05-19T12:30:11\",\n" +
            "                \"manual_text\": \"Das Anliegen wurde seitens eines Nutzers als erledigt gemeldet. 12.05.22\",\n" +
            "                \"owner\": {\n" +
            "                    \"id\": 9000002,\n" +
            "                    \"avatar_uri\": \"https://bms.example-city.de/static/r/646/static/img/avatars/default_avatar_6-256.png\",\n" +
            "                    \"public_name\": \"Musteramt - Beispielverwaltung\",\n" +
            "                    \"links\": {\n" +
            "                        \"self\": {\n" +
            "                            \"href\": \"/api/v1/domain/2/user/9000002\"\n" +
            "                        }\n" +
            "                    }\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"owner\": {\n" +
            "                    \"avatar_uri\": \"https://bms.example-city.de/static/r/646/static/img/avatars/default_avatar_6-256.png\",\n" +
            "                    \"id\": 9000002,\n" +
            "                    \"public_name\": \"Musteramt - Beispielverwaltung\",\n" +
            "                    \"links\": {\n" +
            "                        \"self\": {\n" +
            "                            \"href\": \"/api/v1/domain/2/user/9000002\"\n" +
            "                        }\n" +
            "                    }\n" +
            "                },\n" +
            "                \"text\": \"Es wurde mit der Bearbeitung begonnen.\",\n" +
            "                \"id\": 9300002,\n" +
            "                \"created\": \"2022-05-11T07:30:38\",\n" +
            "                \"manual_text\": \"\"\n" +
            "            },\n" +
            "            {\n" +
            "                \"text\": \"Die Meldung wurde freigegeben und zur Bearbeitung an \\\"Musteramt - Beispielverwaltung\\\" weitergeleitet.\",\n" +
            "                \"created\": \"2022-05-11T05:50:03\",\n" +
            "                \"manual_text\": \"\",\n" +
            "                \"id\": 9300003,\n" +
            "                \"owner\": {\n" +
            "                    \"links\": {\n" +
            "                        \"self\": {\n" +
            "                            \"href\": \"/api/v1/domain/2/user/9000003\"\n" +
            "                        }\n" +
            "                    },\n" +
            "                    \"public_name\": \"Administration\",\n" +
            "                    \"id\": 9000003,\n" +
            "                    \"avatar_uri\": \"https://bms.example-city.de/static/r/646/static/img/avatars/default_avatar_1-256.png\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": 9300004,\n" +
            "                \"text\": \"Die Meldung wurde zur Freigabe an \\\"Moderation\\\" weitergeleitet.\",\n" +
            "                \"created\": \"2022-05-10T16:37:25\",\n" +
            "                \"manual_text\": \"\",\n" +
            "                \"owner\": {\n" +
            "                    \"id\": 9000004,\n" +
            "                    \"avatar_uri\": \"https://bms.example-city.de/static/r/646/static/img/avatar_anon.png\",\n" +
            "                    \"links\": {\n" +
            "                        \"self\": {\n" +
            "                            \"href\": \"/api/v1/domain/2/user/9000004\"\n" +
            "                        }\n" +
            "                    },\n" +
            "                    \"public_name\": \"unbekannter Teilnehmer\"\n" +
            "                }\n" +
            "            }\n" +
            "        ],\n" +
            "        \"thumbnail_sq64\": \"https://imbo.example.com/users/example-env/images/exampleImageId123456789012345678.jpg?t%5B%5D=strip&t%5B%5D=thumbnail%3Awidth%3D64%2Cheight%3D64&publicKey=fakeKeyDoNotUse00000000000000AA&accessToken=0000000000000000000000000000000000000000000000000000000000aa06\"\n" +
            "    }"

    @Test
    fun parse() {
        val md = MessageDetail.fromJSON(JSONObject(json))
        Assert.assertEquals("marker color id is correctly parsed",
                "green", md.colorString)
        Assert.assertEquals("latitude is correctly parsed",
                0.0, md.lat, 1.0)
        Assert.assertEquals("longitude is correctly parsed",
                0.0, md.lon, 1.0)
        Assert.assertTrue("allow_comment is correctly parsed",
                md.allowComment)
        Assert.assertEquals("attachments are correctly parsed",
                1, md.images.size)
        Assert.assertEquals("history is correctly parsed",
                4, md.details.size)
    }

}