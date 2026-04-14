package com.absaide.server.routes

import com.absaide.server.models.*
import com.absaide.server.repositories.InterestRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.interestRoutes() {
    val repo = InterestRepository()

    route("/interests") {
        post {
            val userId = call.principal<JWTPrincipal>()
                ?.payload?.getClaim("userId")?.asInt()
                ?: return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Nothing>(false, "No autorizado")
                )
            val req = call.receive<InterestRequest>()
            val result = repo.add(userId, req.artworkId)
            call.respond(HttpStatusCode.OK,
                ApiResponse(true, "Interés agregado", result))
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
            call.respond(HttpStatusCode.OK,
                ApiResponse<Nothing>(true, "Interés eliminado"))
        }

        get("/mine") {
            val userId = call.principal<JWTPrincipal>()
                ?.payload?.getClaim("userId")?.asInt()
                ?: return@get call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Nothing>(false, "No autorizado")
                )
            val artworks = repo.getByUser(userId)
            call.respond(HttpStatusCode.OK, artworks)
        }

        get("/count/{artworkId}") {
            val artworkId = call.parameters["artworkId"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest)
            val count = repo.getByArtwork(artworkId)
            call.respond(HttpStatusCode.OK,
                ApiResponse(true, "OK", mapOf("count" to count)))
        }
    }
}