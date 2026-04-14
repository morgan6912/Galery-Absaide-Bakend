package com.absaide.server.repositories

import com.absaide.server.database.*
import com.absaide.server.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class UserRepository {
    fun findByEmail(email: String): UserDto? = transaction {
        Users.selectAll().where { Users.email eq email }.singleOrNull()?.toUserDto()
    }
    fun findById(id: Int): UserDto? = transaction {
        Users.selectAll().where { Users.id eq id }.singleOrNull()?.toUserDto()
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
    fun findAll(): List<UserDto> = transaction {
        Users.selectAll().map { it.toUserDto() }
    }
    fun delete(id: Int): Boolean = transaction {
        Users.deleteWhere { Users.id eq id } > 0
    }
    private fun ResultRow.toUserDto() = UserDto(
        this[Users.id], this[Users.name], this[Users.email], this[Users.role].name
    )
}

class ArtworkRepository {
    fun findAll(): List<ArtworkDto> = transaction {
        (Artworks innerJoin Users).selectAll().map { it.toArtworkDto() }
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
    private fun ResultRow.toArtworkDto() = ArtworkDto(
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

class InterestRepository {
    fun add(userId: Int, artworkId: Int): InterestDto = transaction {
        val stmt = Interests.insert {
            it[Interests.userId]    = userId
            it[Interests.artworkId] = artworkId
        }
        InterestDto(stmt[Interests.id], userId, artworkId)
    }
    fun remove(userId: Int, artworkId: Int): Boolean = transaction {
        Interests.deleteWhere {
            (Interests.userId eq userId) and (Interests.artworkId eq artworkId)
        } > 0
    }
    fun getByArtwork(artworkId: Int): Int = transaction {
        Interests.selectAll()
            .where { Interests.artworkId eq artworkId }
            .count().toInt()
    }
    fun getByUser(userId: Int): List<ArtworkDto> = transaction {
        val ids = Interests.selectAll()
            .where { Interests.userId eq userId }
            .map { it[Interests.artworkId] }
        (Artworks innerJoin Users).selectAll()
            .where { Artworks.id inList ids }
            .map {
                ArtworkDto(
                    it[Artworks.id], it[Artworks.title], it[Artworks.description],
                    it[Artworks.artistId], it[Artworks.imageUrl], it[Users.name]
                )
            }
    }
    fun isInterested(userId: Int, artworkId: Int): Boolean = transaction {
        Interests.selectAll()
            .where {
                (Interests.userId eq userId) and
                        (Interests.artworkId eq artworkId)
            }.count() > 0
    }
}

class MessageRepository {
    fun send(senderId: Int, receiverId: Int, artworkId: Int, content: String): MessageDto = transaction {
        val now = java.time.LocalDateTime.now().toString()
        val stmt = Messages.insert {
            it[Messages.senderId]   = senderId
            it[Messages.receiverId] = receiverId
            it[Messages.artworkId]  = artworkId
            it[Messages.content]    = content
            it[Messages.createdAt]  = now
        }
        val senderName   = Users.selectAll()
            .where { Users.id eq senderId }.single()[Users.name]
        val artworkTitle = Artworks.selectAll()
            .where { Artworks.id eq artworkId }.single()[Artworks.title]
        MessageDto(
            stmt[Messages.id], senderId, senderName,
            receiverId, artworkId, artworkTitle, content, now
        )
    }
    fun getReceived(userId: Int): List<MessageDto> = transaction {
        Messages.selectAll()
            .where { Messages.receiverId eq userId }
            .map { row ->
                val senderName   = Users.selectAll()
                    .where { Users.id eq row[Messages.senderId] }.single()[Users.name]
                val artworkTitle = Artworks.selectAll()
                    .where { Artworks.id eq row[Messages.artworkId] }.single()[Artworks.title]
                MessageDto(
                    row[Messages.id], row[Messages.senderId], senderName,
                    row[Messages.receiverId], row[Messages.artworkId],
                    artworkTitle, row[Messages.content], row[Messages.createdAt]
                )
            }
    }
    fun getSent(userId: Int): List<MessageDto> = transaction {
        Messages.selectAll()
            .where { Messages.senderId eq userId }
            .map { row ->
                val senderName   = Users.selectAll()
                    .where { Users.id eq row[Messages.senderId] }.single()[Users.name]
                val artworkTitle = Artworks.selectAll()
                    .where { Artworks.id eq row[Messages.artworkId] }.single()[Artworks.title]
                MessageDto(
                    row[Messages.id], row[Messages.senderId], senderName,
                    row[Messages.receiverId], row[Messages.artworkId],
                    artworkTitle, row[Messages.content], row[Messages.createdAt]
                )
            }
    }
}

// ── Reactions ──────────────────────────────────────────────────────────────
class ReactionRepository {
    fun addOrUpdate(userId: Int, artworkId: Int, emoji: String): ReactionDto = transaction {
        Reactions.deleteWhere {
            (Reactions.userId eq userId) and (Reactions.artworkId eq artworkId)
        }
        val stmt = Reactions.insert {
            it[Reactions.userId]    = userId
            it[Reactions.artworkId] = artworkId
            it[Reactions.emoji]     = emoji
        }
        ReactionDto(stmt[Reactions.id], userId, artworkId, emoji)
    }

    fun remove(userId: Int, artworkId: Int): Boolean = transaction {
        Reactions.deleteWhere {
            (Reactions.userId eq userId) and (Reactions.artworkId eq artworkId)
        } > 0
    }

    fun getByArtwork(artworkId: Int): List<ReactionCountDto> = transaction {
        Reactions.selectAll()
            .where { Reactions.artworkId eq artworkId }
            .groupBy { it[Reactions.emoji] }
            .map { (emoji, rows) -> ReactionCountDto(emoji, rows.size) }
    }

    fun getUserReaction(userId: Int, artworkId: Int): String? = transaction {
        Reactions.selectAll()
            .where {
                (Reactions.userId eq userId) and
                        (Reactions.artworkId eq artworkId)
            }.singleOrNull()?.get(Reactions.emoji)
    }

    fun getAllForArtworks(artworkIds: List<Int>): Map<Int, List<ReactionCountDto>> = transaction {
        Reactions.selectAll()
            .where { Reactions.artworkId inList artworkIds }
            .groupBy { it[Reactions.artworkId] }
            .mapValues { (_, rows) ->
                rows.groupBy { it[Reactions.emoji] }
                    .map { (emoji, r) -> ReactionCountDto(emoji, r.size) }
            }
    }
}

// ── Follows ────────────────────────────────────────────────────────────────
class FollowRepository {
    fun follow(followerId: Int, artistId: Int): FollowDto = transaction {
        val stmt = Follows.insert {
            it[Follows.followerId] = followerId
            it[Follows.artistId]   = artistId
        }
        FollowDto(stmt[Follows.id], followerId, artistId)
    }

    fun unfollow(followerId: Int, artistId: Int): Boolean = transaction {
        Follows.deleteWhere {
            (Follows.followerId eq followerId) and (Follows.artistId eq artistId)
        } > 0
    }

    fun isFollowing(followerId: Int, artistId: Int): Boolean = transaction {
        Follows.selectAll()
            .where {
                (Follows.followerId eq followerId) and
                        (Follows.artistId eq artistId)
            }.count() > 0
    }

    fun getFollowing(followerId: Int): List<ArtistPublicDto> = transaction {
        val artistIds = Follows.selectAll()
            .where { Follows.followerId eq followerId }
            .map { it[Follows.artistId] }
        Users.selectAll()
            .where { Users.id inList artistIds }
            .map { user ->
                val artworkCount  = Artworks.selectAll()
                    .where { Artworks.artistId eq user[Users.id] }.count().toInt()
                val followerCount = Follows.selectAll()
                    .where { Follows.artistId eq user[Users.id] }.count().toInt()
                ArtistPublicDto(
                    id            = user[Users.id],
                    name          = user[Users.name],
                    email         = user[Users.email],
                    artworkCount  = artworkCount,
                    followerCount = followerCount,
                    isFollowing   = true
                )
            }
    }

    fun getFollowers(artistId: Int): Int = transaction {
        Follows.selectAll()
            .where { Follows.artistId eq artistId }
            .count().toInt()
    }

    fun getArtists(followerId: Int): List<ArtistPublicDto> = transaction {
        val followingIds = Follows.selectAll()
            .where { Follows.followerId eq followerId }
            .map { it[Follows.artistId] }.toSet()
        Users.selectAll()
            .where { Users.role eq RoleEnum.ARTIST }
            .map { user ->
                val artworkCount  = Artworks.selectAll()
                    .where { Artworks.artistId eq user[Users.id] }.count().toInt()
                val followerCount = Follows.selectAll()
                    .where { Follows.artistId eq user[Users.id] }.count().toInt()
                ArtistPublicDto(
                    id            = user[Users.id],
                    name          = user[Users.name],
                    email         = user[Users.email],
                    artworkCount  = artworkCount,
                    followerCount = followerCount,
                    isFollowing   = user[Users.id] in followingIds
                )
            }
    }
}
// ── User Admin ─────────────────────────────────────────────────────────────
class UserAdminRepository {
    fun updateRole(id: Int, role: String): Boolean = transaction {
        val roleEnum = runCatching { RoleEnum.valueOf(role.uppercase()) }
            .getOrDefault(RoleEnum.USER)
        Users.update({ Users.id eq id }) {
            it[Users.role] = roleEnum
        } > 0
    }

    fun updateStatus(id: Int, status: String): Boolean = transaction {
        Users.update({ Users.id eq id }) {
            it[Users.status] = status
        } > 0
    }
}

// ── Exhibitions ────────────────────────────────────────────────────────────
class ExhibitionRepository {
    fun getAll(): List<ExhibitionDto> = transaction {
        Exhibitions.selectAll().map { it.toDto() }
    }

    fun getByOwner(ownerId: Int): List<ExhibitionDto> = transaction {
        Exhibitions.selectAll()
            .where { Exhibitions.ownerId eq ownerId }
            .map { it.toDto() }
    }

    fun create(ownerId: Int, req: ExhibitionRequest): ExhibitionDto = transaction {
        val stmt = Exhibitions.insert {
            it[Exhibitions.name]        = req.name
            it[Exhibitions.description] = req.description
            it[Exhibitions.startDate]   = req.startDate
            it[Exhibitions.endDate]     = req.endDate
            it[Exhibitions.status]      = "activa"
            it[Exhibitions.ownerId]     = ownerId
        }
        ExhibitionDto(stmt[Exhibitions.id], req.name, req.description,
            req.startDate, req.endDate, "activa", ownerId)
    }

    fun update(id: Int, req: ExhibitionRequest): Boolean = transaction {
        Exhibitions.update({ Exhibitions.id eq id }) {
            it[Exhibitions.name]        = req.name
            it[Exhibitions.description] = req.description
            it[Exhibitions.startDate]   = req.startDate
            it[Exhibitions.endDate]     = req.endDate
        } > 0
    }

    fun delete(id: Int): Boolean = transaction {
        Exhibitions.deleteWhere { Exhibitions.id eq id } > 0
    }

    private fun ResultRow.toDto() = ExhibitionDto(
        this[Exhibitions.id], this[Exhibitions.name], this[Exhibitions.description],
        this[Exhibitions.startDate], this[Exhibitions.endDate],
        this[Exhibitions.status], this[Exhibitions.ownerId]
    )
}

// ── Artist Requests ────────────────────────────────────────────────────────
class ArtistRequestRepository {
    fun getAll(): List<ArtistRequestDto> = transaction {
        ArtistRequests.selectAll().map { it.toDto() }
    }

    fun create(req: ArtistRequestRequest): ArtistRequestDto = transaction {
        val now = java.time.LocalDateTime.now().toString()
        val stmt = ArtistRequests.insert {
            it[ArtistRequests.name]        = req.name
            it[ArtistRequests.email]       = req.email
            it[ArtistRequests.description] = req.description
            it[ArtistRequests.website]     = req.website
            it[ArtistRequests.status]      = "pendiente"
            it[ArtistRequests.createdAt]   = now
        }
        ArtistRequestDto(stmt[ArtistRequests.id], req.name, req.email,
            req.description, req.website, "pendiente", now)
    }

    fun process(id: Int, status: String): Boolean = transaction {
        ArtistRequests.update({ ArtistRequests.id eq id }) {
            it[ArtistRequests.status] = status
        } > 0
    }

    private fun ResultRow.toDto() = ArtistRequestDto(
        this[ArtistRequests.id], this[ArtistRequests.name], this[ArtistRequests.email],
        this[ArtistRequests.description], this[ArtistRequests.website],
        this[ArtistRequests.status], this[ArtistRequests.createdAt]
    )
}