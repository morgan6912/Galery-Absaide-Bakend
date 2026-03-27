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
