package com.absaide.server.routes

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.absaide.server.models.ApiResponse
import java.io.File
import java.util.UUID

fun Route.uploadRoutes() {
    val uploadsDir = File("uploads").apply { mkdirs() }

    post("/upload/image") {
        val principal = call.principal<JWTPrincipal>()
        val userId = principal?.payload?.getClaim("userId")?.asInt()
            ?: return@post call.respond(
                HttpStatusCode.Unauthorized,
                ApiResponse<Nothing>(false, "No autorizado")
            )

        val multipart = call.receiveMultipart()
        var imageUrl: String? = null

        multipart.forEachPart { part ->
            when (part) {
                is PartData.FileItem -> {
                    val extension = part.originalFileName
                        ?.substringAfterLast(".", "jpg") ?: "jpg"
                    val fileName = "${UUID.randomUUID()}.$extension"
                    val file = File(uploadsDir, fileName)
                    part.streamProvider().use { input ->
                        file.outputStream().buffered().use { output ->
                            input.copyTo(output)
                        }
                    }
                    imageUrl = "http://192.168.117.92:8080/uploads/$fileName"
                }
                else -> {}
            }
            part.dispose()
        }

        if (imageUrl != null) {
            call.respond(
                HttpStatusCode.OK,
                ApiResponse(true, "Imagen subida", mapOf("url" to imageUrl!!))
            )
        } else {
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Nothing>(false, "No se recibió imagen")
            )
        }
    }

    get("/uploads/{filename}") {
        val filename = call.parameters["filename"]
            ?: return@get call.respond(HttpStatusCode.BadRequest)
        val file = File("uploads/$filename")
        if (file.exists()) {
            call.respondFile(file)
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }
}
