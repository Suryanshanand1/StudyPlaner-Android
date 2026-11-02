package com.studyplanner.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {
    @Query("SELECT * FROM subjects ORDER BY createdAt ASC")
    fun observeSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM chapters ORDER BY chapter_order ASC")
    fun observeChapters(): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM study_plans ORDER BY date ASC, startTime ASC")
    fun observePlans(): Flow<List<StudyPlanEntity>>

    @Query("SELECT * FROM focus_sessions ORDER BY createdAt DESC")
    fun observeFocusSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM subjects ORDER BY createdAt ASC")
    suspend fun getSubjects(): List<SubjectEntity>

    @Query("SELECT * FROM chapters ORDER BY chapter_order ASC")
    suspend fun getChapters(): List<ChapterEntity>

    @Query("SELECT * FROM study_plans ORDER BY date ASC, startTime ASC")
    suspend fun getPlans(): List<StudyPlanEntity>

    @Query("SELECT * FROM study_plans WHERE id = :id")
    suspend fun getPlan(id: String): StudyPlanEntity?

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getChapter(id: String): ChapterEntity?

    @Query("SELECT * FROM focus_sessions ORDER BY createdAt DESC")
    suspend fun getFocusSessions(): List<FocusSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<SubjectEntity>)

    @Query("UPDATE subjects SET name = :name, color = :color WHERE id = :id")
    suspend fun updateSubject(id: String, name: String, color: String)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubject(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Query("UPDATE chapters SET completed = :completed WHERE id = :id")
    suspend fun setChapterCompleted(id: String, completed: Boolean)

    @Query("UPDATE chapters SET chapter_order = :order WHERE id = :id")
    suspend fun setChapterOrder(id: String, order: Int)

    @Query("DELETE FROM chapters WHERE id = :id")
    suspend fun deleteChapter(id: String)

    @Query("DELETE FROM chapters WHERE subjectId = :subjectId")
    suspend fun deleteChaptersForSubject(subjectId: String)

    @Query("SELECT MAX(chapter_order) FROM chapters WHERE subjectId = :subjectId")
    suspend fun maxChapterOrder(subjectId: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: StudyPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlans(plans: List<StudyPlanEntity>)

    @Query("DELETE FROM study_plans WHERE id = :id")
    suspend fun deletePlan(id: String)

    @Query("DELETE FROM study_plans WHERE subjectId = :subjectId")
    suspend fun deletePlansForSubject(subjectId: String)

    @Query("DELETE FROM study_plans WHERE chapterId = :chapterId")
    suspend fun deletePlansForChapter(chapterId: String)

    @Query("UPDATE study_plans SET confirmed = :confirmed WHERE id = :id")
    suspend fun setPlanConfirmed(id: String, confirmed: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusSession(session: FocusSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusSessions(sessions: List<FocusSessionEntity>)

    @Query("DELETE FROM focus_sessions")
    suspend fun deleteAllFocusSessions()

    @Query("DELETE FROM subjects")
    suspend fun deleteAllSubjects()

    @Query("DELETE FROM chapters")
    suspend fun deleteAllChapters()

    @Query("DELETE FROM study_plans")
    suspend fun deleteAllPlans()
}
