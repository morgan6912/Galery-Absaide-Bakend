package com.absaide.server.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init(config: ApplicationConfig) {
        val hc = HikariConfig().apply {
            driverClassName      = config.property("database.driverClassName").getString()
            jdbcUrl              = config.property("database.jdbcURL").getString()
            username             = config.property("database.username").getString()
            password             = config.property("database.password").getString()
            maximumPoolSize      = config.property("database.maximumPoolSize").getString().toInt()
            isAutoCommit         = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        Database.connect(HikariDataSource(hc))
        transaction {
            SchemaUtils.create(
                Users, Artworks, Favorites, Interests,
                Messages, Reactions, Follows,
                Exhibitions, ArtistRequests
            )
        }
    }
}

enum class RoleEnum { ADMIN, ARTIST, USER }

object Users : Table("users") {
    val id       = integer("id").autoIncrement()
    val name     = varchar("name", 100)
    val email    = varchar("email", 150).uniqueIndex()
    val password = varchar("password", 255)
    val role     = enumerationByName("role", 10, RoleEnum::class)
    val status   = varchar("status", 20).default("Activo")
    override val primaryKey = PrimaryKey(id)
}

object Artworks : Table("artworks") {
    val id          = integer("id").autoIncrement()
    val title       = varchar("title", 200)
    val description = text("description")
    val artistId    = integer("artist_id").references(Users.id)
    val imageUrl    = varchar("image_url", 500)
    override val primaryKey = PrimaryKey(id)
}

object Favorites : Table("favorites") {
    val id        = integer("id").autoIncrement()
    val userId    = integer("user_id").references(Users.id)
    val artworkId = integer("artwork_id").references(Artworks.id)
    override val primaryKey = PrimaryKey(id)
    init { uniqueIndex(userId, artworkId) }
}

object Interests : Table("interests") {
    val id        = integer("id").autoIncrement()
    val userId    = integer("user_id").references(Users.id)
    val artworkId = integer("artwork_id").references(Artworks.id)
    override val primaryKey = PrimaryKey(id)
    init { uniqueIndex(userId, artworkId) }
}

object Messages : Table("messages") {
    val id         = integer("id").autoIncrement()
    val senderId   = integer("sender_id").references(Users.id)
    val receiverId = integer("receiver_id").references(Users.id)
    val artworkId  = integer("artwork_id").references(Artworks.id)
    val content    = text("content")
    val createdAt  = varchar("created_at", 50)
    override val primaryKey = PrimaryKey(id)
}

object Reactions : Table("reactions") {
    val id        = integer("id").autoIncrement()
    val userId    = integer("user_id").references(Users.id)
    val artworkId = integer("artwork_id").references(Artworks.id)
    val emoji     = varchar("emoji", 10)
    val createdAt = varchar("created_at", 50).default("")
    override val primaryKey = PrimaryKey(id)
    init { uniqueIndex(userId, artworkId) }
}

object Follows : Table("follows") {
    val id         = integer("id").autoIncrement()
    val followerId = integer("follower_id").references(Users.id)
    val artistId   = integer("artist_id").references(Users.id)
    val createdAt  = varchar("created_at", 50).default("")
    override val primaryKey = PrimaryKey(id)
    init { uniqueIndex(followerId, artistId) }
}

object Exhibitions : Table("exhibitions") {
    val id          = integer("id").autoIncrement()
    val name        = varchar("name", 200)
    val description = text("description").default("")
    val startDate   = varchar("start_date", 20).default("")
    val endDate     = varchar("end_date", 20).default("")
    val status      = varchar("status", 20).default("activa")
    val ownerId     = integer("owner_id").references(Users.id)
    override val primaryKey = PrimaryKey(id)
}

object ArtistRequests : Table("artist_requests") {
    val id          = integer("id").autoIncrement()
    val name        = varchar("name", 200)
    val email       = varchar("email", 200)
    val description = text("description").default("")
    val website     = varchar("website", 300).default("")
    val status      = varchar("status", 20).default("pendiente")
    val createdAt   = varchar("created_at", 50).default("")
    override val primaryKey = PrimaryKey(id)
}