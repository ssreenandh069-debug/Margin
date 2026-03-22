package com.Margin.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId"), Index("date")]
)
data class AttendanceRecordEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "guest",
    val subjectId: String,
    val date: Long, // Unix day timestamp (start of day millis)
    val status: String = "NONE", // PRESENT | ABSENT | CANCELLED | PROXY | NONE
    val lastModified: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
