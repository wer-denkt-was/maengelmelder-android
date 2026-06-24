package de.maengelmelder.mainmodule

import de.maengelmelder.mainmodule.objects.SystemInfo
import org.junit.Assert
import org.junit.Test

class SystemInfoTest {

    @Test
    fun generatedId() {
        val sys = SystemInfo().apply {
            appId = "1"
            domainName = "mm"
        }
        Assert.assertTrue("new systeminfo has external=false by default",
                !sys.isExternal)
        Assert.assertEquals("generated SystemInfo id has appId and domainName in it",
            "1-mm", sys.generateId())
    }

}