package com.absaide.server.repositories

import com.absaide.server.database.*
import com.absaide.server.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class UserRepository {
    fun findByEmail(email: String): UserDto? = transaction {
        Users.selectAll().where { Users.email eq email }.singleOrNull()?.toDto()
    }
    fun findById(id: Int): UserDto? = transaction {
        Users.selectAll().where { Users.id eq id }.singleOrNull()?.toDto()
    }
    fun findPasswordHash(email: String): String? = transaction {
        Users.selectAll().where { Users.email eq email }.singleOrNull()?.get(Users.password)
    }
    fun create(req: RegisterRequest, hashed: String): UserDto = transaction {
        val role = runCatching { RoleEnum.valueOf(req.role.uppercase()) }.getOrDefault(RoleEnum.USER)
        val stmt = Users.insert {
            it[name]       = req.name
            it[email]      = req.email
            it[password]   = hashed
            it[Users.role] = role
        }
        UserDto(stmt[Users.id], req.name, req.email, role.name)
    }
    fun findAll(): List<UserDto> = transaction { Users.selectAll().map { it.toDto() } }
    fun delete(id: Int): Boolean = transaction {
        Users.deleteWhere { Users.id eq id } > 0
    }
    private fun ResultRow.toDto() = UserDto(
        this[Users.id], this[Users.name], this[Users.email], this[Users.role].name
    )
}

class ArtworkRepository {
    fun findAll(): List<ArtworkDto> = transaction {
        (Artworks innerJoin Users).selectAll().map { it.toDto() }
    }
    fun create(artistId: Int, req: ArtworkRequest): ArtworkDto = transaction {
        val stmt = Artworks.insert {
            it[title]             = req.title
            it[description]       = req.description
            it[Artworks.artistId] = artistId
            it[imageUrl]          = req.imageUrl
        }
        val artistName = Users.selectAll()
            .where { Users.id eq artistId }
            .singleOrNull()?.get(Users.name) ?: ""
        ArtworkDto(stmt[Artworks.id], req.title, req.description, artistId, req.imageUrl, artistName)
    }
    fun delete(id: Int): Boolean = transaction {
        Artworks.deleteWhere { Artworks.id eq id } > 0
    }
    private fun ResultRow.toDto() = ArtworkDto(
        this[Artworks.id], this[Artworks.title], this[Artworks.description],
        this[Artworks.artistId], this[Artworks.imageUrl], this[Users.name]
    )
}

class FavoriteRepository {
    fun getFavoriteArtworks(userId: Int): List<ArtworkDto> = transaction {
        val ids = Favorites.selectAll()
            .where { Favorites.userId eq userId }
            .map { it[Favorites.artworkId] }
        (Artworks innerJoin Users).selectAll()
            .where { Artworks.id inList ids }
            .map {
                ArtworkDto(
                    it[Artworks.id], it[Artworks.title], it[Artworks.description],
                    it[Artworks.artistId], it[Artworks.imageUrl], it[Users.name]
                )
            }
    }
    fun add(userId: Int, artworkId: Int): FavoriteDto = transaction {
        val stmt = Favorites.insert {
            it[Favorites.userId]    = userId
            it[Favorites.artworkId] = artworkId
        }
        FavoriteDto(stmt[Favorites.id], userId, artworkId)
    }
    fun remove(userId: Int, artworkId: Int): Boolean = transaction {
        Favorites.deleteWhere {
            (Favorites.userId eq userId) and (Favorites.artworkId eq artworkId)
        } > 0
    }
}
