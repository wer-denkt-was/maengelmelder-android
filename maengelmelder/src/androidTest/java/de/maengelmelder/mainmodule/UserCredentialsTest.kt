package de.maengelmelder.mainmodule

import androidx.test.platform.app.InstrumentationRegistry
import de.maengelmelder.mainmodule.objects.UserCred
import de.maengelmelder.mainmodule.utils.UserData
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class UserCredentialsTest {

    private val ctx = InstrumentationRegistry.getInstrumentation().context;

    @Test
    fun quickLoginWithFakeData() {
        UserData.login(ctx, "test", "test") {
            Assert.assertEquals("Wrong credential should return null response",
                    it, null)
            val cred = UserData.getUserCred(ctx)
            Assert.assertTrue("Wrong login attempt should yield no token",
                    cred == null || !cred.isUserValid())
        }
    }

    @Test
    fun logout() {
        val uc = UserCred().apply { token = "orbgflaiwbg" }
        UserData.saveUserCred(ctx, uc)

        UserData.logout(ctx) {
            Assert.assertEquals("logging out with wrong token should return null response",
                    it, null)
        }
    }

    @Test
    fun basicAuthForTestServer() {
        Assert.assertEquals("Test Server requires Basic Auth. with credential wdw-up2date",
                UserData.getForBasicAuth(ctx, true), "Basic d2R3OnVwMmRhdGU=")
    }

    @Test
    fun userDataRemoval() {
        UserData.removeUserCred(ctx)
        Assert.assertEquals("Token should not be saved upon removal",
                UserData.getUserCred(ctx)?.token?: "", "")
    }

}