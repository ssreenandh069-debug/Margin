package com.Margin.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "monthly_summaries",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("subjectId")]
)
data class MonthlySummaryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val subjectId: String,
    val monthYearText: String,   // "2025-03"
    val attendedCount: Int,      // PRESENT + PROXY
    val totalCount: Int,         // PRESENT + PROXY + BUNK + EXCUSED
    val bunkCount: Int,
    val proxyCount: Int = 0      // How many of attendedCount were PROXY (v3+)
)
