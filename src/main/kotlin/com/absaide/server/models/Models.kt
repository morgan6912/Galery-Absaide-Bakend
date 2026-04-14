package com.absaide.server.models

import kotlinx.serialization.Serializable

@Serializable data class UserDto(val id: Int = 0, val name: String, val email: String, val role: String)
@Serializable data class ArtworkDto(val id: Int = 0, val title: String, val description: String, val artistId: Int, val imageUrl: String, val artistName: String = "")
@Serializable data class FavoriteDto(val id: Int = 0, val userId: Int, val artworkId: Int)
@Serializable data class LoginRequest(val email: String, val password: String)
@Serializable data class RegisterRequest(val name: String, val email: String, val password: String, val role: String = "USER")
@Serializable data class ArtworkRequest(val title: String, val description: String, val imageUrl: String)
@Serializable data class FavoriteRequest(val artworkId: Int)
@Serializable data class AuthResponse(val token: String, val user: UserDto)
@Serializable data class ApiResponse<T>(val success: Boolean, val message: String = "", val data: T? = null)

@Serializable data class InterestRequest(val artworkId: Int)
@Serializable data class InterestDto(val id: Int, val userId: Int, val artworkId: Int)

@Serializable data class MessageRequest(val receiverId: Int, val artworkId: Int, val content: String)
@Serializable data class MessageDto(
    val id: Int, val senderId: Int, val senderName: String,
    val receiverId: Int, val artworkId: Int, val artworkTitle: String,
    val content: String, val createdAt: String
)

@Serializable data class ReactionDto(val id: Int, val userId: Int, val artworkId: Int, val emoji: String)
@Serializable data class ReactionRequest(val artworkId: Int, val emoji: String)
@Serializable data class ReactionCountDto(val emoji: String, val count: Int)

@Serializable data class FollowDto(val id: Int, val followerId: Int, val artistId: Int)
@Serializable data class ArtistPublicDto(
    val id: Int, val name: String, val email: String,
    val artworkCount: Int, val followerCount: Int, val isFollowing: Boolean
)

// ── Nuevos modelos ─────────────────────────────────────────────────────────
@Serializable data class UpdateRoleRequest(val role: String)
@Serializable data class UpdateStatusRequest(val status: String)

@Serializable data class ExhibitionDto(
    val id: Int, val name: String, val description: String,
    val startDate: String, val endDate: String,
    val status: String, val ownerId: Int
)
@Serializable data class ExhibitionRequest(
    val name: String, val description: String = "",
    val startDate: String, val endDate: String
)

@Serializable data class ArtistRequestDto(
    val id: Int, val name: String, val email: String,
    val description: String, val website: String,
    val status: String, val createdAt: String
)
@Serializable data class ArtistRequestRequest(
    val name: String, val email: String,
    val description: String, val website: String = ""
)
@Serializable data class ProcessRequestDto(val status: String)