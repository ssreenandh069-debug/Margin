package com.attendease.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "timetable",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId"), Index("dayOfWeek")]
)
data class TimetableEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "guest",
    val subjectId: String,
    val dayOfWeek: Int, // 1=Mon … 7=Sun
    val startTime: String, // e.g. "09:00 AM"
    val lastModified: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
