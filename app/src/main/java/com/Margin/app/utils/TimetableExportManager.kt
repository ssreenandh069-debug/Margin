package com.Margin.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import com.Margin.app.data.local.entity.SubjectEntity
import com.Margin.app.data.local.entity.TimetableEntity
import com.Margin.app.domain.AttendanceMath
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Handles JSON and Squad Code serialization for timetable export/import.
 */
object TimetableExportManager {

    private const val FORMAT_VERSION = 1

    // ── Export ────────────────────────────────────────────────────────────────

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

        return root.toString(2)
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
        return "MARGIN:$encoded"
    }

    fun writeToShareableFile(context: Context, json: String, sessionName: String): Uri {
        val safeFileName = "margin_timetable_${sessionName.replace(" ", "_")}.json"
        val file = File(context.cacheDir, safeFileName)
        file.writeText(json, Charsets.UTF_8)

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun buildShareIntent(uri: Uri, sessionName: String): Intent {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Timetable — $sessionName")
            putExtra(Intent.EXTRA_TEXT, "Here's my semester timetable exported from Margin!")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(shareIntent, "Share Timetable via…")
    }

    // ── Import ────────────────────────────────────────────────────────────────

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

    fun parseFromStream(stream: InputStream): ParseResult {
        return try {
            val json = stream.bufferedReader(Charsets.UTF_8).readText()
            parseFromString(json)
        } catch (e: Exception) {
            ParseResult.Failure("Could not read file: ${e.message}")
        }
    }

    /**
     * Parses from either a Squad Code ("MARGIN:...") or raw JSON.
     * Resilient against surrounding text copied from messenger apps.
     */
    fun parseFromString(rawInput: String): ParseResult {
        return try {
            val trimmed = rawInput.trim()
            val json = if (trimmed.contains("MARGIN:")) {
                val afterPrefix = trimmed.substringAfter("MARGIN:").trim()
                val base64Data = afterPrefix.replace(Regex("""\s+"""), "")
                String(Base64.decode(base64Data, Base64.DEFAULT), Charsets.UTF_8)
            } else {
                trimmed
            }
            val root = JSONObject(json)
            val version = root.optInt("version", 0)
            val sessionName = root.optString("sessionName", "Imported Timetable").trim()

            if (version < 1) {
                return ParseResult.Failure("Unrecognised format (version=$version)")
            }

            val subjectsArr = root.getJSONArray("subjects")
            val subjects = mutableListOf<Pair<String, String>>()
            for (i in 0 until subjectsArr.length()) {
                val obj = subjectsArr.getJSONObject(i)
                val code = obj.getString("code").trim()
                val name = obj.getString("name").trim()
                if (code.isNotEmpty() && name.isNotEmpty()) subjects += Pair(code, name)
            }

            val slotsArr = root.getJSONArray("slots")
            val slots = mutableListOf<ImportedSlot>()
            for (i in 0 until slotsArr.length()) {
                val obj = slotsArr.getJSONObject(i)
                val code = obj.getString("subjectCode").trim()
                val day = obj.getInt("day")
                val time = obj.getString("time").trim()
                val room = obj.optString("room", "").trim()
                if (code.isNotEmpty() && day in 1..7 && time.isNotEmpty()) {
                    slots += ImportedSlot(code, day, time, room)
                }
            }

            // Guarantee chronological order for each day upon import
            val sortedSlots = slots.sortedWith(
                compareBy<ImportedSlot> { it.day }
                    .thenBy { AttendanceMath.parseTimeToMinutes(it.time) }
            )

            if (subjects.isEmpty()) {
                return ParseResult.Failure("No subjects found in timetable.")
            }

            ParseResult.Success(sessionName, subjects, sortedSlots)
        } catch (e: Exception) {
            ParseResult.Failure("Invalid format: ${e.message}")
        }
    }
}
