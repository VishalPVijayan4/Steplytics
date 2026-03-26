package com.buildndeploy.steplytics.domain.model

data class UserProfile(
    val name: String,
    val email: String,
    val age: Int,
    val weight: Float,
    val height: Float,
    val gender: String
)
