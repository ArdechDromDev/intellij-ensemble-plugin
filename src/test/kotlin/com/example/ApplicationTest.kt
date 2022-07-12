package com.example

import com.example.plugins.Session
import com.example.plugins.TestRunner
import com.example.plugins.configureRouting
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

data class StubSession(val connected: Boolean = true) : Session {
    override fun isConnected() = connected
}

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        application {
            configureRouting(StubSession())
        }
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Hello World!", bodyAsText())
        }
    }

    @Test
    fun testStaticIndex() = testApplication {
        application {
            configureRouting(StubSession())
        }
        client.get("/static/index.html").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals(
                """<html>
 <head>
 </head>
 <body>
   <h1>Hello Ktor!</h1>
 </body>
</html>""", bodyAsText()
            )
        }
    }

    @Test
    fun testAttendeeTriggerTestsWhenNoPluginConnected() {
        testApplication {
            val runner = testRunner()
            startServer(StubSession(connected = false), runner)
            //TODO: change to restful semantic
            client.post("/trigger").apply {
                assertEquals(HttpStatusCode.Conflict, status)
                assertEquals("No test plugin connected", bodyAsText())
                assertEquals(false, runner.hasBeenRun())
            }
        }
    }

    private fun ApplicationTestBuilder.startServer(session: StubSession, runner: TestRunner) {
        application {
            configureRouting(session, runner)
        }
    }

    @Test
    fun testAttendeeTriggerTests() = testApplication {
        startServer(StubSession(connected = true), testRunner())
        //TODO: change to restful semantic
        client.post("/trigger").apply {
            assertEquals(HttpStatusCode.Created, status)
        }
    }

    @Test
    fun testTriggeringTestsShouldRunThem() = testApplication {
        val runner = testRunner()
        application {
            configureRouting(StubSession(connected = true), runner)
        }
        //TODO: change to restful semantic
        client.post("/trigger").apply {
            assertEquals(true, runner.hasBeenRun())
        }
    }

    private fun testRunner(): TestRunner {
        return object : TestRunner {
            var hasBeenRun = false
            override fun runTests() {
                hasBeenRun = true
            }

            override fun hasBeenRun() = hasBeenRun
        }
    }

}