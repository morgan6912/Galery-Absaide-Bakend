package com.absaide.server.routes

import com.absaide.server.models.*
import com.absaide.server.repositories.MessageRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.messageRoutes() {
    val repo = MessageRepository()

    route("/messages") {
        post {
            val senderId = call.principal<JWTPrincipal>()
                ?.payload?.getClaim("userId")?.asInt()
                ?: return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Nothing>(false, "No autorizado")
                )
            val req = call.receive<MessageRequest>()
            val msg = repo.send(senderId, req.receiverId, req.artworkId, req.content)
            call.respond(HttpStatusCode.OK,
                ApiResponse(true, "Mensaje enviado", msg))
        }

        get("/received") {
            val userId = call.principal<JWTPrincipal>()
                ?.payload?.getClaim("userId")?.asInt()
                ?: return@get call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Nothing>(false, "No autorizado")
                )
            val msgs = repo.getReceived(userId)
            call.respond(HttpStatusCode.OK, msgs)
        }

        get("/sent") {
            val userId = call.principal<JWTPrincipal>()
                ?.payload?.getClaim("userId")?.asInt()
                ?: return@get call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Nothing>(false, "No autorizado")
                )
            val msgs = repo.getSent(userId)
            call.respond(HttpStatusCode.OK, msgs)
        }
    }
}