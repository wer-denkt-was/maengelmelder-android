package de.maengelmelder.mainmodule

import de.maengelmelder.mainmodule.objects.Category
import de.maengelmelder.mainmodule.objects.Domain
import org.junit.Assert
import org.junit.Test

class DomainTest {

    @Test
    fun defaultDomain() {
        MMConstants.DefaultDomainId = 32
        MMConstants.DefaultDomainName = "maengelmelder"
        val domain = Domain.createDefault()

        Assert.assertEquals(
                "Default domain created will always have the assigned domainid from MMConstants",
                "32", domain.id)
        Assert.assertEquals(
                "Default domain created will always have the assigned domain name from MMConstants",
                "maengelmelder", domain.name)
    }

    @Test
    fun jsonSettings() {
        val json = "{\"maxDescriptionLength\": 200, \"warning\": \"too long\"}"
        val domain = Domain.createDefault()
        domain.settingsFromJson(json)

        Assert.assertEquals(
                "Domain correctly loads the JSON settings",
                200, domain.settings["maxDescriptionLength"] as Int)
    }

    @Test
    fun hasCategory() {
        val domain = Domain.createDefault()
        domain.addCategory(Category())

        Assert.assertTrue(
                "Domain has categories",
                domain.hasCategories())
    }

}