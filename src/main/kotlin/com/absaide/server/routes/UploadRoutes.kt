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
import java.net.HttpURLConnection
import java.net.URL
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.security.MessageDigest

fun Route.uploadRoutes() {

    post("/upload/image") {
        val principal = call.principal<JWTPrincipal>()
        val userId = principal?.payload?.getClaim("userId")?.asInt()
            ?: return@post call.respond(
                HttpStatusCode.Unauthorized,
                ApiResponse<Nothing>(false, "No autorizado")
            )

        val multipart = call.receiveMultipart()
        var imageBytes: ByteArray? = null
        var fileName = "image.jpg"

        multipart.forEachPart { part ->
            when (part) {
                is PartData.FileItem -> {
                    fileName   = part.originalFileName ?: "image.jpg"
                    imageBytes = part.streamProvider().readBytes()
                }
                else -> {}
            }
            part.dispose()
        }

        if (imageBytes == null) {
            return@post call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Nothing>(false, "No se recibió imagen")
            )
        }

        try {
            val cloudName = "dkn0uaome"
            val apiKey    = "894776483867838"
            val apiSecret = "h_HFwkCIRfkKxdeNcSxCTvZcGZ8"
            val timestamp = (System.currentTimeMillis() / 1000).toString()

            // Generar firma SHA1
            val toSign    = "timestamp=$timestamp$apiSecret"
            val signature = MessageDigest.getInstance("SHA-1")
                .digest(toSign.toByteArray())
                .joinToString("") { "%02x".format(it) }

            val boundary = "Boundary-${System.currentTimeMillis()}"
            val url      = URL("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
            val conn     = url.openConnection() as HttpURLConnection

            conn.requestMethod = "POST"
            conn.doOutput      = true
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

            val body   = ByteArrayOutputStream()
            val writer = PrintStream(body, true, "UTF-8")

            // api_key
            writer.println("--$boundary")
            writer.println("Content-Disposition: form-data; name=\"api_key\"")
            writer.println()
            writer.println(apiKey)

            // timestamp
            writer.println("--$boundary")
            writer.println("Content-Disposition: form-data; name=\"timestamp\"")
            writer.println()
            writer.println(timestamp)

            // signature
            writer.println("--$boundary")
            writer.println("Content-Disposition: form-data; name=\"signature\"")
            writer.println()
            writer.println(signature)

            // file
            writer.println("--$boundary")
            writer.println("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"")
            writer.println("Content-Type: image/jpeg")
            writer.println()
            writer.flush()
            body.write(imageBytes!!)
            writer.println()
            writer.println("--$boundary--")
            writer.flush()

            conn.outputStream.write(body.toByteArray())
            conn.outputStream.flush()

            val response = try {
                conn.inputStream.bufferedReader().readText()
            } catch (e: Exception) {
                conn.errorStream?.bufferedReader()?.readText() ?: throw e
            }

            val urlRegex  = Regex("\"secure_url\"\\s*:\\s*\"([^\"]+)\"")
            val secureUrl = urlRegex.find(response)?.groupValues?.get(1)?.replace("\\/", "/")
                ?: return@post call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Nothing>(false, "Error Cloudinary: $response")
                )

            call.respond(
                HttpStatusCode.OK,
                ApiResponse(true, "Imagen subida", mapOf("url" to secureUrl))
            )

        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiResponse<Nothing>(false, "Error: ${e.message}")
            )
        }
    }
}