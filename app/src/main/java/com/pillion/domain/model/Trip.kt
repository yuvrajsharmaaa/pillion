package com.pillion.domain.model

data class Trip(
    val id: Long = 0L,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
)
