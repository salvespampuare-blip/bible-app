package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ============================================================================
// 1. Database Entities
// ============================================================================

@Entity(tableName = "verses", primaryKeys = ["book", "chapter", "verseNum"])
data class Verse(
    val book: String,
    val chapter: Int,
    val verseNum: Int,
    val text: String,
    val translation: String = "KJV",
    val isPremium: Boolean = false
)

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val book: String,
    val chapter: Int,
    val verseNum: Int,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING" // "PENDING" or "SYNCED"
)

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val book: String,
    val chapter: Int,
    val verseNum: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING" // "PENDING" or "SYNCED"
)

@Entity(tableName = "cross_references")
data class CrossReference(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sourceBook: String,
    val sourceChapter: Int,
    val sourceVerse: Int,
    val targetBook: String,
    val targetChapter: Int,
    val targetVerse: Int,
    val description: String = "Parallel Theme"
)

@Entity(tableName = "historical_commentaries")
data class HistoricalCommentary(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val book: String,
    val chapter: Int,
    val verseNum: Int,
    val author: String, // e.g., "Matthew Henry", "Augustine", "Aquinas", "Gemini AI"
    val text: String,
    val isAiGenerated: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reading_plans")
data class ReadingPlan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val totalDays: Int,
    val currentDay: Int = 1,
    val completedDaysCsv: String = "" // Comma-separated list of completed days: e.g., "1,2"
)

// ============================================================================
// 2. Data Access Objects (DAOs)
// ============================================================================

@Dao
interface VerseDao {
    @Query("SELECT * FROM verses ORDER BY book, chapter, verseNum")
    fun getAllVerses(): Flow<List<Verse>>

    @Query("SELECT * FROM verses WHERE book = :book ORDER BY chapter, verseNum")
    fun getVersesByBook(book: String): Flow<List<Verse>>

    @Query("SELECT * FROM verses WHERE book = :book AND chapter = :chapter ORDER BY verseNum")
    fun getVersesByChapter(book: String, chapter: Int): Flow<List<Verse>>

    @Query("SELECT * FROM verses WHERE book = :book AND chapter = :chapter AND verseNum = :verseNum LIMIT 1")
    suspend fun getVerse(book: String, chapter: Int, verseNum: Int): Verse?

    @Query("SELECT DISTINCT book FROM verses")
    fun getAvailableBooks(): Flow<List<String>>

    @Query("SELECT DISTINCT chapter FROM verses WHERE book = :book ORDER BY chapter")
    fun getChaptersForBook(book: String): Flow<List<Int>>

    @Query("SELECT * FROM verses WHERE text LIKE '%' || :query || '%'")
    fun searchVerses(query: String): Flow<List<Verse>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerse(verse: Verse)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerses(verses: List<Verse>)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE book = :book AND chapter = :chapter AND verseNum = :verseNum ORDER BY timestamp DESC")
    fun getNotesForVerse(book: String, chapter: Int, verseNum: Int): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Int)

    @Query("SELECT * FROM notes WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncNotes(): List<Note>

    @Query("UPDATE notes SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markNotesAsSynced(ids: List<Int>)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<Bookmark>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE book = :book AND chapter = :chapter AND verseNum = :verseNum)")
    fun isBookmarked(book: String, chapter: Int, verseNum: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addBookmark(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE book = :book AND chapter = :chapter AND verseNum = :verseNum")
    suspend fun removeBookmark(book: String, chapter: Int, verseNum: Int)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Int)

    @Query("SELECT * FROM bookmarks WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncBookmarks(): List<Bookmark>

    @Query("UPDATE bookmarks SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markBookmarksAsSynced(ids: List<Int>)
}

@Dao
interface CrossReferenceDao {
    @Query("SELECT * FROM cross_references WHERE sourceBook = :book AND sourceChapter = :chapter AND sourceVerse = :verseNum")
    fun getCrossReferencesForVerse(book: String, chapter: Int, verseNum: Int): Flow<List<CrossReference>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossReference(reference: CrossReference)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossReferences(references: List<CrossReference>)
}

@Dao
interface HistoricalCommentaryDao {
    @Query("SELECT * FROM historical_commentaries WHERE book = :book AND chapter = :chapter AND verseNum = :verseNum")
    fun getCommentariesForVerse(book: String, chapter: Int, verseNum: Int): Flow<List<HistoricalCommentary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommentary(commentary: HistoricalCommentary)
}

@Dao
interface ReadingPlanDao {
    @Query("SELECT * FROM reading_plans")
    fun getReadingPlans(): Flow<List<ReadingPlan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadingPlan(plan: ReadingPlan)

    @Update
    suspend fun updateReadingPlan(plan: ReadingPlan)
}

// ============================================================================
// 3. Main Database Class
// ============================================================================

@Database(
    entities = [
        Verse::class,
        Note::class,
        Bookmark::class,
        CrossReference::class,
        HistoricalCommentary::class,
        ReadingPlan::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BibleDatabase : RoomDatabase() {
    abstract fun verseDao(): VerseDao
    abstract fun noteDao(): NoteDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun crossReferenceDao(): CrossReferenceDao
    abstract fun historicalCommentaryDao(): HistoricalCommentaryDao
    abstract fun readingPlanDao(): ReadingPlanDao
}
