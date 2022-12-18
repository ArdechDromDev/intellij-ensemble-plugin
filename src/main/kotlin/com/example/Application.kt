package com.example

import com.example.plugins.Session
import com.example.plugins.configureRouting
import com.example.plugins.configureSerialization
import io.ktor.server.engine.*
import io.ktor.server.netty.*

//TODO: runner qui appel intellij

class MySession: Session {
    override fun isConnected() = false
}

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureRouting(MySession())
        configureSerialization()
    }.start(wait = true)
}
