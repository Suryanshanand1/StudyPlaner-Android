package com.studyplanner.app.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.roundToInt

class StudyRepository(
    private val db: StudyDatabase,
    @Suppress("unused") private val context: Context,
) {
    private val dao = db.dao()

    val subjects: Flow<List<SubjectEntity>> = dao.observeSubjects()
    val chapters: Flow<List<ChapterEntity>> = dao.observeChapters()
    val plans: Flow<List<StudyPlanEntity>> = dao.observePlans()
    val focusSessions: Flow<List<FocusSessionEntity>> = dao.observeFocusSessions()

    suspend fun addSubject(name: String, color: String): String {
        val id = UUID.randomUUID().toString()
        dao.insertSubject(
            SubjectEntity(
                id = id,
                name = name.trim(),
                color = color,
                createdAt = System.currentTimeMillis(),
            ),
        )
        return id
    }

    suspend fun editSubject(id: String, name: String, color: String) {
        dao.updateSubject(id, name.trim(), color)
    }

    suspend fun deleteSubject(id: String) {
        db.withTransaction {
            dao.deletePlansForSubject(id)
            dao.deleteChaptersForSubject(id)
            dao.deleteSubject(id)
        }
    }

    suspend fun addChapter(subjectId: String, name: String): String {
        val id = UUID.randomUUID().toString()
        val nextOrder = (dao.maxChapterOrder(subjectId) ?: -1) + 1
        dao.insertChapter(
            ChapterEntity(
                id = id,
                subjectId = subjectId,
                name = name.trim(),
                completed = false,
                order = nextOrder,
                createdAt = System.currentTimeMillis(),
            ),
        )
        return id
    }

    suspend fun toggleChapter(id: String) {
        val chapter = dao.getChapter(id) ?: return
        dao.setChapterCompleted(id, !chapter.completed)
    }

    suspend fun deleteChapter(id: String) {
        db.withTransaction {
            dao.deletePlansForChapter(id)
            dao.deleteChapter(id)
        }
    }

    suspend fun moveChapter(chapterId: String, subjectId: String, direction: Int) {
        db.withTransaction {
            val siblings = dao.getChapters()
                .filter { it.subjectId == subjectId }
                .sortedBy { it.order }
                .toMutableList()
            val index = siblings.indexOfFirst { it.id == chapterId }
            if (index < 0) return@withTransaction
            val target = (index + direction).coerceIn(0, siblings.lastIndex)
            if (target == index) return@withTransaction
            val moved = siblings.removeAt(index)
            siblings.add(target, moved)
            siblings.forEachIndexed { i, chapter -> dao.setChapterOrder(chapter.id, i) }
        }
    }

    suspend fun addPlan(
        subjectId: String,
        chapterId: String,
        subjectName: String,
        chapterName: String,
        date: String,
        startTime: String,
        endTime: String,
        confirmed: Boolean = false,
        source: String = "planner",
    ): String {
        val id = UUID.randomUUID().toString()
        dao.insertPlan(
            StudyPlanEntity(
                id = id,
                subjectId = subjectId,
                chapterId = chapterId,
                subjectName = subjectName,
                chapterName = chapterName,
                date = date,
                startTime = startTime,
                endTime = endTime,
                createdAt = System.currentTimeMillis(),
                confirmed = confirmed,
                source = source,
            ),
        )
        return id
    }

    suspend fun deletePlan(id: String) = dao.deletePlan(id)

    suspend fun togglePlanConfirmed(id: String) {
        val plan = dao.getPlan(id) ?: return
        dao.setPlanConfirmed(id, !plan.confirmed)
    }

    suspend fun addFocusSession(
        label: String,
        date: String,
        startTime: String,
        endTime: String,
        minutes: Int,
    ) {
        dao.insertFocusSession(
            FocusSessionEntity(
                id = UUID.randomUUID().toString(),
                label = label,
                date = date,
                startTime = startTime,
                endTime = endTime,
                minutes = minutes,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun getPlansOnce(): List<StudyPlanEntity> = dao.getPlans()

    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        root.put("subjects", JSONArray().apply {
            dao.getSubjects().forEach { subject ->
                put(JSONObject().apply {
                    put("id", subject.id)
                    put("name", subject.name)
                    put("color", subject.color)
                    put("createdAt", subject.createdAt)
                })
            }
        })

        root.put("chapters", JSONArray().apply {
            dao.getChapters().forEach { chapter ->
                put(JSONObject().apply {
                    put("id", chapter.id)
                    put("subjectId", chapter.subjectId)
                    put("name", chapter.name)
                    put("completed", chapter.completed)
                    put("order", chapter.order)
                    put("createdAt", chapter.createdAt)
                })
            }
        })

        root.put("studyPlans", JSONArray().apply {
            dao.getPlans().forEach { plan ->
                put(JSONObject().apply {
                    put("id", plan.id)
                    put("subjectId", plan.subjectId)
                    put("chapterId", plan.chapterId)
                    put("subjectName", plan.subjectName)
                    put("chapterName", plan.chapterName)
                    put("date", plan.date)
                    put("startTime", plan.startTime)
                    put("endTime", plan.endTime)
                    put("createdAt", plan.createdAt)
                    put("confirmed", plan.confirmed)
                    put("source", plan.source)
                })
            }
        })

        root.put("focusSessions", JSONArray().apply {
            dao.getFocusSessions().forEach { session ->
                put(JSONObject().apply {
                    put("id", session.id)
                    put("label", session.label)
                    put("date", session.date)
                    put("startTime", session.startTime)
                    put("endTime", session.endTime)
                    put("minutes", session.minutes)
                    put("createdAt", session.createdAt)
                })
            }
        })

        root.toString(2)
    }

    /**
     * Imports both native backups and backups exported by the original web app.
     * Unknown keys are ignored; malformed entries receive safe defaults.
     */
    suspend fun importJson(text: String): Boolean = withContext(Dispatchers.IO) {
        val root = runCatching { JSONObject(text) }.getOrNull() ?: return@withContext false
        val subjectsJson = root.optJSONArray("subjects") ?: return@withContext false
        val chaptersJson = root.optJSONArray("chapters") ?: return@withContext false
        val plansJson = root.optJSONArray("studyPlans") ?: return@withContext false
        val focusJson = root.optJSONArray("focusSessions") ?: JSONArray()
        val now = System.currentTimeMillis()

        val subjects = (0 until subjectsJson.length()).mapNotNull { i ->
            subjectsJson.optJSONObject(i)?.let { json ->
                SubjectEntity(
                    id = json.optString("id").ifBlank { UUID.randomUUID().toString() },
                    name = json.optString("name"),
                    color = json.optString("color").ifBlank { "#3B82F6" },
                    createdAt = json.optLong("createdAt", now),
                )
            }
        }
        val chapters = (0 until chaptersJson.length()).mapNotNull { i ->
            chaptersJson.optJSONObject(i)?.let { json ->
                ChapterEntity(
                    id = json.optString("id").ifBlank { UUID.randomUUID().toString() },
                    subjectId = json.optString("subjectId"),
                    name = json.optString("name"),
                    completed = json.optBoolean("completed", false),
                    order = json.optInt("order", i),
                    createdAt = json.optLong("createdAt", now),
                )
            }
        }
        val plans = (0 until plansJson.length()).mapNotNull { i ->
            plansJson.optJSONObject(i)?.let { json ->
                StudyPlanEntity(
                    id = json.optString("id").ifBlank { UUID.randomUUID().toString() },
                    subjectId = json.optString("subjectId"),
                    chapterId = json.optString("chapterId"),
                    subjectName = json.optString("subjectName"),
                    chapterName = json.optString("chapterName"),
                    date = json.optString("date"),
                    startTime = json.optString("startTime"),
                    endTime = json.optString("endTime"),
                    createdAt = json.optLong("createdAt", now),
                    confirmed = json.optBoolean("confirmed", false),
                    source = json.optString("source").ifBlank { "planner" },
                )
            }
        }
        val focusSessions = (0 until focusJson.length()).mapNotNull { i ->
            focusJson.optJSONObject(i)?.let { json ->
                FocusSessionEntity(
                    id = json.optString("id").ifBlank { UUID.randomUUID().toString() },
                    label = json.optString("label"),
                    date = json.optString("date"),
                    startTime = json.optString("startTime"),
                    endTime = json.optString("endTime"),
                    minutes = json.optInt("minutes", 0),
                    createdAt = json.optLong("createdAt", now),
                )
            }
        }

        db.withTransaction {
            dao.deleteAllPlans()
            dao.deleteAllChapters()
            dao.deleteAllSubjects()
            dao.deleteAllFocusSessions()
            if (subjects.isNotEmpty()) dao.insertSubjects(subjects)
            if (chapters.isNotEmpty()) dao.insertChapters(chapters)
            if (plans.isNotEmpty()) dao.insertPlans(plans)
            if (focusSessions.isNotEmpty()) dao.insertFocusSessions(focusSessions)
        }
        true
    }

    suspend fun clearAll() {
        db.withTransaction {
            dao.deleteAllPlans()
            dao.deleteAllChapters()
            dao.deleteAllSubjects()
            dao.deleteAllFocusSessions()
        }
    }

    fun minutesFor(ms: Long): Int = (ms / 60_000.0).roundToInt().coerceAtLeast(0)
}
