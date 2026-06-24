package de.maengelmelder.mainmodule

import de.maengelmelder.mainmodule.objects.Domain
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DomainSettingsTest {

    @Test
    fun testSettings() {
        val settingsJson = "{\"a\":\"1\",\"b\":\"2\",\"c\":\"3\"}"
        val settingsAdd = "{\"b\":\"4\",\"d\":\"5\"}"
        val d = Domain().apply {
            settingsFromJson(settingsJson)
        }
        Assert.assertEquals("It should store the JSON string to a key-value map",
                d.settings["a"], "1")

        val converted = d.settingsToJson()
        Assert.assertEquals("It should return back to the original JSON string",
                settingsJson, converted.toString())

        d.settingsFromJson(settingsAdd)
        Assert.assertEquals("It should override and update old settings",
                d.settings["b"], "4")
    }
}