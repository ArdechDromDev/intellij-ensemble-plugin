package com.example

import org.apache.pulsar.client.api.Message
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.SubscriptionType
import java.util.concurrent.TimeUnit

class TestCommandHandler(pulsarClient: PulsarClient) {
    val consumer =
        pulsarClient
            .newConsumer()
            .subscriptionName("test-sub")
            .topic("commands")
            .subscriptionType(SubscriptionType.Exclusive)
            .subscribe()
    fun receive(): Message<ByteArray>? {
        return consumer.receive(1, TimeUnit.SECONDS)
    }
}
