package com.absaide.server

import com.absaide.server.database.DatabaseFactory
import com.absaide.server.routes.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.calllogging.* // Corregido: doble 'g'
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.* // Necesario para respond y respondFile
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import java.io.File

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    val jwtSecret   = environment.config.property("jwt.secret").getString()
    val jwtIssuer   = environment.config.property("jwt.issuer").getString()
    val jwtAudience = environment.config.property("jwt.audience").getString()
    val jwtRealm    = environment.config.property("jwt.realm").getString()

    DatabaseFactory.init(environment.config)

    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; isLenient = true; prettyPrint = true })
    }

    install(CallLogging) {
        level = Level.INFO
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Put)
    }

    install(Authentication) {
        jwt("jwt-auth") {
            realm = jwtRealm
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtIssuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("userId").asInt() != null)
                    JWTPrincipal(credential.payload) else null
            }
        }
    }

    routing {
        authRoutes(jwtSecret, jwtIssuer, jwtAudience)

        get("/uploads/{filename}") {
            val filename = call.parameters["filename"] ?: return@get
            val file = File("uploads/$filename")
            if (file.exists()) {
                call.respondFile(file)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        authenticate("jwt-auth") {
            artworkRoutes()
            userRoutes()
            favoriteRoutes()
            uploadRoutes()
        }
    }
}