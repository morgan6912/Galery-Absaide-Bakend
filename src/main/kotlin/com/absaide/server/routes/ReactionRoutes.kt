package com.absaide.server.routes

import com.absaide.server.models.*
import com.absaide.server.repositories.ReactionRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.reactionRoutes() {
    val repo = ReactionRepository()

    route("/reactions") {
        post {
            val userId = call.principal<JWTPrincipal>()
                ?.payload?.getClaim("userId")?.asInt()
                ?: return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Nothing>(false, "No autorizado")
                )
            val req = call.receive<ReactionRequest>()
            val result = repo.addOrUpdate(userId, req.artworkId, req.emoji)
            call.respond(HttpStatusCode.OK,
                ApiResponse(true, "Reacción guardada", result))
        }

        delete("/{artworkId}") {
            val userId = call.principal<JWTPrincipal>()
                ?.payload?.getClaim("userId")?.asInt()
                ?: return@delete call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Nothing>(false, "No autorizado")
                )
            val artworkId = call.parameters["artworkId"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest)
            repo.remove(userId, artworkId)
            call.respond(
                HttpStatusCode.OK,
                ApiResponse<Nothing>(true, "Reacción eliminada")
            )
        }

        get("/artwork/{artworkId}") {
            val artworkId = call.parameters["artworkId"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest)
            val reactions = repo.getByArtwork(artworkId)
            call.respond(HttpStatusCode.OK, reactions)
        }

        get("/mine/{artworkId}") {
            val userId = call.principal<JWTPrincipal>()
                ?.payload?.getClaim("userId")?.asInt()
                ?: return@get call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Nothing>(false, "No autorizado")
                )
            val artworkId = call.parameters["artworkId"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest)
            val emoji = repo.getUserReaction(userId, artworkId)
            call.respond(HttpStatusCode.OK,
                ApiResponse(true, "OK", mapOf("emoji" to (emoji ?: ""))))
        }
    }
}