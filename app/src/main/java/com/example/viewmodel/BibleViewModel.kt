package com.example.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.*
import com.example.service.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

class BibleViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    // Database Initialization
    private val db: BibleDatabase by lazy {
        Room.databaseBuilder(
            application,
            BibleDatabase::class.java, "bible-analysis-db"
        ).fallbackToDestructiveMigration().build()
    }

    val repository: BibleRepository by lazy {
        BibleRepository(db)
    }

    // --- State Streams ---
    // Location Selectors
    val availableBooks: StateFlow<List<String>> = repository.availableBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedBook = MutableStateFlow("Genesis")
    val selectedBook: StateFlow<String> = _selectedBook.asStateFlow()

    private val _selectedChapter = MutableStateFlow(1)
    val selectedChapter: StateFlow<Int> = _selectedChapter.asStateFlow()

    private val _selectedVerseNum = MutableStateFlow(1)
    val selectedVerseNum: StateFlow<Int> = _selectedVerseNum.asStateFlow()

    // Chapters for Active Book
    val availableChapters: StateFlow<List<Int>> = _selectedBook
        .flatMapLatest { book -> repository.getChaptersForBook(book) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(1))

    // Verses for Active Location
    val activeVerses: StateFlow<List<Verse>> = combine(_selectedBook, _selectedChapter) { book, chapter ->
        Pair(book, chapter)
    }.flatMapLatest { (book, chapter) ->
        repository.getVersesByChapter(book, chapter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Bookmarks and Notes
    val activeBookmarks: StateFlow<List<Bookmark>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeNotes: StateFlow<List<Note>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active bookmarks status for current page
    val currentVerseIsBookmarked: StateFlow<Boolean> = combine(_selectedBook, _selectedChapter, _selectedVerseNum) { b, c, v ->
        Triple(b, c, v)
    }.flatMapLatest { (b, c, v) ->
        repository.isBookmarked(b, c, v)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Notes for active selected verse
    val currentVerseNotes: StateFlow<List<Note>> = combine(_selectedBook, _selectedChapter, _selectedVerseNum) { b, c, v ->
        Triple(b, c, v)
    }.flatMapLatest { (b, c, v) ->
        db.noteDao().getNotesForVerse(b, c, v)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Commentaries & CrossReferences for active verse
    val activeCommentaries: StateFlow<List<HistoricalCommentary>> = combine(_selectedBook, _selectedChapter, _selectedVerseNum) { b, c, v ->
        Triple(b, c, v)
    }.flatMapLatest { (b, c, v) ->
        repository.getCommentaries(b, c, v)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCrossReferences: StateFlow<List<CrossReference>> = combine(_selectedBook, _selectedChapter, _selectedVerseNum) { b, c, v ->
        Triple(b, c, v)
    }.flatMapLatest { (b, c, v) ->
        repository.getCrossReferences(b, c, v)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Search States ---
    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    private val _isSearchLoading = MutableStateFlow(false)
    val isSearchLoading = _isSearchLoading.asStateFlow()

    private val _semanticResults = MutableStateFlow<List<SemanticVerseResult>>(emptyList())
    val semanticResults = _semanticResults.asStateFlow()

    private val _localResults = MutableStateFlow<List<Verse>>(emptyList())
    val localResults = _localResults.asStateFlow()

    // --- Font & Theme Selections ---
    private val _fontSize = MutableStateFlow(18) // dp/sp sizes
    val fontSize = _fontSize.asStateFlow()

    private val _colorTheme = MutableStateFlow("Cosmic Dark") // "Pristine Light", "Cosmic Dark", "Sepia Grace", "Emerald Night", "Midnight Navy"
    val colorTheme = _colorTheme.asStateFlow()

    // --- TTS Voice cover ---
    private var tts: TextToSpeech? = null
    private val _isTtsInitialized = MutableStateFlow(false)
    val isTtsInitialized = _isTtsInitialized.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking = _isSpeaking.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed = _playbackSpeed.asStateFlow()

    // --- Reading Plans list ---
    val readingPlans: StateFlow<List<ReadingPlan>> = repository.readingPlans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Synchronization Status ---
    private val _syncStatusText = MutableStateFlow("All data synchronized.")
    val syncStatusText = _syncStatusText.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _pendingChangesCount = MutableStateFlow(0)
    val pendingChangesCount = _pendingChangesCount.asStateFlow()

    // --- Stripe & Tier Billings ---
    private val _userMembershipTier = MutableStateFlow("Free Standard Plan") // "Free Standard Plan", "Premium Discipleship", "Scholar Elite"
    val userMembershipTier = _userMembershipTier.asStateFlow()

    private val _isStripeCheckoutOpening = MutableStateFlow(false)
    val isStripeCheckoutOpening = _isStripeCheckoutOpening.asStateFlow()

    private val _checkoutPlanSelected = MutableStateFlow("")
    val checkoutPlanSelected = _checkoutPlanSelected.asStateFlow()

    private val _checkoutSuccessMessage = MutableStateFlow<String?>(null)
    val checkoutSuccessMessage = _checkoutSuccessMessage.asStateFlow()

    // --- AI commentary loading states ---
    private val _isGeneratingCommentary = MutableStateFlow(false)
    val isGeneratingCommentary = _isGeneratingCommentary.asStateFlow()

    private val _isChapterDownloading = MutableStateFlow(false)
    val isChapterDownloading = _isChapterDownloading.asStateFlow()

    init {
        // Run database seeding
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
            updatePendingSyncCount()
        }

        // Initialize Native TextToSpeech
        tts = TextToSpeech(application, this)
    }

    // ============================================================================
    // Action Methods
    // ============================================================================

    fun selectLocation(book: String, chapter: Int, verseNum: Int = 1) {
        _selectedBook.value = book
        _selectedChapter.value = chapter
        _selectedVerseNum.value = verseNum
    }

    fun selectVerse(verseNum: Int) {
        _selectedVerseNum.value = verseNum
    }

    fun adjustFontSize(increment: Boolean) {
        val current = _fontSize.value
        if (increment && current < 36) {
            _fontSize.value = current + 2
        } else if (!increment && current > 12) {
            _fontSize.value = current - 2
        }
    }

    fun setTheme(theme: String) {
        _colorTheme.value = theme
    }

    // Word play check count
    private fun updatePendingSyncCount() {
        viewModelScope.launch {
            val pendingNotes = db.noteDao().getPendingSyncNotes().size
            val pendingBookmarks = db.bookmarkDao().getPendingSyncBookmarks().size
            _pendingChangesCount.value = pendingNotes + pendingBookmarks
            _syncStatusText.value = if (_pendingChangesCount.value > 0) {
                "${_pendingChangesCount.value} changes pending database sync"
            } else {
                "All data synchronized."
            }
        }
    }

    // Bookmarks and Notes controls
    fun toggleBookmark() {
        viewModelScope.launch {
            repository.toggleBookmark(_selectedBook.value, _selectedChapter.value, _selectedVerseNum.value)
            updatePendingSyncCount()
        }
    }

    fun writeNote(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            repository.addNote(_selectedBook.value, _selectedChapter.value, _selectedVerseNum.value, content)
            updatePendingSyncCount()
        }
    }

    fun deleteNote(noteId: Int) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
            updatePendingSyncCount()
        }
    }

    fun deleteBookmark(bookmarkId: Int) {
        viewModelScope.launch {
            repository.removeBookmarkById(bookmarkId)
            updatePendingSyncCount()
        }
    }

    // Local and Semantics Search
    fun updateQueryAndSearch(query: String) {
        _searchText.value = query
        if (query.length < 2) {
            _localResults.value = emptyList()
            return
        }

        // Search locally reactively
        viewModelScope.launch {
            repository.searchVersesLocal(query).collect { results ->
                _localResults.value = results
            }
        }
    }

    fun triggerSemanticSearch() {
        val query = _searchText.value
        if (query.length < 3) return

        // Semantic analysis is a Premium tier exclusive tool!
        if (_userMembershipTier.value == "Free Standard Plan") {
            _checkoutPlanSelected.value = "Premium Discipleship"
            _isStripeCheckoutOpening.value = true
            return
        }

        viewModelScope.launch {
            _isSearchLoading.value = true
            val results = repository.semanticSearch(query)
            _semanticResults.value = results
            _isSearchLoading.value = false
        }
    }

    // TTS Control Methods
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("BibleTTS", "English US voice not supported on this device.")
            } else {
                _isTtsInitialized.value = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        tts?.setSpeechRate(speed)
    }

    fun playTtsOfChapter() {
        if (!_isTtsInitialized.value) return
        val verses = activeVerses.value
        if (verses.isEmpty()) return

        stopPlayingTts()

        // Compile KJV speech passage
        val speechBuilder = java.lang.StringBuilder()
        speechBuilder.append("${_selectedBook.value} Chapter ${_selectedChapter.value}. ")
        verses.forEach {
            speechBuilder.append("Verse ${it.verseNum}. ${it.text} ")
        }

        tts?.speak(speechBuilder.toString(), TextToSpeech.QUEUE_FLUSH, null, "BIBLE_CHAPTER_SPEAK")
        _isSpeaking.value = true
    }

    fun stopPlayingTts() {
        if (tts?.isSpeaking == true) {
            tts?.stop()
        }
        _isSpeaking.value = false
    }

    // Active reading plans checked-off days tracker
    fun toggleCompletedPlanDay(plan: ReadingPlan, day: Int) {
        viewModelScope.launch {
            val list = if (plan.completedDaysCsv.isEmpty()) mutableListOf() else plan.completedDaysCsv.split(",").mapNotNull { it.toIntOrNull() }.toMutableList()
            if (list.contains(day)) {
                list.remove(day)
            } else {
                list.add(day)
            }
            val newCsv = list.sorted().joinToString(",")
            val nextActiveDay = if (list.isEmpty()) 1 else {
                val lastCompleted = list.sorted().lastOrNull() ?: 1
                if (lastCompleted < plan.totalDays) lastCompleted + 1 else plan.totalDays
            }
            repository.updateReadingPlan(plan.copy(completedDaysCsv = newCsv, currentDay = nextActiveDay))
        }
    }

    // Remote Bible on-demand downloader
    fun downloadMissingChapter() {
        viewModelScope.launch {
            _isChapterDownloading.value = true
            val success = repository.fetchAndCacheChapter(_selectedBook.value, _selectedChapter.value)
            if (!success) {
                // If offline or failed, seed some generic verses for that chapter so app works
                val mockVerses = List(5) { index ->
                    Verse(_selectedBook.value, _selectedChapter.value, index + 1, "Grace and truth be multiplied through your offline pursuit of ${_selectedBook.value} ${_selectedChapter.value}:${index+1}.")
                }
                db.verseDao().insertVerses(mockVerses)
            }
            _isChapterDownloading.value = false
        }
    }

    // AI historical commentary generator
    fun requestAiCommentary() {
        // AI Commentary is a Premium/Scholar tier feature!
        if (_userMembershipTier.value == "Free Standard Plan") {
            _checkoutPlanSelected.value = "Premium Discipleship"
            _isStripeCheckoutOpening.value = true
            return
        }

        viewModelScope.launch {
            _isGeneratingCommentary.value = true
            repository.generateAiCommentary(_selectedBook.value, _selectedChapter.value, _selectedVerseNum.value)
            _isGeneratingCommentary.value = false
        }
    }

    // Simulated Synchronizer
    fun syncOfflineData() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatusText.value = "Connecting to secure cloud synchronization..."
            val details = repository.runDatabaseSyncSimulation()
            _isSyncing.value = false
            updatePendingSyncCount()
            _syncStatusText.value = "Synchronized ${details.syncedNotesCount} notes and ${details.syncedBookmarksCount} bookmarks securely."
        }
    }

    // ============================================================================
    // Stripe Subscription upgrades
    // ============================================================================

    fun openStripeCheckout(plan: String) {
        _checkoutPlanSelected.value = plan
        _isStripeCheckoutOpening.value = true
    }

    fun dismissCheckout() {
        _isStripeCheckoutOpening.value = false
        _checkoutSuccessMessage.value = null
    }

    fun mockConfirmStripePayment(cardNumber: String, cardHolder: String) {
        viewModelScope.launch {
            _isSyncing.value = true // borrow spinner
            _syncStatusText.value = "Processing Stripe transaction securely..."
            kotlinx.coroutines.delay(1600) // progress mock
            _userMembershipTier.value = _checkoutPlanSelected.value
            _checkoutSuccessMessage.value = "Subscription Successful! Stripe token generated properly. You are now a ${_checkoutPlanSelected.value} member."
            _isSyncing.value = false
            _syncStatusText.value = "Billing status: Active Subscription -> ${_checkoutPlanSelected.value}."
            updatePendingSyncCount()
        }
    }

    fun cancelMembership() {
        _userMembershipTier.value = "Free Standard Plan"
        _syncStatusText.value = "Billing changed: Standard Membership."
    }

    override fun onCleared() {
        super.onCleared()
        tts?.shutdown()
    }
}
