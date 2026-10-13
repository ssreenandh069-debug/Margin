package com.Margin.app.utils

import android.util.Base64
import com.Margin.app.data.local.entity.SubjectEntity
import com.Margin.app.data.local.entity.TimetableEntity
import com.Margin.app.domain.AttendanceMath
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Handles Squad Code serialization and deserialization for timetable sharing.
 * Completely peer-to-peer, serverless, and fast.
 */
object TimetableExportManager {

    private const val FORMAT_VERSION = 1
    private const val SQUAD_PREFIX = "MARGIN:"

    // ── Export / Generation ───────────────────────────────────────────────────

    fun toJson(
        sessionName: String,
        subjects: List<SubjectEntity>,
        slots: List<TimetableEntity>
    ): String {
        val root = JSONObject()
        root.put("version", FORMAT_VERSION)
        root.put("exportedAt", SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()))
        root.put("sessionName", sessionName)

        val subjectsArr = JSONArray()
        subjects.forEach { s ->
            subjectsArr.put(JSONObject().apply {
                put("code", s.code)
                put("name", s.name)
            })
        }
        root.put("subjects", subjectsArr)

        val idToCode = subjects.associateBy({ it.id }, { it.code })

        // Sort slots strictly by dayOfWeek ASC, then by parsed time minutes ASC
        val sortedSlots = slots.sortedWith(
            compareBy<TimetableEntity> { it.dayOfWeek }
                .thenBy { AttendanceMath.parseTimeToMinutes(it.startTime) }
        )

        val slotsArr = JSONArray()
        sortedSlots.forEach { slot ->
            val code = idToCode[slot.subjectId] ?: return@forEach
            slotsArr.put(JSONObject().apply {
                put("subjectCode", code)
                put("day", slot.dayOfWeek)
                put("time", slot.startTime)
                if (slot.room.isNotBlank()) put("room", slot.room)
            })
        }
        root.put("slots", slotsArr)

        return root.toString()
    }

    /**
     * Encodes the timetable into a compact, 1-tap shareable Squad Code string.
     */
    fun encodeToSquadCode(
        sessionName: String,
        subjects: List<SubjectEntity>,
        slots: List<TimetableEntity>
    ): String {
        val json = toJson(sessionName, subjects, slots)
        val encoded = Base64.encodeToString(json.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return "$SQUAD_PREFIX$encoded"
    }

    // ── Import / Decoding ────────────────────────────────────────────────────

    data class ImportedSlot(
        val subjectCode: String,
        val day: Int,
        val time: String,
        val room: String = ""
    )

    sealed class ParseResult {
        data class Success(
            val sessionName: String,
            val subjects: List<Pair<String, String>>,
            val slots: List<ImportedSlot>
        ) : ParseResult()
        data class Failure(val reason: String) : ParseResult()
    }

    /**
     * Parses a Squad Code ("MARGIN:...") or fallback JSON string.
     * Resilient against surrounding text copied from messenger apps (WhatsApp/Telegram/Discord).
     */
    fun parseFromString(rawInput: String): ParseResult {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return ParseResult.Failure("Squad Code cannot be empty.")
        }

        val json = try {
            if (trimmed.contains(SQUAD_PREFIX)) {
                val afterPrefix = trimmed.substringAfter(SQUAD_PREFIX).trim()
                val base64Data = afterPrefix.replace(Regex("""\s+"""), "")
                try {
                    String(Base64.decode(base64Data, Base64.DEFAULT), Charsets.UTF_8)
                } catch (e: IllegalArgumentException) {
                    return ParseResult.Failure("Malformed Squad Code (bad Base64 encoding). Ensure the entire code was copied.")
                }
            } else {
                trimmed
            }
        } catch (e: Exception) {
            return ParseResult.Failure("Could not decode Squad Code: ${e.message}")
        }

        return try {
            val root = JSONObject(json)
            val version = root.optInt("version", 0)
            val sessionName = root.optString("sessionName", "Imported Timetable").trim()

            if (version < 1) {
                return ParseResult.Failure("Incompatible Squad Code format (version $version).")
            }

            val subjectsArr = root.optJSONArray("subjects")
                ?: return ParseResult.Failure("Invalid Squad Code: missing subjects list.")
            val subjects = mutableListOf<Pair<String, String>>()
            for (i in 0 until subjectsArr.length()) {
                val obj = subjectsArr.getJSONObject(i)
                val code = obj.optString("code", "").trim()
                val name = obj.optString("name", "").trim()
                if (code.isNotEmpty() && name.isNotEmpty()) subjects += Pair(code, name)
            }

            if (subjects.isEmpty()) {
                return ParseResult.Failure("No valid subjects found in Squad Code.")
            }

            val slotsArr = root.optJSONArray("slots")
            val slots = mutableListOf<ImportedSlot>()
            if (slotsArr != null) {
                for (i in 0 until slotsArr.length()) {
                    val obj = slotsArr.getJSONObject(i)
                    val code = obj.optString("subjectCode", "").trim()
                    val day = obj.optInt("day", -1)
                    val time = obj.optString("time", "").trim()
                    val room = obj.optString("room", "").trim()
                    if (code.isNotEmpty() && day in 1..7 && time.isNotEmpty()) {
                        slots += ImportedSlot(code, day, time, room)
                    }
                }
            }

            // Guarantee chronological order for each day upon import
            val sortedSlots = slots.sortedWith(
                compareBy<ImportedSlot> { it.day }
                    .thenBy { AttendanceMath.parseTimeToMinutes(it.time) }
            )

            ParseResult.Success(sessionName, subjects, sortedSlots)
        } catch (e: Exception) {
            ParseResult.Failure("Invalid or corrupted Squad Code: ${e.message}")
        }
    }
}
