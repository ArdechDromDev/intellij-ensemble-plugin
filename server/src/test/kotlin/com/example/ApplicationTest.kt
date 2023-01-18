package com.example

import com.example.plugins.Session
import com.example.plugins.TestRunner
import com.example.plugins.configureRouting
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.apache.pulsar.client.api.Message
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.SubscriptionType
import org.junit.Assert
import org.testcontainers.containers.PulsarContainer
import org.testcontainers.utility.DockerImageName
import java.util.concurrent.TimeUnit
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

class ApplicationIntegrationTest {

    var pulsar: PulsarContainer? = null

    @BeforeTest
    fun before() {
        val pulsar = PulsarContainer(DockerImageName.parse("apachepulsar/pulsar:latest"))
        pulsar.start()


        this.pulsar = pulsar
    }

    class PulsarTestRunner(pulsarClient: PulsarClient) : TestRunner {

        val producer = pulsarClient
            .newProducer()
            .topic("commands")
            .sendTimeout(10, TimeUnit.SECONDS)
            .blockIfQueueFull(true)
            .create();

        override fun runTests() {
            producer.sendAsync("run-the-tests".toByteArray())
        }

        override fun hasBeenRun(): Boolean = TODO()

    }

    @Test
    fun testTriggerShouldProduceACommand() = testApplication {
        val pulsarClient = PulsarClient.builder()
            .serviceUrl(pulsar?.pulsarBrokerUrl)
            .build()

        val consumer =
            pulsarClient
                .newConsumer()
                .subscriptionName("test-sub")
                .topic("commands")
                .subscriptionType(SubscriptionType.Exclusive)
                .subscribe()

        val pulsarTestRunner: TestRunner = PulsarTestRunner(pulsarClient)



        application {
            configureRouting(StubSession(), runner = pulsarTestRunner)
        }

        client.post("/trigger").apply {
            assertEquals(HttpStatusCode.Created, status)
            val message: Message<ByteArray>? = consumer.receive(1, TimeUnit.SECONDS)
            Assert.assertEquals(message?.let { String(it.data) },  "run-the-tests")
        }
    }
}