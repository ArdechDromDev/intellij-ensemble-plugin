package com.example

import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.SubscriptionType
import java.util.concurrent.TimeUnit

object RunTest

class TestCommandHandler(pulsarClient: PulsarClient) {
    val consumer =
        pulsarClient
            .newConsumer()
            .subscriptionName("test-sub")
            .topic("commands")
            .subscriptionType(SubscriptionType.Exclusive)
            .subscribe()
    fun receive(): RunTest? {
        return consumer.receive(1, TimeUnit.SECONDS)?.let { RunTest }
    }
}
