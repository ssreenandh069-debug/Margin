package com.Margin.app.data

enum class TaskType(val colorType: String) {
    ASSIGNMENT("blue"),
    PRESENTATION("pink"),
    PRACTICAL("yellow"),
    STUDY_GOAL("green"),
    OTHER("gray");

    companion object {
        /** Safe parse — unknown strings → OTHER instead of crashing */
        fun fromString(value: String): TaskType =
            entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) } ?: OTHER
    }
}
