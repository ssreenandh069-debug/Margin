package com.attendease.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "guest",
    val name: String,
    val startDate: Long,
    val isActive: Boolean = false,
    val lastModified: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
