package com.absaide.server.routes

import com.absaide.server.models.*
import com.absaide.server.repositories.*
import com.absaide.server.services.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(jwtSecret: String, jwtIssuer: String, jwtAudience: String) {
    val svc = AuthService(UserRepository(), jwtSecret, jwtIssuer, jwtAudience)
    post("/login") {
        val req = call.receive<LoginRequest>()
        svc.login(req)?.let { call.respond(HttpStatusCode.OK, it) }
            ?: call.respond(HttpStatusCode.Unauthorized, ApiResponse<Nothing>(false, "Credenciales inválidas"))
    }
    post("/register") {
        val req = call.receive<RegisterRequest>()
        svc.register(req)?.let { call.respond(HttpStatusCode.Created, it) }
            ?: call.respond(HttpStatusCode.Conflict, ApiResponse<Nothing>(false, "Email ya registrado"))
    }
}

fun Route.artworkRoutes() {
    val svc = ArtworkService(ArtworkRepository())
    route("/artworks") {
        get { call.respond(HttpStatusCode.OK, svc.getAll()) }
        post {
            val artistId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                ?: return@post call.respond(HttpStatusCode.Unauthorized)
            call.respond(HttpStatusCode.Created, svc.create(artistId, call.receive()))
        }
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            if (svc.delete(id)) call.respond(HttpStatusCode.OK, ApiResponse<Nothing>(true, "Obra eliminada"))
            else call.respond(HttpStatusCode.NotFound, ApiResponse<Nothing>(false, "No encontrada"))
        }
    }
}

fun Route.userRoutes() {
    val svc = UserService(UserRepository())
    route("/users") {
        get {
            val role = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()
            if (role != "ADMIN") return@get call.respond(HttpStatusCode.Forbidden, ApiResponse<Nothing>(false, "Solo admins"))
            call.respond(HttpStatusCode.OK, svc.getAll())
        }
        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
            svc.getById(id)?.let { call.respond(HttpStatusCode.OK, it) }
                ?: call.respond(HttpStatusCode.NotFound, ApiResponse<Nothing>(false, "No encontrado"))
        }
        delete("/{id}") {
            val role = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()
            if (role != "ADMIN") return@delete call.respond(HttpStatusCode.Forbidden)
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            if (svc.delete(id)) call.respond(HttpStatusCode.OK, ApiResponse<Nothing>(true, "Usuario eliminado"))
            else call.respond(HttpStatusCode.NotFound, ApiResponse<Nothing>(false, "No encontrado"))
        }
    }
}

fun Route.favoriteRoutes() {
    val svc = FavoriteService(FavoriteRepository())
    route("/favorites") {
        get {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                ?: return@get call.respond(HttpStatusCode.Unauthorized)
            call.respond(HttpStatusCode.OK, svc.getFavorites(userId))
        }
        post {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                ?: return@post call.respond(HttpStatusCode.Unauthorized)
            call.respond(HttpStatusCode.Created, svc.add(userId, call.receive<FavoriteRequest>().artworkId))
        }
        delete("/{artworkId}") {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt()
                ?: return@delete call.respond(HttpStatusCode.Unauthorized)
            val artworkId = call.parameters["artworkId"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest)
            if (svc.remove(userId, artworkId)) call.respond(HttpStatusCode.OK, ApiResponse<Nothing>(true, "Eliminado"))
            else call.respond(HttpStatusCode.NotFound, ApiResponse<Nothing>(false, "No encontrado"))
        }
    }
}
