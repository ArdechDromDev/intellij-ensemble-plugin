package com.example

import com.example.plugins.TestRunner
import org.apache.pulsar.client.api.PulsarClient
import java.util.concurrent.TimeUnit

class PulsarTestRunner(pulsarClient: PulsarClient) : TestRunner {

    val producer = pulsarClient
        .newProducer()
        .topic("commands")
        .sendTimeout(10, TimeUnit.SECONDS)
        .blockIfQueueFull(true)
        .create()

    override fun runTests() {
        producer.sendAsync("run-the-tests".toByteArray())
    }
}
