package com.Margin.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.Margin.app.data.local.entity.SubjectEntity
import com.Margin.app.data.local.entity.TimetableEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Handles all JSON serialization/deserialization for timetable export/import.
 *
 * Uses org.json (built into Android — zero new dependencies).
 *
 * JSON format:
 * {
 *   "version": 1,
 *   "exportedAt": "2025-03",
 *   "sessionName": "Semester 1",
 *   "subjects": [
 *     { "code": "CS101", "name": "Data Structures" },
 *     ...
 *   ],
 *   "slots": [
 *     { "subjectCode": "CS101", "day": 1, "time": "09:00 AM" },
 *     ...
 *   ]
 * }
 *
 * Note: UUIDs are intentionally excluded from the export — the importer
 * creates new local UUIDs for every entity so there is no ID collision
 * between devices.
 */
object TimetableExportManager {

    private const val FORMAT_VERSION = 1

    // ── Export ────────────────────────────────────────────────────────────────

    /**
     * Serialises [subjects] and [slots] into a JSON string.
     * [sessionName] is embedded for human-readable context.
     */
    fun toJson(
        sessionName: String,
        subjects: List<SubjectEntity>,
        slots: List<TimetableEntity>
    ): String {
        val root = JSONObject()
        root.put("version",    FORMAT_VERSION)
        root.put("exportedAt", SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()))
        root.put("sessionName", sessionName)

        // Subjects array — only code + name (no IDs, no internal fields)
        val subjectsArr = JSONArray()
        subjects.forEach { s ->
            subjectsArr.put(JSONObject().apply {
                put("code", s.code)
                put("name", s.name)
            })
        }
        root.put("subjects", subjectsArr)

        // Build a lookup map so slots can reference subjects by code
        val idToCode = subjects.associateBy({ it.id }, { it.code })

        val slotsArr = JSONArray()
        slots.forEach { slot ->
            val code = idToCode[slot.subjectId] ?: return@forEach
            slotsArr.put(JSONObject().apply {
                put("subjectCode", code)
                put("day",  slot.dayOfWeek)   // 1=Mon … 7=Sun
                put("time", slot.startTime)
            })
        }
        root.put("slots", slotsArr)

        return root.toString(2)   // Pretty-print with 2-space indent
    }

    /**
     * Writes JSON to a temp file in the app's cache directory and returns
     * a content:// URI suitable for sharing via Intent.ACTION_SEND.
     *
     * The file is named "margin_timetable_<sessionName>.json".
     */
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

    /**
     * Builds a chooser [Intent] that lets the user share the timetable file
     * via any app (WhatsApp, Gmail, Drive, Bluetooth, etc.).
     */
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

    /**
     * Result of a parse attempt.
     * On success, holds the session name and parsed lists ready for DB insertion.
     * On failure, holds a human-readable error message.
     */
    sealed class ParseResult {
        data class Success(
            val sessionName: String,
            val subjects: List<Pair<String, String>>,  // Pair(code, name)
            val slots: List<Triple<String, Int, String>> // Triple(subjectCode, day, time)
        ) : ParseResult()
        data class Failure(val reason: String) : ParseResult()
    }

    /**
     * Parses a JSON [InputStream] (from a file picker result) into a [ParseResult].
     * Does not touch the database — the ViewModel handles the actual insertion.
     */
    fun parseFromStream(stream: InputStream): ParseResult {
        return try {
            val json = stream.bufferedReader(Charsets.UTF_8).readText()
            parseFromString(json)
        } catch (e: Exception) {
            ParseResult.Failure("Could not read file: ${e.message}")
        }
    }

    fun parseFromString(json: String): ParseResult {
        return try {
            val root        = JSONObject(json)
            val version     = root.optInt("version", 0)
            val sessionName = root.optString("sessionName", "Imported Timetable").trim()

            if (version < 1) {
                return ParseResult.Failure("Unrecognised file format (version=$version)")
            }

            // Parse subjects
            val subjectsArr = root.getJSONArray("subjects")
            val subjects    = mutableListOf<Pair<String, String>>()
            for (i in 0 until subjectsArr.length()) {
                val obj  = subjectsArr.getJSONObject(i)
                val code = obj.getString("code").trim()
                val name = obj.getString("name").trim()
                if (code.isNotEmpty() && name.isNotEmpty()) subjects += Pair(code, name)
            }

            // Parse slots
            val slotsArr = root.getJSONArray("slots")
            val slots    = mutableListOf<Triple<String, Int, String>>()
            for (i in 0 until slotsArr.length()) {
                val obj  = slotsArr.getJSONObject(i)
                val code = obj.getString("subjectCode").trim()
                val day  = obj.getInt("day")
                val time = obj.getString("time").trim()
                if (code.isNotEmpty() && day in 1..7 && time.isNotEmpty()) {
                    slots += Triple(code, day, time)
                }
            }

            if (subjects.isEmpty()) {
                return ParseResult.Failure("No subjects found in the file.")
            }

            ParseResult.Success(sessionName, subjects, slots)
        } catch (e: Exception) {
            ParseResult.Failure("Invalid JSON format: ${e.message}")
        }
    }
}
