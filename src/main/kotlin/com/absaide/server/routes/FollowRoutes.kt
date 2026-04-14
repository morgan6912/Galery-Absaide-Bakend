package com.absaide.server.routes

import com.absaide.server.models.*
import com.absaide.server.repositories.FollowRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.followRoutes() {
    val repo = FollowRepository()

    route("/follows") {
        post("/{artistId}") {
            val followerId = call.principal<JWTPrincipal>()
                ?.payload?.getClaim("userId")?.asInt()
                ?: return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Nothing>(false, "No autorizado")
                )
            val artistId = call.parameters["artistId"]?.toIntOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest)
            val result = repo.follow(followerId, artistId)
            call.respond(HttpStatusCode.OK,
                ApiResponse(true, "Siguiendo", result))
        }

        delete("/{artistId}") {
            val followerId = call.principal<JWTPrincipal>()
                ?.payload?.getClaim("userId")?.asInt()
                ?: return@delete call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Nothing>(false, "No autorizado")
                )
            val artistId = call.parameters["artistId"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest)
            repo.unfollow(followerId, artistId)
            call.respond(
                HttpStatusCode.OK,
                ApiResponse<Nothing>(true, "Dejaste de seguir")
            )
        }

        get("/following") {
            val followerId = call.principal<JWTPrincipal>()
                ?.payload?.getClaim("userId")?.asInt()
                ?: return@get call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Nothing>(false, "No autorizado")
                )
            val artists = repo.getFollowing(followerId)
            call.respond(HttpStatusCode.OK, artists)
        }

        get("/artists") {
            val userId = call.principal<JWTPrincipal>()
                ?.payload?.getClaim("userId")?.asInt()
                ?: return@get call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Nothing>(false, "No autorizado")
                )
            val artists = repo.getArtists(userId)
            call.respond(HttpStatusCode.OK, artists)
        }

        get("/count/{artistId}") {
            val artistId = call.parameters["artistId"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest)
            val count = repo.getFollowers(artistId)
            call.respond(HttpStatusCode.OK,
                ApiResponse(true, "OK", mapOf("count" to count)))
        }
    }
}