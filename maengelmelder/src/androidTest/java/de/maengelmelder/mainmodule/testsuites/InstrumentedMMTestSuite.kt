package de.maengelmelder.mainmodule.testsuites

import de.maengelmelder.mainmodule.DatabaseTest
import de.maengelmelder.mainmodule.DomainSettingsTest
import de.maengelmelder.mainmodule.ReadOnlyApiParseTest
import de.maengelmelder.mainmodule.UserCredentialsTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
        UserCredentialsTest::class,
        DatabaseTest::class,
        ReadOnlyApiParseTest::class,
        DomainSettingsTest::class)
class InstrumentedMMTestSuite