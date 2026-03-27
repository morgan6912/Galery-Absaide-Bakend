package com.absaide.server.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init(config: ApplicationConfig) {
        val hc = HikariConfig().apply {
            driverClassName  = config.property("database.driverClassName").getString()
            jdbcUrl          = config.property("database.jdbcURL").getString()
            username         = config.property("database.username").getString()
            password         = config.property("database.password").getString()
            maximumPoolSize  = config.property("database.maximumPoolSize").getString().toInt()
            isAutoCommit     = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        Database.connect(HikariDataSource(hc))
        transaction { SchemaUtils.create(Users, Artworks, Favorites) }
    }
}

object Users : Table("users") {
    val id       = integer("id").autoIncrement()
    val name     = varchar("name", 100)
    val email    = varchar("email", 150).uniqueIndex()
    val password = varchar("password", 255)
    val role     = enumerationByName("role", 10, RoleEnum::class)
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

enum class RoleEnum { ADMIN, ARTIST, USER }
