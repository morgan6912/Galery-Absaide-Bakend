package com.absaide.server.services

import at.favre.lib.crypto.bcrypt.BCrypt
import com.absaide.server.models.*
import com.absaide.server.repositories.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

class AuthService(private val userRepo: UserRepository, private val jwtSecret: String, private val jwtIssuer: String, private val jwtAudience: String) {
    fun login(req: LoginRequest): AuthResponse? {
        val hash  = userRepo.findPasswordHash(req.email) ?: return null
        if (!BCrypt.verifyer().verify(req.password.toCharArray(), hash).verified) return null
        val user  = userRepo.findByEmail(req.email) ?: return null
        return AuthResponse(token(user.id, user.role), user)
    }
    fun register(req: RegisterRequest): AuthResponse? {
        if (userRepo.findByEmail(req.email) != null) return null
        val hashed = BCrypt.withDefaults().hashToString(12, req.password.toCharArray())
        val user   = userRepo.create(req, hashed)
        return AuthResponse(token(user.id, user.role), user)
    }
    private fun token(userId: Int, role: String): String = JWT.create()
        .withAudience(jwtAudience).withIssuer(jwtIssuer)
        .withClaim("userId", userId).withClaim("role", role)
        .withExpiresAt(Date(System.currentTimeMillis() + 86_400_000L * 7))
        .sign(Algorithm.HMAC256(jwtSecret))
}

class UserService(private val repo: UserRepository) {
    fun getAll()         = repo.findAll()
    fun getById(id: Int) = repo.findById(id)
    fun delete(id: Int)  = repo.delete(id)
}

class ArtworkService(private val repo: ArtworkRepository) {
    fun getAll()                                   = repo.findAll()
    fun create(artistId: Int, req: ArtworkRequest) = repo.create(artistId, req)
    fun delete(id: Int)                            = repo.delete(id)
}

class FavoriteService(private val repo: FavoriteRepository) {
    fun getFavorites(userId: Int)           = repo.getFavoriteArtworks(userId)
    fun add(userId: Int, artworkId: Int)    = repo.add(userId, artworkId)
    fun remove(userId: Int, artworkId: Int) = repo.remove(userId, artworkId)
}
