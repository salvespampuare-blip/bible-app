package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import com.example.service.GeminiService
import com.example.service.SemanticVerseResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BibleRepository(private val db: BibleDatabase) {

    // DAOs
    private val verseDao = db.verseDao()
    private val noteDao = db.noteDao()
    private val bookmarkDao = db.bookmarkDao()
    private val crossReferenceDao = db.crossReferenceDao()
    private val commentaryDao = db.historicalCommentaryDao()
    private val readingPlanDao = db.readingPlanDao()

    // Flow Getters
    val availableBooks: Flow<List<String>> = verseDao.getAvailableBooks()
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()
    val allBookmarks: Flow<List<Bookmark>> = bookmarkDao.getAllBookmarks()
    val readingPlans: Flow<List<ReadingPlan>> = readingPlanDao.getReadingPlans()

    fun getChaptersForBook(book: String): Flow<List<Int>> = verseDao.getChaptersForBook(book)
    fun getVersesByChapter(book: String, chapter: Int): Flow<List<Verse>> = verseDao.getVersesByChapter(book, chapter)
    fun isBookmarked(book: String, chapter: Int, verseNum: Int): Flow<Boolean> = bookmarkDao.isBookmarked(book, chapter, verseNum)
    fun getCrossReferences(book: String, chapter: Int, verseNum: Int): Flow<List<CrossReference>> = crossReferenceDao.getCrossReferencesForVerse(book, chapter, verseNum)
    fun getCommentaries(book: String, chapter: Int, verseNum: Int): Flow<List<HistoricalCommentary>> = commentaryDao.getCommentariesForVerse(book, chapter, verseNum)
    fun searchVersesLocal(query: String): Flow<List<Verse>> = verseDao.searchVerses(query)

    // Suspend operations
    suspend fun getVerse(book: String, chapter: Int, verseNum: Int): Verse? = verseDao.getVerse(book, chapter, verseNum)
    suspend fun addNote(book: String, chapter: Int, verseNum: Int, content: String) {
        noteDao.insertNote(Note(book = book, chapter = chapter, verseNum = verseNum, content = content))
    }
    suspend fun deleteNote(id: Int) = noteDao.deleteNoteById(id)

    suspend fun toggleBookmark(book: String, chapter: Int, verseNum: Int) {
        val already = bookmarkDao.isBookmarked(book, chapter, verseNum).firstOrNull() ?: false
        if (already) {
            bookmarkDao.removeBookmark(book, chapter, verseNum)
        } else {
            bookmarkDao.addBookmark(Bookmark(book = book, chapter = chapter, verseNum = verseNum))
        }
    }
    suspend fun removeBookmarkById(id: Int) = bookmarkDao.deleteBookmarkById(id)

    suspend fun addCommentary(commentary: HistoricalCommentary) = commentaryDao.insertCommentary(commentary)
    suspend fun updateReadingPlan(plan: ReadingPlan) = readingPlanDao.updateReadingPlan(plan)

    /**
     * Seeds the Bible database with beautiful foundational verses if it's currently empty,
     * ensuring instant offline access of world-class scriptures for all users out of the box.
     */
    suspend fun seedDatabaseIfEmpty() = withContext(Dispatchers.IO) {
        val existing = verseDao.getAvailableBooks().firstOrNull() ?: emptyList()
        if (existing.isNotEmpty()) return@withContext // Already seeded

        val seededVerses = mutableListOf<Verse>()

        // 1. Genesis 1:1-5
        seededVerses.add(Verse("Genesis", 1, 1, "In the beginning God created the heaven and the earth."))
        seededVerses.add(Verse("Genesis", 1, 2, "And the earth was without form, and void; and darkness was upon the face of the deep. And the Spirit of God moved upon the face of the waters."))
        seededVerses.add(Verse("Genesis", 1, 3, "And God said, Let there be light: and there was light."))
        seededVerses.add(Verse("Genesis", 1, 4, "And God saw the light, that it was good: and God divided the light from the darkness."))
        seededVerses.add(Verse("Genesis", 1, 5, "And God called the light Day, and the darkness he called Night. And the evening and the morning were the first day."))

        // 2. Psalm 23:1-6
        seededVerses.add(Verse("Psalms", 23, 1, "The LORD is my shepherd; I shall not want."))
        seededVerses.add(Verse("Psalms", 23, 2, "He maketh me to lie down in green pastures: he leadeth me beside the still waters."))
        seededVerses.add(Verse("Psalms", 23, 3, "He restoreth my soul: he leadeth me in the paths of righteousness for his name's sake."))
        seededVerses.add(Verse("Psalms", 23, 4, "Yea, though I walk through the valley of the shadow of death, I will fear no evil: for thou art with me; thy rod and thy staff they comfort me."))
        seededVerses.add(Verse("Psalms", 23, 5, "Thou preparest a table before me in the presence of mine enemies: thou anointest my head with oil; my cup runneth over."))
        seededVerses.add(Verse("Psalms", 23, 6, "Surely goodness and mercy shall follow me all the days of my life: and I will dwell in the house of the LORD for ever."))

        // 3. Psalm 46:1-3
        seededVerses.add(Verse("Psalms", 46, 1, "God is our refuge and strength, a very present help in trouble."))
        seededVerses.add(Verse("Psalms", 46, 2, "Therefore will not we fear, though the earth be removed, and though the mountains be carried into the midst of the sea;"))
        seededVerses.add(Verse("Psalms", 46, 3, "Though the waters thereof roar and be troubled, though the mountains shake with the swelling thereof. Selah."))

        // 4. Proverbs 3:5-6
        seededVerses.add(Verse("Proverbs", 3, 5, "Trust in the LORD with all thine heart; and lean not unto thine own understanding."))
        seededVerses.add(Verse("Proverbs", 3, 6, "In all thy ways acknowledge him, and he shall direct thy paths."))

        // 5. Isaiah 40:29-31
        seededVerses.add(Verse("Isaiah", 40, 29, "He giveth power to the faint; and to them that have no might he increaseth strength."))
        seededVerses.add(Verse("Isaiah", 40, 30, "Even the youths shall faint and be weary, and the young men shall utterly fall:"))
        seededVerses.add(Verse("Isaiah", 40, 31, "But they that wait upon the LORD shall renew their strength; they shall mount up with wings as eagles; they shall run, and not be weary; and they shall walk, and not faint."))

        // 6. John 1:1-5
        seededVerses.add(Verse("John", 1, 1, "In the beginning was the Word, and the Word was with God, and the Word was God."))
        seededVerses.add(Verse("John", 1, 2, "The same was in the beginning with God."))
        seededVerses.add(Verse("John", 1, 3, "All things were made by him; and without him was not any thing made that was made."))
        seededVerses.add(Verse("John", 1, 4, "In him was life; and the life was the light of men."))
        seededVerses.add(Verse("John", 1, 5, "And the light shineth in darkness; and the darkness comprehended it not."))

        // 7. John 3:16 (Premium Content Marker Demo)
        seededVerses.add(Verse("John", 3, 16, "For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish, but have everlasting life.", isPremium = false))

        // 8. John 14:6
        seededVerses.add(Verse("John", 14, 6, "Jesus saith unto him, I am the way, the truth, and the life: no man cometh unto the Father, but by me."))

        // 9. Romans 8:28
        seededVerses.add(Verse("Romans", 8, 28, "And we know that all things work together for good to them that love God, to them who are the called according to his purpose."))

        // 10. Romans 8:37-39 (Scholar/Premium exclusive content demo)
        seededVerses.add(Verse("Romans", 8, 37, "Nay, in all these things we are more than conquerors through him that loved us.", isPremium = true))
        seededVerses.add(Verse("Romans", 8, 38, "For I am persuaded, that neither death, nor life, nor angels, nor principalities, nor powers, nor things present, nor things to come,", isPremium = true))
        seededVerses.add(Verse("Romans", 8, 39, "Nor height, nor depth, nor any other creature, shall be able to separate us from the love of God, which is in Christ Jesus our Lord.", isPremium = true))

        // 11. Philippians 4:6-7
        seededVerses.add(Verse("Philippians", 4, 6, "Be careful for nothing; but in every thing by prayer and supplication with thanksgiving let your requests be made known unto God."))
        seededVerses.add(Verse("Philippians", 4, 7, "And the peace of God, which passeth all understanding, shall keep your hearts and minds through Christ Jesus."))

        // 12. Philippians 4:13
        seededVerses.add(Verse("Philippians", 4, 13, "I can do all things through Christ which strengtheneth me."))

        // 13. Hebrews 11:1
        seededVerses.add(Verse("Hebrews", 11, 1, "Now faith is the substance of things hoped for, the evidence of things not seen."))

        verseDao.insertVerses(seededVerses)

        // Seed Core Cross-References
        val seededRefs = listOf(
            CrossReference(sourceBook = "John", sourceChapter = 1, sourceVerse = 1, targetBook = "Genesis", targetChapter = 1, targetVerse = 1, description = "Both declare the co-eternal origin of God and creation."),
            CrossReference(sourceBook = "Psalms", sourceChapter = 23, sourceVerse = 1, targetBook = "John", targetChapter = 10, targetVerse = 11, description = "The Lord is shepherd matching Jesus Christ's Good Shepherd declaration."),
            CrossReference(sourceBook = "Proverbs", sourceChapter = 3, sourceVerse = 5, targetBook = "Romans", targetChapter = 12, targetVerse = 16, description = "Instructions warning against relying on your own intellect/wisdom."),
            CrossReference(sourceBook = "Philippians", sourceChapter = 4, sourceVerse = 6, targetBook = "Psalms", targetChapter = 46, targetVerse = 1, description = "Resting in God's peace as a refuge amidst world anxiety.")
        )
        crossReferenceDao.insertCrossReferences(seededRefs)

        // Seed Matthew Henry Commentary
        commentaryDao.insertCommentary(HistoricalCommentary(
            book = "Genesis", chapter = 1, verseNum = 1,
            author = "Matthew Henry",
            text = "The first verse of the Bible gives us a satisfying and useful account of the origin of the visible world. It shows that God is the Creator of all things, infinite in power, wisdom, and goodness, and that creation is the work of His sovereignty."
        ))
        commentaryDao.insertCommentary(HistoricalCommentary(
            book = "Psalms", chapter = 23, verseNum = 1,
            author = "Matthew Henry",
            text = "The psalmist here claims God as his shepherd, depicting supreme peace, assurance, and confidence. Under His pasture guard we shall never want. God provides food for the body and sanctuary for the everlasting soul."
        ))
        commentaryDao.insertCommentary(HistoricalCommentary(
            book = "John", chapter = 1, verseNum = 1,
            author = "Matthew Henry",
            text = "The Word is personified as co-eternal with the Father, God of God, Light of Light, establishing the absolute divinity of Christ. This teaches us that Christ was active in creation and is Himself the supreme light of men."
        ))
        commentaryDao.insertCommentary(HistoricalCommentary(
            book = "Philippians", chapter = 4, verseNum = 6,
            author = "Matthew Henry",
            text = "Anxiety is the great enemy of our peace. The apostle prescribes a double cure: first, prayer to lay open our griefs; second, thanksgiving to acknowledge His goodness, which cures anxious worry and preserves the soul."
        ))

        // Seed 3 Customizable Reading Plans
        readingPlanDao.insertReadingPlan(ReadingPlan(
            title = "New Testament Foundations",
            description = "A customizable route covering key gospels and letters to establish an understanding of Christ's teaching.",
            totalDays = 30,
            currentDay = 1,
            completedDaysCsv = ""
        ))
        readingPlanDao.insertReadingPlan(ReadingPlan(
            title = "Psalms of Refuge (Selah)",
            description = "Meditate on songs of comfort, reflection, and quiet sanctuary. Perfect for night reading.",
            totalDays = 14,
            currentDay = 1,
            completedDaysCsv = ""
        ))
        readingPlanDao.insertReadingPlan(ReadingPlan(
            title = "Wisdom Journey",
            description = "Daily study in Proverbs and Ecclesiastes to gather discernment and strength for regular decision-making.",
            totalDays = 21,
            currentDay = 1,
            completedDaysCsv = ""
        ))
    }

    /**
     * Executes Semantic Search using Gemini API
     * Returns a parsed list of matching scriptures and caches them locally so they are henceforth offline available!
     */
    suspend fun semanticSearch(queryText: String): List<SemanticVerseResult> = withContext(Dispatchers.IO) {
        val systemPrompt = """
            You are a scholarly Bible search engine. Analyze semantic user queries representing themes, worries, or historical ideas.
            Search through the Old and New Testaments. Find the top 3-5 most relevant, inspiring, and direct verses for that query.
            You must return strictly a JSON array of objects with the precise keys:
            - "book": String (full standard book name, capitalized, e.g. "Romans")
            - "chapter": Int (chapter number)
            - "verseNum": Int (verse number)
            - "text": String (the full verse content in clean KJV translation)
            - "relevanceReason": String (a short explanatory sentence of how this verse matches the user's semantic topic)
            
            Strictly return only the JSON array. Do not wrap in markdown ```json or block fences. Output pristine, valid JSON.
        """.trimIndent()

        val prompt = "Find verses relevant to this study theme: \"$queryText\""

        try {
            val jsonResult = GeminiService.queryGemini(prompt, systemPrompt, isJson = true)
            val parsed = GeminiService.parseSemanticSearchResults(jsonResult)

            // Cache verses locally so they can be viewed offline!
            parsed.forEach { item ->
                verseDao.insertVerse(Verse(
                    book = item.book,
                    chapter = item.chapter,
                    verseNum = item.verseNum,
                    text = item.text
                ))
            }
            return@withContext parsed
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    /**
     * Request a customizable chapter from Gemini if not pre-seeded, making the entire Bible available.
     * Caches immediately in local Room database.
     */
    suspend fun fetchAndCacheChapter(book: String, chapter: Int): Boolean = withContext(Dispatchers.IO) {
        val systemPrompt = """
            You are an accurate Bible transcription assistant. Provide all the verses for $book Chapter $chapter in standard KJV translation.
            Strictly return a JSON array containing objects with these exact keys:
            - "verse": Int (the verse number)
            - "text": String (the verse text)
            
            Ensure perfect accuracy. Do not explain, do not add markdown wrapping. Output pristine JSON.
        """.trimIndent()

        val prompt = "Give all KJV verses for $book $chapter"

        try {
            val jsonResult = GeminiService.queryGemini(prompt, systemPrompt, isJson = true)
            val parsedVerses = GeminiService.parseRemoteVerses(jsonResult)
            if (parsedVerses.isEmpty()) return@withContext false

            val modelVerses = parsedVerses.map { remote ->
                Verse(
                    book = book,
                    chapter = chapter,
                    verseNum = remote.verse,
                    text = remote.text
                )
            }
            verseDao.insertVerses(modelVerses)
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * Generate an AI-driven historical Commentary on the fly using Gemini, then save it in our Room database for offline review!
     */
    suspend fun generateAiCommentary(book: String, chapter: Int, verseNum: Int): String = withContext(Dispatchers.IO) {
        val prompt = """
            Provide a deep, historical commentary for $book $chapter:$verseNum.
            Adopt the perspective of a classical, scholarly church history professor.
            Format the commentary in structured Markdown, detailing:
            ### 1. Literary & Historical Context
            Examine the original historical occasion and the target audience of this book.
            ### 2. Theological Commentary
            Unpack the deep spiritual and theological truths centered inside the verse.
            ### 3. Patristic & Classical Interpretation
            Reference classic views (e.g., Augustine, Matthew Henry, early church summaries).
            
            Keep the tone scholarly, highly theological, and deeply clear.
        """.trimIndent()

        try {
            val commentaryText = GeminiService.queryGemini(prompt)
            if (commentaryText.contains("ERROR_MISSING_API_KEY")) {
                return@withContext "API_KEY_ERROR"
            }
            // Save to Local DB
            commentaryDao.insertCommentary(HistoricalCommentary(
                book = book,
                chapter = chapter,
                verseNum = verseNum,
                author = "Gemini Theological Scholar AI",
                text = commentaryText,
                isAiGenerated = true
            ))
            return@withContext commentaryText
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext "Error generating historical analysis: ${e.localizedMessage}"
        }
    }

    /**
     * Sync local offline queue changes to a simulated secure server.
     * Marks all pending local modifications as SYNCED.
     */
    suspend fun runDatabaseSyncSimulation(): SyncDetails = withContext(Dispatchers.IO) {
        val pendingNotes = noteDao.getPendingSyncNotes()
        val pendingBookmarks = bookmarkDao.getPendingSyncBookmarks()

        // Simulate server communication latency
        Thread.sleep(1800)

        if (pendingNotes.isNotEmpty()) {
            noteDao.markNotesAsSynced(pendingNotes.map { it.id })
        }
        if (pendingBookmarks.isNotEmpty()) {
            bookmarkDao.markBookmarksAsSynced(pendingBookmarks.map { it.id })
        }

        return@withContext SyncDetails(
            syncedNotesCount = pendingNotes.size,
            syncedBookmarksCount = pendingBookmarks.size,
            serverUrl = "https://sync.bible-analysis-cloud.org/v1/user-sync",
            timestamp = System.currentTimeMillis()
        )
    }
}

data class SyncDetails(
    val syncedNotesCount: Int,
    val syncedBookmarksCount: Int,
    val serverUrl: String,
    val timestamp: Long
)
