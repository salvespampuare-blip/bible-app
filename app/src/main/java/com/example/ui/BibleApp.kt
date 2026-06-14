package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.BorderStroke
import com.example.data.*
import com.example.viewmodel.BibleViewModel
import com.example.service.*

// ============================================================================
// 1. Theme Configuration Mapping
// ============================================================================

data class AppThemeColors(
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val onBackground: Color,
    val onSurface: Color,
    val accent: Color
)

fun getThemeColors(themeName: String): AppThemeColors {
    return when (themeName) {
        "Pristine Light" -> AppThemeColors(
            primary = Color(0xFF6200EE),
            secondary = Color(0xFF3700B3),
            background = Color(0xFFF9F9FB),
            surface = Color.White,
            onBackground = Color(0xFF1E1E24),
            onSurface = Color(0xFF1E1E24),
            accent = Color(0xFF03DAC6)
        )
        "Sepia Grace" -> AppThemeColors(
            primary = Color(0xFF795548),
            secondary = Color(0xFF5D4037),
            background = Color(0xFFFDF6E2),
            surface = Color(0xFFF4ECCE),
            onBackground = Color(0xFF4E3629),
            onSurface = Color(0xFF4E3629),
            accent = Color(0xFF8D6E63)
        )
        "Emerald Night" -> AppThemeColors(
            primary = Color(0xFF81C784),
            secondary = Color(0xFF388E3C),
            background = Color(0xFF0D1C13),
            surface = Color(0xFF142C1E),
            onBackground = Color(0xFFE8F5E9),
            onSurface = Color(0xFFE8F5E9),
            accent = Color(0xFF4CAF50)
        )
        "Midnight Navy" -> AppThemeColors(
            primary = Color(0xFF90CAF9),
            secondary = Color(0xFF1565C0),
            background = Color(0xFF0B132B),
            surface = Color(0xFF1C2541),
            onBackground = Color(0xFFE0E6ED),
            onSurface = Color(0xFFE0E6ED),
            accent = Color(0xFF00B4D8)
        )
        else -> AppThemeColors( // Cosmic Dark (Default)
            primary = Color(0xFFD0BCFF),
            secondary = Color(0xFFCCC2DC),
            background = Color(0xFF0C0B0F),
            surface = Color(0xFF17151D),
            onBackground = Color(0xFFE6E1E5),
            onSurface = Color(0xFFE6E1E5),
            accent = Color(0xFFBB86FC)
        )
    }
}

// ============================================================================
// 2. Main High-Fidelity Entry Point
// ============================================================================

@Composable
fun BibleApp(viewModel: BibleViewModel) {
    val activeTheme by viewModel.colorTheme.collectAsStateWithLifecycle()
    val colors = getThemeColors(activeTheme)

    // Core Active Screens
    var currentTab by rememberSaveable { mutableStateOf("reader") } // "reader", "search", "bookmarks", "plans", "billing"

    // Theme selector controls
    var showThemeMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.background
    ) {
        Scaffold(
            topBar = {
                HeaderBar(
                    viewModel = viewModel,
                    colors = colors,
                    onThemeToggle = { showThemeMenu = !showThemeMenu }
                )
            },
            bottomBar = {
                BottomBar(
                    colors = colors,
                    currentTab = currentTab,
                    onTabSelect = { currentTab = it }
                )
            },
            containerColor = colors.background,
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Background Gradient overlay for rich atmosphere
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(colors.background, colors.background, colors.surface)
                            )
                        )
                )

                // Navigation Screens routing with smooth fading transitions
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "TabTransition"
                ) { targetState ->
                    when (targetState) {
                        "search" -> SearchScreen(viewModel = viewModel, colors = colors)
                        "bookmarks" -> BookmarksScreen(viewModel = viewModel, colors = colors, onGoToVerse = { currentTab = "reader" })
                        "plans" -> PlansScreen(viewModel = viewModel, colors = colors)
                        "billing" -> BillingScreen(viewModel = viewModel, colors = colors)
                        else -> ReaderScreen(viewModel = viewModel, colors = colors)
                    }
                }

                // Theme selection dropdown dialog
                if (showThemeMenu) {
                    ThemeDialog(
                        activeTheme = activeTheme,
                        colors = colors,
                        onSelect = {
                            viewModel.setTheme(it)
                            showThemeMenu = false
                        },
                        onDismiss = { showThemeMenu = false }
                    )
                }

                // Simulated Stripe Checkout dynamic bottom-sheet overlay dialog
                val isStripeCheckoutOpening by viewModel.isStripeCheckoutOpening.collectAsStateWithLifecycle()
                if (isStripeCheckoutOpening) {
                    StripeCheckoutDialog(
                        viewModel = viewModel,
                        colors = colors
                    )
                }
            }
        }
    }
}

// ============================================================================
// 3. App Header Bar
// ============================================================================

@Composable
fun HeaderBar(
    viewModel: BibleViewModel,
    colors: AppThemeColors,
    onThemeToggle: () -> Unit
) {
    val syncText by viewModel.syncStatusText.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val pendingCount by viewModel.pendingChangesCount.collectAsStateWithLifecycle()
    val membership by viewModel.userMembershipTier.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.MenuBook,
                    contentDescription = "Bible Icon",
                    tint = colors.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Bible Analysis",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onBackground,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = membership,
                        fontSize = 12.sp,
                        color = colors.accent,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Quick Configuration Indicators & Font adjusters
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Font Sizes Adjuster
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.background)
                        .border(1.dp, colors.onBackground.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "A-",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onBackground,
                        modifier = Modifier
                            .clickable { viewModel.adjustFontSize(false) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                    Divider(
                        modifier = Modifier
                            .height(16.dp)
                            .width(1.dp),
                        color = colors.onBackground.copy(alpha = 0.2f)
                    )
                    Text(
                        text = "A+",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onBackground,
                        modifier = Modifier
                            .clickable { viewModel.adjustFontSize(true) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Database Sync trigger
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (pendingCount > 0) colors.primary.copy(alpha = 0.15f) else colors.background)
                        .clickable { viewModel.syncOfflineData() }
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                            color = colors.primary
                        )
                    } else {
                        BadgedBox(
                            badge = {
                                if (pendingCount > 0) {
                                    Badge(containerColor = colors.primary) {
                                        Text(pendingCount.toString(), color = Color.White)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (pendingCount > 0) Icons.Filled.CloudSync else Icons.Filled.Sync,
                                contentDescription = "Sync Cloud",
                                tint = if (pendingCount > 0) colors.primary else colors.onBackground.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Theme selector Palette
                IconButton(
                    onClick = onThemeToggle,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Palette,
                        contentDescription = "Font Palette",
                        tint = colors.primary
                    )
                }
            }
        }

        // Subtitle Sync Alert status bar
        Text(
            text = syncText,
            color = colors.onBackground.copy(alpha = 0.6f),
            fontSize = 11.sp,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ============================================================================
// 4. Custom App Theme Choice Popup Dialog
// ============================================================================

@Composable
fun ThemeDialog(
    activeTheme: String,
    colors: AppThemeColors,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Custom Theme Configurations",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = colors.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val listOfThemes = listOf("Cosmic Dark", "Pristine Light", "Sepia Grace", "Emerald Night", "Midnight Navy")
                listOfThemes.forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activeTheme == theme) colors.primary.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable { onSelect(theme) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = theme,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.onSurface
                        )
                        if (activeTheme == theme) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Active Theme icon",
                                tint = colors.primary
                            )
                        } else {
                            // Mini circles illustrating color theme preview
                            Row {
                                val demoColors = getThemeColors(theme)
                                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(demoColors.background).border(0.5.dp, colors.onSurface.copy(alpha=0.3f), CircleShape))
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(demoColors.surface).border(0.5.dp, colors.onSurface.copy(alpha=0.3f), CircleShape))
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(demoColors.primary))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text("Close", color = Color.White)
                }
            }
        }
    }
}

// ============================================================================
// 5. App Bottom Tab Bar Navigation
// ============================================================================

@Composable
fun BottomBar(
    colors: AppThemeColors,
    currentTab: String,
    onTabSelect: (String) -> Unit
) {
    NavigationBar(
        containerColor = colors.surface,
        modifier = Modifier.navigationBarsPadding()
    ) {
        val tabs = listOf(
            Triple("reader", "Reader", Icons.Filled.MenuBook),
            Triple("search", "Search", Icons.Filled.Search),
            Triple("bookmarks", "Saves", Icons.Filled.Bookmark),
            Triple("plans", "Plans", Icons.Filled.Event),
            Triple("billing", "Premium", Icons.Filled.CreditCard)
        )

        tabs.forEach { (tabId, label, icon) ->
            NavigationBarItem(
                selected = currentTab == tabId,
                onClick = { onTabSelect(tabId) },
                icon = {
                    Icon(imageVector = icon, contentDescription = label)
                },
                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.primary,
                    selectedTextColor = colors.primary,
                    indicatorColor = colors.primary.copy(alpha = 0.15f),
                    unselectedIconColor = colors.onSurface.copy(alpha = 0.6f),
                    unselectedTextColor = colors.onSurface.copy(alpha = 0.6f)
                )
            )
        }
    }
}

// ============================================================================
// 6. READER TAB: Main Scripture Reader, Commentary, and Notes Canvas
// ============================================================================

@Composable
fun ReaderScreen(
    viewModel: BibleViewModel,
    colors: AppThemeColors
) {
    val verses by viewModel.activeVerses.collectAsStateWithLifecycle()
    val books by viewModel.availableBooks.collectAsStateWithLifecycle()
    val chapters by viewModel.availableChapters.collectAsStateWithLifecycle()

    val activeBook by viewModel.selectedBook.collectAsStateWithLifecycle()
    val activeChapter by viewModel.selectedChapter.collectAsStateWithLifecycle()
    val activeVerseNum by viewModel.selectedVerseNum.collectAsStateWithLifecycle()

    val ttsSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val speechSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val isDownloading by viewModel.isChapterDownloading.collectAsStateWithLifecycle()

    val isBookmarked by viewModel.currentVerseIsBookmarked.collectAsStateWithLifecycle()
    val themeFontSize by viewModel.fontSize.collectAsStateWithLifecycle()
    val currentTier by viewModel.userMembershipTier.collectAsStateWithLifecycle()

    // Panel controls
    var showBookSelector by remember { mutableStateOf(false) }
    var showChapterSelector by remember { mutableStateOf(false) }

    // Scroll state & notes writing
    var customNoteText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // BOOK AND CHAPTER SELECTOR CHIPS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                // Book selector
                AssistChip(
                    onClick = { showBookSelector = true },
                    label = { Text(activeBook, fontWeight = FontWeight.Bold) },
                    trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = "Dropdown") },
                    colors = AssistChipDefaults.assistChipColors(labelColor = colors.primary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Chapter selector
                AssistChip(
                    onClick = { showChapterSelector = true },
                    label = { Text("Chapter $activeChapter", fontWeight = FontWeight.Bold) },
                    trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = "Dropdown") },
                    colors = AssistChipDefaults.assistChipColors(labelColor = colors.primary)
                )
            }

            // Voice Speech Reader controls Widget
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                // Voice button
                IconButton(
                    onClick = {
                        if (ttsSpeaking) viewModel.stopPlayingTts() else viewModel.playTtsOfChapter()
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = if (ttsSpeaking) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                        contentDescription = "Bible Voiceover play",
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Speed factor
                Box {
                    var speedExpanded by remember { mutableStateOf(false) }
                    Text(
                        text = "${speechSpeed}x",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary,
                        modifier = Modifier
                            .clickable { speedExpanded = true }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                    DropdownMenu(
                        expanded = speedExpanded,
                        onDismissRequest = { speedExpanded = false },
                        modifier = Modifier.background(colors.surface)
                    ) {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { s ->
                            DropdownMenuItem(
                                text = { Text("${s}x speed") },
                                onClick = {
                                    viewModel.setPlaybackSpeed(s)
                                    speedExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // MAIN SCRIPTURE LAZYCOLUMNS VIEW
        if (isDownloading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = colors.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Downloading full scripture transcription...", color = colors.onBackground, fontSize = 14.sp)
                }
            }
        } else if (verses.isEmpty()) {
            // Seeding trigger card if verses are empty or download needed
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Warning, "No Offline Content Icon", modifier = Modifier.size(48.dp), tint = colors.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "This chapter is currently offline.",
                            fontWeight = FontWeight.Bold,
                            color = colors.onBackground,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Enable dynamic transcription: AI will fetch, translate, and securely cache this scripture block offline.",
                            fontSize = 12.sp,
                            color = colors.onBackground.copy(alpha=0.7f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.downloadMissingChapter() },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                        ) {
                            Icon(Icons.Filled.CloudDownload, "Download Offline Icon")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Fetch & Stream Translation", color = Color.White)
                        }
                    }
                }
            }
        } else {
            // SCRIPTURE PANEL
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    items(verses) { verse ->
                        val isSelected = verse.verseNum == activeVerseNum
                        val isVersePremium = verse.isPremium

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) colors.primary.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { viewModel.selectVerse(verse.verseNum) }
                                .padding(vertical = 10.dp, horizontal = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "Verse ${verse.verseNum}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (themeFontSize - 4).sp,
                                    color = if (isSelected) colors.primary else colors.onBackground.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )

                                Row {
                                    if (isVersePremium) {
                                        Icon(
                                            imageVector = Icons.Filled.Stars,
                                            contentDescription = "Premium Verse indicator",
                                            tint = Color(0xFFFFB300),
                                            modifier = Modifier
                                                .size(16.dp)
                                                .padding(end = 4.dp)
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Bookmark,
                                            contentDescription = "Bookmark toggle status",
                                            tint = if (isBookmarked) colors.primary else colors.onBackground.copy(alpha = 0.3f),
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clickable { viewModel.toggleBookmark() }
                                        )
                                    }
                                }
                            }

                            // Paywall check simulation
                            if (isVersePremium && currentTier == "Free Standard Plan") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(colors.primary.copy(alpha = 0.05f))
                                        .border(1.dp, Color(0xFFFFB300), RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Lock, "Premium locked passage", tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Academic Tier Exclusive passage", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFFFB300))
                                    }
                                    Text("This verse is locked under the premium tiered membership framework. Secure checkout unlock is requested.", fontSize = 11.sp, color = colors.onBackground.copy(alpha=0.8f), modifier = Modifier.padding(top=4.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Button(
                                        onClick = { viewModel.openStripeCheckout("Premium Discipleship") },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp).testTag("premium_unlock_button")
                                    ) {
                                        Text("Unlock with Stripe", fontSize = 10.sp, color = Color.Black)
                                    }
                                }
                            } else {
                                Text(
                                    text = verse.text,
                                    fontSize = themeFontSize.sp,
                                    lineHeight = (themeFontSize * 1.5).sp,
                                    color = if (isSelected) colors.primary else colors.onBackground,
                                    fontFamily = FontFamily.Serif
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // UNDER-READER ACTIVE VERSE ANALYSIS BOX (Notes, Cross References, Historical Commentary)
        Text(
            text = "$activeBook $activeChapter:$activeVerseNum Analysis Panel",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = colors.primary,
            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
        )

        // TABS FOR ANALYSIS COLUMN
        var analysisTab by remember { mutableStateOf("commentary") } // "commentary", "cross", "notes"
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabsList = listOf(
                Pair("commentary", "Historical Commentary"),
                Pair("cross", "Cross References"),
                Pair("notes", "Verse Journal")
            )
            tabsList.forEach { (tabId, tabName) ->
                val tabSelected = analysisTab == tabId
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (tabSelected) colors.primary else colors.surface)
                        .clickable { analysisTab = tabId }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tabName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (tabSelected) Color.White else colors.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // ANALYSIS CONTENT AREA
        Card(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                when (analysisTab) {
                    "cross" -> {
                        val crossRefs by viewModel.activeCrossReferences.collectAsStateWithLifecycle()
                        if (crossRefs.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No preloaded references. Explore other verses like Psa 23:1 or John 1:1, or use AI Search.", fontSize = 11.sp, textAlign = TextAlign.Center, color = colors.onBackground.copy(alpha=0.5f))
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(crossRefs) { ref ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(colors.background.copy(alpha = 0.5f))
                                            .border(1.dp, colors.onBackground.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                            .padding(10.dp)
                                            .clickable { viewModel.selectLocation(ref.targetBook, ref.targetChapter, ref.targetVerse) }
                                    ) {
                                        Text(
                                            text = "→ ${ref.targetBook} ${ref.targetChapter}:${ref.targetVerse}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = colors.primary
                                        )
                                        Text(
                                            text = ref.description,
                                            fontSize = 11.sp,
                                            color = colors.onBackground.copy(alpha = 0.8f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                    }
                    "notes" -> {
                        val verseNotes by viewModel.currentVerseNotes.collectAsStateWithLifecycle()
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextField(
                                    value = customNoteText,
                                    onValueChange = { customNoteText = it },
                                    placeholder = { Text("Write personal study notes...", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = colors.background,
                                        unfocusedContainerColor = colors.background,
                                        focusedTextColor = colors.onBackground,
                                        unfocusedTextColor = colors.onBackground
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = {
                                        viewModel.writeNote(customNoteText)
                                        customNoteText = ""
                                    },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.primary)
                                ) {
                                    Icon(Icons.Filled.Send, "Add verse note", tint = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (verseNotes.isEmpty()) {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("This verse is journal-empty. Capture thoughts above.", color = colors.onBackground.copy(alpha=0.5f), fontSize = 11.sp)
                                }
                            } else {
                                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    items(verseNotes) { note ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(colors.background.copy(alpha = 0.5f))
                                                .padding(6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(0.85f)) {
                                                Text(note.content, fontSize = 12.sp, color = colors.onBackground)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = if (note.syncStatus == "SYNCED") Icons.Filled.CloudDone else Icons.Filled.CloudUpload,
                                                        contentDescription = "Sync state icon",
                                                        tint = if (note.syncStatus == "SYNCED") Color(0xFF4CAF50) else Color(0xFFFFB300),
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(note.syncStatus, fontSize = 9.sp, color = colors.onBackground.copy(alpha=0.5f))
                                                }
                                            }
                                            IconButton(
                                                onClick = { viewModel.deleteNote(note.id) },
                                                modifier = Modifier.size(28.dp).weight(0.15f)
                                            ) {
                                                Icon(Icons.Filled.Delete, "Delete personal note", tint = Color.Red.copy(alpha=0.6f), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Divider(color = colors.onBackground.copy(alpha = 0.1f))
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            }
                        }
                    }
                    else -> { // COMMENTARY TAB
                        val commentaries by viewModel.activeCommentaries.collectAsStateWithLifecycle()
                        val isGenerating by viewModel.isGeneratingCommentary.collectAsStateWithLifecycle()

                        Column(modifier = Modifier.fillMaxSize()) {
                            if (isGenerating) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = colors.primary)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Gemini AI consulting historical codex and patristic commentaries...", fontSize = 11.sp, color = colors.onBackground)
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Offline Commentary Sources:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.onBackground.copy(alpha=0.7f))
                                    Button(
                                        onClick = { viewModel.requestAiCommentary() },
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Icon(Icons.Filled.AutoAwesome, "Gemini auto commentary", modifier = Modifier.size(12.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Ask AI analysis", fontSize = 10.sp, color = Color.White)
                                    }
                                }

                                if (commentaries.isEmpty()) {
                                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("No preloaded commentary for this verse.", color = colors.onBackground.copy(alpha=0.5f), fontSize = 11.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Click 'Ask AI analysis' to synthesize scholars views.", fontSize = 10.sp, color = colors.onBackground.copy(alpha=0.4f))
                                        }
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                        items(commentaries) { comm ->
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(colors.background.copy(alpha = 0.5f))
                                                    .padding(8.dp)
                                            ) {
                                                Text(
                                                    text = comm.author,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.primary
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = comm.text,
                                                    fontSize = 12.sp,
                                                    color = colors.onBackground
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // LIST DIALOG DROPDOWNS FOR BOOK AND CHAPTER CHIPS
    if (showBookSelector) {
        Dialog(onDismissRequest = { showBookSelector = false }) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                modifier = Modifier.fillMaxWidth().height(420.dp).padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select Book of Scripture", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.onSurface)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    val bibleBooksDemoList = listOf("Genesis", "Psalms", "Proverbs", "Isaiah", "John", "Romans", "Philippians", "Hebrews")

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(bibleBooksDemoList) { item ->
                            Text(
                                text = item,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectLocation(item, 1)
                                        showBookSelector = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                fontSize = 16.sp,
                                color = colors.onSurface,
                                fontWeight = if (item == activeBook) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }

    if (showChapterSelector) {
        Dialog(onDismissRequest = { showChapterSelector = false }) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                modifier = Modifier.fillMaxWidth().height(260.dp).padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select Chapter", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.onSurface)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    val chaptersDemoMap = mapOf(
                        "Genesis" to listOf(1),
                        "Psalms" to listOf(23, 46),
                        "Proverbs" to listOf(3),
                        "Isaiah" to listOf(40),
                        "John" to listOf(1, 3, 14),
                        "Romans" to listOf(8),
                        "Philippians" to listOf(4),
                        "Hebrews" to listOf(11)
                    )
                    val listChapters = chaptersDemoMap[activeBook] ?: listOf(1)

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(listChapters) { ch ->
                            Text(
                                text = "Chapter $ch",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectLocation(activeBook, ch)
                                        showChapterSelector = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                fontSize = 16.sp,
                                color = colors.onSurface,
                                fontWeight = if (ch == activeChapter) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 7. SEARCH TAB: Keyword and Deep Semantic AI Search Engine
// ============================================================================

@Composable
fun SearchScreen(
    viewModel: BibleViewModel,
    colors: AppThemeColors
) {
    val query by viewModel.searchText.collectAsStateWithLifecycle()
    val localResults by viewModel.localResults.collectAsStateWithLifecycle()
    val semanticResults by viewModel.semanticResults.collectAsStateWithLifecycle()
    val searchLoading by viewModel.isSearchLoading.collectAsStateWithLifecycle()
    val subscriptionTier by viewModel.userMembershipTier.collectAsStateWithLifecycle()

    var activeSearchMode by remember { mutableStateOf("local") } // "local" or "semantic"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Bible Study Search Room",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = colors.primary
        )
        Text(
            text = "Index with traditional texts or use patented Semantic Analysis powered by Gemini AI.",
            fontSize = 12.sp,
            color = colors.onBackground.copy(alpha=0.7f),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Text input field search bar
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.updateQueryAndSearch(it) },
            placeholder = { Text("Enter keywords, themes or feelings (e.g. anxiety, creation)...") },
            leadingIcon = { Icon(Icons.Filled.Search, "Search bar icon") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.onBackground,
                unfocusedTextColor = colors.onBackground,
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.onBackground.copy(alpha=0.3f),
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Search options selection indicators (Local, Semantic)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { activeSearchMode = "local" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeSearchMode == "local") colors.primary else colors.surface,
                    contentColor = if (activeSearchMode == "local") Color.White else colors.onBackground.copy(alpha=0.7f)
                )
            ) {
                Icon(Icons.Filled.List, "Standard Text Index Icon")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Keyword Match", fontSize = 12.sp)
            }

            Button(
                onClick = { activeSearchMode = "semantic" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeSearchMode == "semantic") colors.primary else colors.surface,
                    contentColor = if (activeSearchMode == "semantic") Color.White else colors.onBackground.copy(alpha=0.7f)
                )
            ) {
                Icon(Icons.Filled.AutoAwesome, "Gemini Semantic AI search Icon")
                Spacer(modifier = Modifier.width(6.dp))
                Text("AI Semantic", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // DISPLAY ACCORDING TO MODE
        if (activeSearchMode == "semantic") {
            // Semantic Area
            if (subscriptionTier == "Free Standard Plan") {
                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Lock, "Premium locked features", modifier = Modifier.size(48.dp), tint = colors.primary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Standard Membership Constraint", fontWeight = FontWeight.Bold, color = colors.onSurface, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "AI Semantic search evaluates deeper spiritual meanings and lists relevant verses, even without exact keyword matches. This is locked under Premium membership.",
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                color = colors.onSurface.copy(alpha=0.7f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.openStripeCheckout("Premium Discipleship") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                                modifier = Modifier.testTag("upgrade_checkout_entry_button")
                            ) {
                                Text("Upgrade using Stripe", color = Color.Black)
                            }
                        }
                    }
                }
            } else {
                // Active semantic search panel
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AI-Generated Thematic Indexing:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.onBackground)
                    Button(
                        onClick = { viewModel.triggerSemanticSearch() },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                    ) {
                        Text("Search with Gemini AI", fontSize = 11.sp, color = Color.White)
                    }
                }

                if (searchLoading) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = colors.primary)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Gemini searching for parallel theological principles...", color = colors.onBackground, fontSize = 12.sp)
                        }
                    }
                } else if (semanticResults.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Type a themed sentence (e.g., 'feeling tired & weak') and click 'Search with Gemini AI'.", color = colors.onBackground.copy(alpha=0.5f), fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        items(semanticResults) { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable { viewModel.selectLocation(item.book, item.chapter, item.verseNum) },
                                colors = CardDefaults.cardColors(containerColor = colors.surface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${item.book} ${item.chapter}:${item.verseNum}", fontWeight = FontWeight.Bold, color = colors.primary)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(colors.accent.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("Semantically Matched", fontSize = 9.sp, color = colors.primary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(item.text, fontSize = 14.sp, fontFamily = FontFamily.Serif, color = colors.onSurface)
                                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = colors.onSurface.copy(alpha=0.1f))
                                    Text("Relevance Explanation: ${item.relevanceReason}", fontSize = 11.sp, color = colors.onSurface.copy(alpha=0.7f))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Local Match Area
            if (query.length < 2) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("Type a search keyword to search offline KJV Bible database indices immediately.", color = colors.onBackground.copy(alpha=0.5f), fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            } else if (localResults.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No keyword matches found in local SQLite catalog.", color = colors.onBackground.copy(alpha=0.5f), fontSize = 12.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    items(localResults) { v ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable { viewModel.selectLocation(v.book, v.chapter, v.verseNum) },
                            colors = CardDefaults.cardColors(containerColor = colors.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("${v.book} ${v.chapter}:${v.verseNum}", fontWeight = FontWeight.Bold, color = colors.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(v.text, fontSize = 13.sp, fontFamily = FontFamily.Serif, color = colors.onSurface)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 8. BOOKMARKS SCREEN: Easy Navigation to favorited scriptures
// ============================================================================

@Composable
fun BookmarksScreen(
    viewModel: BibleViewModel,
    colors: AppThemeColors,
    onGoToVerse: () -> Unit
) {
    val bookmarks by viewModel.activeBookmarks.collectAsStateWithLifecycle()
    val notes by viewModel.activeNotes.collectAsStateWithLifecycle()

    var activeSavesMode by remember { mutableStateOf("bookmarks") } // "bookmarks" or "notes"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "My Scripture Archive",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = colors.primary
        )

        // Select bookmarks vs notes
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { activeSavesMode = "bookmarks" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeSavesMode == "bookmarks") colors.primary else colors.surface,
                    contentColor = if (activeSavesMode == "bookmarks") Color.White else colors.onBackground.copy(alpha=0.7f)
                )
            ) {
                Text("Saved Passages", fontSize = 12.sp)
            }

            Button(
                onClick = { activeSavesMode = "notes" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeSavesMode == "notes") colors.primary else colors.surface,
                    contentColor = if (activeSavesMode == "notes") Color.White else colors.onBackground.copy(alpha=0.7f)
                )
            ) {
                Text("Journal Notes", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (activeSavesMode == "bookmarks") {
            if (bookmarks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Bookmark passages using the reader icon to organize study verses.", color = colors.onBackground.copy(alpha=0.5f), fontSize=12.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(bookmarks) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable {
                                    viewModel.selectLocation(item.book, item.chapter, item.verseNum)
                                    onGoToVerse()
                                },
                            colors = CardDefaults.cardColors(containerColor = colors.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(0.85f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Bookmark, "Bookmarked item", tint = colors.primary)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("${item.book} ${item.chapter}:${item.verseNum}", fontWeight = FontWeight.Bold, color = colors.onSurface)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (item.syncStatus == "SYNCED") Icons.Filled.CloudDone else Icons.Filled.CloudUpload,
                                                contentDescription = "Sync state",
                                                tint = if (item.syncStatus == "SYNCED") Color(0xFF4CAF50) else Color(0xFFFFB300),
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(item.syncStatus, fontSize = 9.sp, color = colors.onSurface.copy(alpha=0.5f))
                                        }
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.deleteBookmark(item.id) },
                                    modifier = Modifier.size(34.dp).weight(0.15f)
                                ) {
                                    Icon(Icons.Filled.DeleteOutline, "Remove Bookmark", tint = Color.Red.copy(alpha=0.6f))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Notes list
            if (notes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No journal items created. Select any verse in the reader to type notes.", color = colors.onBackground.copy(alpha=0.5f), fontSize=12.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(notes) { note ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable {
                                    viewModel.selectLocation(note.book, note.chapter, note.verseNum)
                                    onGoToVerse()
                                },
                            colors = CardDefaults.cardColors(containerColor = colors.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${note.book} ${note.chapter}:${note.verseNum}", fontWeight = FontWeight.Bold, color = colors.primary)
                                    IconButton(
                                        onClick = { viewModel.deleteNote(note.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Filled.DeleteOutline, "Remove Note", tint = Color.Red.copy(alpha=0.6f))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(note.content, fontSize = 13.sp, color = colors.onSurface)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (note.syncStatus == "SYNCED") Icons.Filled.CloudDone else Icons.Filled.CloudUpload,
                                        contentDescription = "Sync state badge",
                                        tint = if (note.syncStatus == "SYNCED") Color(0xFF4CAF50) else Color(0xFFFFB300),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sync: ${note.syncStatus}", fontSize = 9.sp, color = colors.onSurface.copy(alpha=0.5f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 9. PLANS TAB: Offline study routes trackers
// ============================================================================

@Composable
fun PlansScreen(
    viewModel: BibleViewModel,
    colors: AppThemeColors
) {
    val plans by viewModel.readingPlans.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Theological Reading Plans",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = colors.primary
        )
        Text(
            text = "Track customizable routes to systemize scriptural literacy. Completely offline.",
            fontSize = 12.sp,
            color = colors.onBackground.copy(alpha=0.7f),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(plans) { plan ->
                val completedList = if (plan.completedDaysCsv.isEmpty()) emptyList() else plan.completedDaysCsv.split(",").mapNotNull { it.toIntOrNull() }
                val progressPercent = (completedList.size.toFloat() / plan.totalDays.toFloat() * 100).toInt()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(plan.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.onSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(plan.description, fontSize = 12.sp, color = colors.onSurface.copy(alpha=0.7f))

                        Spacer(modifier = Modifier.height(12.dp))

                        // Linear progress indicator bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Progress: $progressPercent%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                            Text("Day ${plan.currentDay} of ${plan.totalDays}", fontSize = 11.sp, color = colors.onSurface.copy(alpha=0.6f))
                        }

                        LinearProgressIndicator(
                            progress = completedList.size.toFloat() / plan.totalDays.toFloat(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = colors.primary,
                            trackColor = colors.onSurface.copy(alpha = 0.1f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Days horizontal scroll checklists grid representation
                        Text("Completed Days Checklist:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().background(colors.background.copy(alpha=0.5f)).padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Show first 6 days as toggleable buttons, demonstrating full custom tracking
                            for (dayNum in 1..8) {
                                val checked = completedList.contains(dayNum)
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (checked) colors.primary else colors.surface)
                                        .border(1.dp, if (checked) colors.primary else colors.onSurface.copy(alpha=0.2f), CircleShape)
                                        .clickable { viewModel.toggleCompletedPlanDay(plan, dayNum) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayNum.toString(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (checked) Color.White else colors.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 10. PREMIUM BILLING TAB: Stripe Premium Billing Tiers & Plans Checkout
// ============================================================================

@Composable
fun BillingScreen(
    viewModel: BibleViewModel,
    colors: AppThemeColors
) {
    val currentTier by viewModel.userMembershipTier.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Secure Stripe Portal",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = colors.primary
        )
        Text(
            text = "Secure Checkout system utilizing official Stripe API integration. Select plans below.",
            fontSize = 12.sp,
            color = colors.onBackground.copy(alpha=0.7f),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Membership current card banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.primary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("ACCOUNT MEMBERSHIP STATUS", fontSize = 10.sp, color = Color.White.copy(alpha=0.7f), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(currentTier, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(6.dp)
                    ) {
                        Icon(Icons.Filled.Verified, "Verified Subscription Badge", tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (currentTier == "Free Standard Plan") {
                    Text("Unlock semantic indexing, dynamic unlimited audio-speeds, theological historical Commentary analyses, and multiple parallel custom study plans.", fontSize = 11.sp, color = Color.White.copy(alpha=0.9f))
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Stripe Auto-renewal: Active", fontSize = 11.sp, color = Color.White.copy(alpha=0.9f))
                        Text(
                            text = "Cancel subscription",
                            fontSize = 11.sp,
                            color = Color.White,
                            modifier = Modifier
                                .clickable { viewModel.cancelMembership() }
                                .border(0.5.dp, Color.White, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // PLANS SELECTIONS LIST
        Text("Availabilities and Premium Tiers Matrix:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.onBackground)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = BorderStroke(1.dp, colors.onSurface.copy(alpha=0.15f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Free Standard Plan", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.onSurface)
                            Text("Free", fontWeight = FontWeight.Black, color = colors.primary)
                        }
                        Text("Standard offline scripture reading books, local search, corebookmarks, traditional Matthew Henry preloads.", fontSize = 11.sp, color = colors.onSurface.copy(alpha=0.7f))
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = BorderStroke(1.dp, if (currentTier == "Premium Discipleship") colors.primary else colors.onSurface.copy(alpha=0.15f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Premium Discipleship Plan", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.onSurface)
                            Text("$4.99/mo", fontWeight = FontWeight.Black, color = colors.primary)
                        }
                        Text("Unlocks semantic topic-to-scripture search, on-demand AI theology commentaries (Gemini-driven), dynamic TTS playback speed settings.", fontSize = 11.sp, color = colors.onSurface.copy(alpha=0.7f))
                        Spacer(modifier = Modifier.height(8.dp))
                        if (currentTier == "Premium Discipleship") {
                            Button(
                                onClick = {},
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                            ) {
                                Text("Current Active Plan", fontSize = 10.sp)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.openStripeCheckout("Premium Discipleship") },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                                modifier = Modifier.height(28.dp).testTag("select_plan_premium"),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                            ) {
                                Text("Checkout Premium", fontSize = 10.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = BorderStroke(1.dp, if (currentTier == "Scholar Elite") colors.primary else colors.onSurface.copy(alpha=0.15f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Scholar Elite Plan", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.onSurface)
                            Text("$14.99/mo", fontWeight = FontWeight.Black, color = colors.primary)
                        }
                        Text("Unlocks advanced Hebrew & Greek interlinear lexical outlines, offline scholar journals database, live patristic round-tables, and exclusive resources.", fontSize = 11.sp, color = colors.onSurface.copy(alpha=0.7f))
                        Spacer(modifier = Modifier.height(8.dp))
                        if (currentTier == "Scholar Elite") {
                            Button(
                                onClick = {},
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Current Active Plan", fontSize = 10.sp)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.openStripeCheckout("Scholar Elite") },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                            ) {
                                Text("Checkout Scholar Elite", fontSize = 10.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 11. STRIPE SECURE DIALOG: Real-looking stripe SDK Checkout Sheet
// ============================================================================

@Composable
fun StripeCheckoutDialog(
    viewModel: BibleViewModel,
    colors: AppThemeColors
) {
    val plan by viewModel.checkoutPlanSelected.collectAsStateWithLifecycle()
    val successText by viewModel.checkoutSuccessMessage.collectAsStateWithLifecycle()

    var cardHolder by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }

    var localProcessingState by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { viewModel.dismissCheckout() }) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header checkout with Stripe logos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF6772E5))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("stripe", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Secure Payment", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.onSurface.copy(alpha=0.6f))
                    }
                    IconButton(onClick = { viewModel.dismissCheckout() }) {
                        Icon(Icons.Filled.Close, "Close checkout dialog")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = colors.onSurface.copy(alpha=0.1f))

                if (successText != null) {
                    // Success Transaction state
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.CheckCircle, "Payment complete successfully", tint = Color(0xFF4CAF50), modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Stripe Payment Complete", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.onSurface)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(successText!!, fontSize = 11.sp, color = colors.onSurface.copy(alpha=0.8f), textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.dismissCheckout() },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                        ) {
                            Text("Unlock Content Now", color = Color.White)
                        }
                    }
                } else if (localProcessingState) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFF6772E5))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Connecting with Stripe Gateway...", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.onSurface)
                        Text("Authenticating card vault details...", fontSize = 11.sp, color = colors.onSurface.copy(alpha=0.6f))
                    }
                } else {
                    // Input Card info simulation
                    Text(
                        text = "Subscribe to: $plan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = colors.onSurface
                    )
                    Text(
                        text = "Total amount due today: " + (if (plan.contains("Scholar")) "$14.99" else "$4.99"),
                        fontSize = 12.sp,
                        color = colors.onSurface.copy(alpha=0.7f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Credit Card details field
                    Text("Cardholder Name", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.onSurface.copy(alpha=0.7f))
                    OutlinedTextField(
                        value = cardHolder,
                        onValueChange = { cardHolder = it },
                        placeholder = { Text("John Doe") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.onSurface,
                            unfocusedTextColor = colors.onSurface,
                            focusedBorderColor = Color(0xFF6772E5)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Text("Card Number", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.onSurface.copy(alpha=0.7f))
                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = {
                            if (it.length <= 16) cardNumber = it
                        },
                        placeholder = { Text("4242 •••• •••• 4242") },
                        leadingIcon = { Icon(Icons.Filled.Payment, "Card brand icon", tint = Color(0xFF6772E5)) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.onSurface,
                            unfocusedTextColor = colors.onSurface,
                            focusedBorderColor = Color(0xFF6772E5)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Expiry (MM/YY)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.onSurface.copy(alpha=0.7f))
                            OutlinedTextField(
                                value = expiry,
                                onValueChange = { expiry = it },
                                placeholder = { Text("12/28") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = colors.onSurface,
                                    unfocusedTextColor = colors.onSurface,
                                    focusedBorderColor = Color(0xFF6772E5)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1.0f)) {
                            Text("Security CVV", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.onSurface.copy(alpha=0.7f))
                            OutlinedTextField(
                                value = cvv,
                                onValueChange = { if (it.length <= 4) cvv = it },
                                placeholder = { Text("•••") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = colors.onSurface,
                                    unfocusedTextColor = colors.onSurface,
                                    focusedBorderColor = Color(0xFF6772E5)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (cardNumber.isNotBlank() && cardHolder.isNotBlank()) {
                                localProcessingState = true
                                viewModel.mockConfirmStripePayment(cardNumber, cardHolder)
                            }
                        },
                        enabled = (cardNumber.length >= 10 && cardHolder.isNotBlank()),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6772E5)),
                        modifier = Modifier.fillMaxWidth().testTag("stripe_pay_confirm_button")
                    ) {
                        Icon(Icons.Filled.Lock, "Secure payment button", tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pay Seguramente via Stripe", color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Stripe elements sandbox checkout session is fully encrypted and sandboxed.",
                        fontSize = 9.sp,
                        color = colors.onSurface.copy(alpha=0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
