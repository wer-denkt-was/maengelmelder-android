package de.maengelmelder.mainmodule

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1Domain
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1Message
import de.maengelmelder.mainmodule.network.coroutines.v1.MMv1System
import de.maengelmelder.mainmodule.network.responses.BaseResponse
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.nio.charset.Charset

@RunWith(JUnit4::class)
class ReadOnlyApiParseTest {

    private val ctx = InstrumentationRegistry.getInstrumentation().context

    private fun loadSampleFileFromAsset(ctx: Context, path: String): String {
        val inpS = ctx.resources.assets.open(path)
        val content = inpS.readBytes().toString(Charset.defaultCharset())
        inpS.close()
        return content
    }

    @Test
    fun parseSystem() {
        val sampleSystem = loadSampleFileFromAsset(ctx, "sample_system.json")
        val systemApi = MMv1System(ctx, 0.0, 0.0, false)
        val system = systemApi.parseResponse(BaseResponse(200, sampleSystem))

        Assert.assertEquals("It should parse the correct amount of systems",
                system.size, 2)

        val primary = system.find { i -> i.appId == "1" }
        Assert.assertTrue("It should always have at least 1 valid fallback domain",
                primary != null
                        && !primary.isExternal
                        && primary.domainName.contains("api.example.org"))
    }

    @Test
    fun parseDomainV1() {
        val sampleDomainV1 = loadSampleFileFromAsset(ctx, "sample_domain_v1.json")
        val domainV1Api = MMv1Domain(ctx, 0.0, 0.0)
        val domain = domainV1Api.parseResponse(BaseResponse(200, sampleDomainV1))

        Assert.assertTrue("It should always have at least 1 domain", domain.isNotEmpty())

        val first = domain[0]
        Assert.assertTrue("It should always have categories", first.categoriesAsArray().isNotEmpty())


        Assert.assertTrue("It should have the domain name to be displayed",
                first.name != null && first.name?.isNotEmpty() == true)
    }

    @Test
    fun parseMessagesV1() {
        val sampleMessages = loadSampleFileFromAsset(ctx, "sample_messages_v1.json")
        val msgsApiV1 = MMv1Message(ctx, 0.0, 0.0, 0.0, 0.0, 1)
        val result = msgsApiV1.parseResponse(BaseResponse(200, sampleMessages))

        Assert.assertEquals("It should parse the correct number of messages",
                result.size, 4)

        Assert.assertTrue("It should have at least 1 domain object",
                msgsApiV1.domains?.isNotEmpty() == true)

        val first = result[0]

        Assert.assertTrue("A valid message should have all properties needed to generate a marker",
                first.colorString.isNotEmpty()
                        && first.category.markerId.isNotEmpty()
                        && first.lat != Double.MAX_VALUE
                        && first.lon != Double.MAX_VALUE)

        Assert.assertTrue("A valid message should have a valid ID",
                first.serverId.isNotEmpty()
                        && first.id.isNotEmpty()
                        && first.id == first.serverId)

    }
}