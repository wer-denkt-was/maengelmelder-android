package de.maengelmelder.mainmodule

import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1BmsAppCategory
import de.maengelmelder.mainmodule.objects.SystemInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class BmsAppCategoryUrlTest {

    private val external = SystemInfo().apply {
        appId = "7"
        domainName = "https://sags-doch.de"
        isExternal = true
    }

    @Test
    fun usesDefaultHostWithoutSystem() {
        assertEquals("https://api.example.org/api/v1/bmsapp/1/category/21",
            MMv1BmsAppCategory.buildUrl("https://api.example.org", "1", false, null, "category", 21))
    }

    @Test
    fun usesExternalSystem() {
        assertEquals("https://sags-doch.de/api/v1/bmsapp/7/category/21",
            MMv1BmsAppCategory.buildUrl("https://api.example.org", "1", false, external, "category", 21))
    }

    @Test
    fun overriddenAppIdWinsOverExternalSystem() {
        assertEquals("https://sags-doch.de/api/v1/bmsapp/1/category/21",
            MMv1BmsAppCategory.buildUrl("https://api.example.org", "1", true, external, "category", 21))
    }
}
