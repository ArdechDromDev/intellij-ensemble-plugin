package com.example

import com.example.plugins.Session
import com.example.plugins.TestRunner
import com.example.plugins.configureRouting
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.apache.pulsar.client.api.PulsarClient
import org.junit.Assert
import org.testcontainers.containers.PulsarContainer
import org.testcontainers.utility.DockerImageName
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
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
            assertContains(bodyAsText(), """<body>""")
        }
    }

    @Test
    fun testAttendeeTriggerTestsWhenNoPluginConnected() {
        testApplication {
            val runner = TestRunnerStub()
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
        startServer(StubSession(connected = true), TestRunnerStub())
        //TODO: change to restful semantic
        client.post("/trigger").apply {
            assertEquals(HttpStatusCode.Created, status)
        }
    }

    @Test
    fun testTriggeringTestsShouldRunT0hem() = testApplication {
        val runner = TestRunnerStub()
        application {
            configureRouting(StubSession(connected = true), runner)
        }
        //TODO: change to restful semantic
        client.post("/trigger").apply {
            assertEquals(true, runner.hasBeenRun())
        }
    }
}

class TestRunnerStub : TestRunner {
    var hasBeenRun = false
    override fun runTests() {
        hasBeenRun = true
    }

    fun hasBeenRun() = hasBeenRun
}

class ApplicationIntegrationTest {

    var pulsar: PulsarContainer? = null

    @BeforeTest
    fun before() {
        val pulsar = PulsarContainer(DockerImageName.parse("apachepulsar/pulsar:latest"))
        pulsar.start()


        this.pulsar = pulsar
    }

    @Test
    fun testTriggerShouldProduceACommand() = testApplication {
        val pulsarClient = PulsarClient.builder()
            .serviceUrl(pulsar?.pulsarBrokerUrl)
            .build()



        val pulsarTestRunner: TestRunner = PulsarTestRunner(pulsarClient)
        val testCommandHandler = TestCommandHandler(pulsarClient)
        application {
            configureRouting(StubSession(), runner = pulsarTestRunner)
        }

        client.post("/trigger").apply {
            assertEquals(HttpStatusCode.Created, status)
            val message: RunTest? = testCommandHandler.receive()
            Assert.assertEquals(message, RunTest)
        }
    }

    @Test
    fun testShouldTimeOutOnReceive() = testApplication {
        val pulsarClient = PulsarClient.builder()
            .serviceUrl(pulsar?.pulsarBrokerUrl)
            .build()

        val pulsarTestRunner: TestRunner = PulsarTestRunner(pulsarClient)
        val testCommandHandler = TestCommandHandler(pulsarClient)
        application {
            configureRouting(StubSession(), runner = pulsarTestRunner)
        }

        val message: RunTest? = testCommandHandler.receive()
        Assert.assertEquals(message, null)
    }
}
