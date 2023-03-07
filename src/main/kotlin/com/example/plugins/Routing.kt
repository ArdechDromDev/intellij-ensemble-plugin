package com.example.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

interface TestRunner {
    fun runTests()

}

interface Session {
    fun isConnected(): Boolean
}

fun Application.configureRouting(session: Session, runner: TestRunner? = null) {
    install(StatusPages) {
        exception<AuthenticationException> { call, cause ->
            call.respond(HttpStatusCode.Unauthorized)
        }
        exception<AuthorizationException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden)
        }
    }

    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        post("/trigger") {
            if (session.isConnected()) {
                runner?.runTests()
                call.respond(HttpStatusCode.Created)
            } else {
                call.respond(HttpStatusCode.Conflict, "No test plugin connected")
            }

        }
        // Static plugin. Try to access `/static/index.html`
        static("/static") {
            resources("static")
        }
    }
}
class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()
