package com.Margin.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val startDate: Long,
    val isActive: Boolean = false,
    val isCompleted: Boolean = false
)

@Entity(
    tableName = "subjects",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class SubjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val name: String,
    val code: String
)

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
    val subjectId: String,
    val dayOfWeek: Int, // 1=Mon … 7=Sun
    val startTime: String,
    val room: String = ""
)

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
    indices = [Index("subjectId"), Index("date"), Index("slotId")]
)
data class AttendanceRecordEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val subjectId: String,
    val date: Long,
    val slotId: String? = null,
    val status: String = "NONE", // PRESENT | BUNK | EXCUSED | CANCELLED | PROXY | DUTY | DUTY_ABSENT | NONE
    val note: String = ""
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId"), Index("type"), Index("dueDate")]
)
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val subjectId: String,
    val title: String,
    val type: String, // ASSIGNMENT | PRESENTATION | PRACTICAL | STUDY_GOAL | OTHER
    val dueDate: Long,
    val isCompleted: Boolean = false,
    val hasReminder: Boolean = false
)

enum class TaskType(val displayName: String) {
    ASSIGNMENT("Assignment"),
    PRESENTATION("Presentation"),
    PRACTICAL("Practical File"),
    STUDY_GOAL("Study Goal"),
    OTHER("Other");

    companion object {
        fun fromString(value: String): TaskType =
            entries.firstOrNull { it.name.equals(value.trim().replace(' ', '_'), ignoreCase = true) } ?: OTHER
    }
}
