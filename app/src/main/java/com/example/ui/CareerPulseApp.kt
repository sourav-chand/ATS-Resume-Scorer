package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ResumeAnalysis
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CareerPulseApp(viewModel: MainViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val allAnalyses by viewModel.allAnalyses.collectAsStateWithLifecycle()
    val latestAnalysis by viewModel.latestAnalysis.collectAsStateWithLifecycle()
    val selectedAnalysis by viewModel.selectedAnalysis.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val scanError by viewModel.scanError.collectAsStateWithLifecycle()

    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isWideScreen) {
            // Adaptive Layout with NavigationRail (side menu for wider devices/tablets)
            Row(modifier = Modifier.fillMaxSize()) {
                CareerPulseNavigationRail(
                    selectedTab = selectedTab,
                    onTabSelected = { viewModel.selectTab(it) }
                )
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                ) {
                    MainContentArea(
                        selectedTab = selectedTab,
                        allAnalyses = allAnalyses,
                        latestAnalysis = latestAnalysis,
                        selectedAnalysis = selectedAnalysis,
                        isScanning = isScanning,
                        scanError = scanError,
                        viewModel = viewModel
                    )
                }
            }
        } else {
            // Mobile Layout with Top App Bar and Bottom Navigation Bar
            Scaffold(
                topBar = {
                    CareerPulseTopBar()
                },
                bottomBar = {
                    CareerPulseBottomNav(
                        selectedTab = selectedTab,
                        onTabSelected = { viewModel.selectTab(it) }
                    )
                },
                contentWindowInsets = WindowInsets.safeDrawing
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    MainContentArea(
                        selectedTab = selectedTab,
                        allAnalyses = allAnalyses,
                        latestAnalysis = latestAnalysis,
                        selectedAnalysis = selectedAnalysis,
                        isScanning = isScanning,
                        scanError = scanError,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun MainContentArea(
    selectedTab: Int,
    allAnalyses: List<ResumeAnalysis>,
    latestAnalysis: ResumeAnalysis?,
    selectedAnalysis: ResumeAnalysis?,
    isScanning: Boolean,
    scanError: String?,
    viewModel: MainViewModel
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (selectedTab) {
            0 -> DashboardScreen(
                allAnalyses = allAnalyses,
                latestAnalysis = latestAnalysis,
                onViewReport = { analysis -> viewModel.selectAnalysis(analysis) },
                onNavigateToUpload = { viewModel.selectTab(1) },
                onDeleteAnalysis = { viewModel.deleteAnalysis(it) }
            )
            1 -> UploadScreen(
                isScanning = isScanning,
                scanError = scanError,
                onScanTriggered = { name, text, job -> 
                    viewModel.scanResume(name, text, job)
                },
                onClearError = { viewModel.clearScanError() }
            )
            2 -> ReportScreen(
                analysis = selectedAnalysis,
                onSelectDifferent = { viewModel.selectTab(0) }, // Go back to history list
                onNavigateToCoach = { viewModel.selectTab(3) }
            )
            3 -> CoachScreen(viewModel = viewModel)
        }

        // Overlay global busy dialog when scanning
        if (isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxWidth(0.85f)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(28.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = NavyPrimary,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Analyzing Resume Credentials...",
                            style = MaterialTheme.typography.headlineMedium,
                            color = OnSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Extracting critical skills, parsing layout schemas, and grading keyword match indicators through Gemini 3.5 Flash.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// Custom Circular Progress Meter Component
@Composable
fun ScoreCircularMeter(score: Int, modifier: Modifier = Modifier) {
    val progressColor = when {
        score < 50 -> ErrorRed
        score < 80 -> WarningAmber
        else -> SuccessGreen
    }
    val backgroundRingColor = progressColor.copy(alpha = 0.15f)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = 10.dp.toPx()
            val sizeMin = size.minDimension
            val radius = (sizeMin - strokeWidthPx) / 2

            // Draw Background Oval Ring
            drawCircle(
                color = backgroundRingColor,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidthPx)
            )

            // Draw Colored Score Overlay Arc
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = (score / 100f) * 360f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 32.sp),
                color = OnSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "/ 100",
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant
            )
        }
    }
}

// Format Unix Timestamp to Readable Scan Format
fun formatScanDate(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Calendar.getInstance()
    val scanCal = Calendar.getInstance().apply { time = date }

    return when {
        // Today
        now.get(Calendar.YEAR) == scanCal.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == scanCal.get(Calendar.DAY_OF_YEAR) -> {
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            "Scanned Today, ${sdf.format(date)}"
        }
        // Yesterday
        now.get(Calendar.YEAR) == scanCal.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) - scanCal.get(Calendar.DAY_OF_YEAR) == 1 -> {
            "Scanned Yesterday"
        }
        else -> {
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            "Scanned on ${sdf.format(date)}"
        }
    }
}

// --- SUBVIEWS ---

// Mobile Top App Bar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareerPulseTopBar() {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Profile Avatar Circular Icon
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(SurfaceContainerHigh, CircleShape)
                        .border(1.5.dp, NavyPrimary.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User profile logo",
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "CareerPulse",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = NavyPrimary
                )
            }
        },
        actions = {
            IconButton(
                onClick = {},
                modifier = Modifier.size(48.dp)
            ) {
                BadgedBox(badge = { Badge { Text("2") } }) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Quick notifications indicator",
                        tint = OnSurface
                    )
                }
            }
        },
        windowInsets = WindowInsets.statusBars
    )
}

// Tablet Navigation Rail
@Composable
fun CareerPulseNavigationRail(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationRail(
        containerColor = SurfaceContainerLow,
        header = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(vertical = 24.dp)
            ) {
                Text(
                    text = "CareerPulse",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = NavyPrimary
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(SurfaceContainerHigh, CircleShape)
                        .border(1.5.dp, NavyPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Member profile picture avatar",
                        tint = OnSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Pro Member",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarningAmber
                )
            }
        },
        modifier = Modifier.width(100.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            NavigationRailItem(
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                label = { Text("Dashboard", style = MaterialTheme.typography.labelMedium) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = OnPrimary,
                    selectedTextColor = NavyPrimary,
                    indicatorColor = NavyPrimary,
                    unselectedIconColor = OnSurfaceVariant,
                    unselectedTextColor = OnSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            NavigationRailItem(
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                icon = { Icon(Icons.Default.CloudUpload, contentDescription = "Upload") },
                label = { Text("Upload", style = MaterialTheme.typography.labelMedium) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = OnPrimary,
                    selectedTextColor = NavyPrimary,
                    indicatorColor = NavyPrimary,
                    unselectedIconColor = OnSurfaceVariant,
                    unselectedTextColor = OnSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            NavigationRailItem(
                selected = selectedTab == 2,
                onClick = { onTabSelected(2) },
                icon = { Icon(Icons.Default.Analytics, contentDescription = "Analysis") },
                label = { Text("Analysis", style = MaterialTheme.typography.labelMedium) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = OnPrimary,
                    selectedTextColor = NavyPrimary,
                    indicatorColor = NavyPrimary,
                    unselectedIconColor = OnSurfaceVariant,
                    unselectedTextColor = OnSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            NavigationRailItem(
                selected = selectedTab == 3,
                onClick = { onTabSelected(3) },
                icon = { Icon(Icons.Default.Lightbulb, contentDescription = "Advice") },
                label = { Text("Advice", style = MaterialTheme.typography.labelMedium) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = OnPrimary,
                    selectedTextColor = NavyPrimary,
                    indicatorColor = NavyPrimary,
                    unselectedIconColor = OnSurfaceVariant,
                    unselectedTextColor = OnSurfaceVariant
                )
            )
        }
    }
}

// Mobile Bottom Navigation Bar
@Composable
fun CareerPulseBottomNav(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = SurfaceContainerLowest,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets.navigationBars
    ) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
            label = { Text("Dashboard", style = MaterialTheme.typography.labelMedium) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = OnPrimary,
                selectedTextColor = NavyPrimary,
                indicatorColor = NavyPrimary,
                unselectedIconColor = OnSurfaceVariant,
                unselectedTextColor = OnSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_tab_dashboard")
        )
        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            icon = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
            label = { Text("Upload", style = MaterialTheme.typography.labelMedium) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = OnPrimary,
                selectedTextColor = NavyPrimary,
                indicatorColor = NavyPrimary,
                unselectedIconColor = OnSurfaceVariant,
                unselectedTextColor = OnSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_tab_upload")
        )
        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            icon = { Icon(Icons.Default.Analytics, contentDescription = null) },
            label = { Text("Analysis", style = MaterialTheme.typography.labelMedium) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = OnPrimary,
                selectedTextColor = NavyPrimary,
                indicatorColor = NavyPrimary,
                unselectedIconColor = OnSurfaceVariant,
                unselectedTextColor = OnSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_tab_analysis")
        )
        NavigationBarItem(
            selected = selectedTab == 3,
            onClick = { onTabSelected(3) },
            icon = { Icon(Icons.Default.Lightbulb, contentDescription = null) },
            label = { Text("Advice", style = MaterialTheme.typography.labelMedium) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = OnPrimary,
                selectedTextColor = NavyPrimary,
                indicatorColor = NavyPrimary,
                unselectedIconColor = OnSurfaceVariant,
                unselectedTextColor = OnSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_tab_advice")
        )
    }
}

// ---------------- DASHBOARD TAB SCREEN ----------------

@Composable
fun DashboardScreen(
    allAnalyses: List<ResumeAnalysis>,
    latestAnalysis: ResumeAnalysis?,
    onViewReport: (ResumeAnalysis) -> Unit,
    onNavigateToUpload: () -> Unit,
    onDeleteAnalysis: (ResumeAnalysis) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Welcome Header Section
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Welcome back, Alex.",
                    style = MaterialTheme.typography.displayLarge,
                    color = OnSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Here is your current career pulse and resume performance.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnSurfaceVariant
                )
            }
        }

        // Latest Scan Results Bento Card
        item {
            if (latestAnalysis != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                    border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Title header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Latest Scan Results",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = OnSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = latestAnalysis.fileName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            // Pill status "Just Now" / Scanned date
                            Box(
                                modifier = Modifier
                                    .border(1.dp, OutlineVariant, RoundedCornerShape(24.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Just Now",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = OnSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Custom meter and insights row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ScoreCircularMeter(
                                score = latestAnalysis.overallScore,
                                modifier = Modifier.size(112.dp)
                            )

                            // Quick findings box
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Red/Amber Warning keyword snippet
                                val keyWordCount = latestAnalysis.missingKeywords.split(",").size
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = ErrorContainer.copy(alpha = 0.15f)),
                                    border = BorderStroke(1.dp, ErrorContainer.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Warning",
                                            tint = WarningAmber,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Missing Keywords",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = OnSurface
                                            )
                                            Text(
                                                text = "$keyWordCount critical ATS keywords omitted.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = OnSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // Green Check formatting snippet
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SuccessContainer.copy(alpha = 0.15f)),
                                    border = BorderStroke(1.dp, SuccessContainer.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Check",
                                            tint = SuccessGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Formatting",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = OnSurface
                                            )
                                            Text(
                                                text = "Machine-readable structure is excellent.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = OnSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Full report CTA button on bottom right
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            TextButton(
                                onClick = { onViewReport(latestAnalysis) },
                                modifier = Modifier.testTag("view_report_button")
                            ) {
                                Text(
                                    text = "View Full Report",
                                    color = NavyPrimary,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = NavyPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Upload / Action Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "New Analysis",
                        style = MaterialTheme.typography.headlineMedium,
                        color = OnSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Drag Drop simulated container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.5.dp,
                                color = OutlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .background(SurfaceContainerLow)
                            .clickable { onNavigateToUpload() }
                            .padding(24.dp)
                            .testTag("drag_drop_zone"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(PrimaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = "Upload icon",
                                    tint = NavyPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Drag & Drop Resume",
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurface,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "PDF, DOCX, or Raw Text (Max 5MB)",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onNavigateToUpload,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("upload_new_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryContainer,
                            contentColor = OnSurface
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Upload New Resume",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        // Recent Resumes List Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Resumes",
                    style = MaterialTheme.typography.headlineLarge,
                    color = OnSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "View All",
                    style = MaterialTheme.typography.labelLarge,
                    color = NavyPrimary,
                    modifier = Modifier.clickable { /* action */ }
                )
            }
        }

        if (allAnalyses.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No analyzed resumes yet. Start scanning now!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                }
            }
        } else {
            items(allAnalyses) { analysis ->
                RecentResumeListItem(
                    analysis = analysis,
                    onClick = { onViewReport(analysis) },
                    onDelete = { onDeleteAnalysis(analysis) }
                )
            }
        }
    }
}

@Composable
fun RecentResumeListItem(
    analysis: ResumeAnalysis,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
        border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // File Icon Box
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(SurfaceContainerHigh, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "Document icon",
                        tint = OnSurfaceVariant
                    )
                }

                Column(modifier = Modifier.padding(end = 8.dp)) {
                    Text(
                        text = analysis.fileName,
                        style = MaterialTheme.typography.labelLarge,
                        color = OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatScanDate(analysis.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Score Badge Pill with up/check icon to match mockup
                val isHigh = analysis.overallScore >= 80
                val icon = if (isHigh) Icons.Default.Check else Icons.Default.TrendingUp
                val containerColor = if (isHigh) SuccessContainer else WarningContainer
                val contentColor = if (isHigh) OnSuccessContainer else OnWarningContainer

                Row(
                    modifier = Modifier
                        .background(containerColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .border(1.dp, containerColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${analysis.overallScore}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(12.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box {
                    IconButton(onClick = { expandedMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = OnSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false },
                        modifier = Modifier.background(SurfaceContainerHigh)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Delete report", color = ErrorRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed) },
                            onClick = {
                                expandedMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

// ---------------- UPLOAD TAB SCREEN ----------------

@Composable
fun UploadScreen(
    isScanning: Boolean,
    scanError: String?,
    onScanTriggered: (name: String, text: String, job: String) -> Unit,
    onClearError: () -> Unit
) {
    var resumeName by remember { mutableStateOf("") }
    var targetJob by remember { mutableStateOf("") }
    var resumeText by remember { mutableStateOf("") }

    val presetPMName = "Product Manager Candidate Draft"
    val presetPMJob = "Senior Product Manager"
    val presetPMText = """
        Alex Mercer
        Email: alex.mercer@gmail.com | Phone: 555-0199
        SUMMARY:
        Dynamic team lead and technical professional with 7+ years of experience leading cross-functional teams in shipping analytics platforms and user monetization systems. Proven expertise in system architecture, agile methodologies, wireframing, and user growth frameworks.
        
        EXPERIENCE:
        Lead Technical Planner @ FinCloud (2022 - Present)
        - Managed SaaS feature roadmap, deploying 4 enterprise systems that handled 50,000 active concurrent queries.
        - Coordinated agile retrospectives, wireframed primary layout patterns, and integrated metric databases to monitor funnel drops.
        
        System Orchestrator @ NetSystems (2019 - 2022)
        - Refactored legacy transaction core pipelines, reducing downtime by 34%.
        - Coordinated deployments with marketing and data science teams to execute structured tests.
        
        SKILLS:
        Product design workflows, SaaS deployments, SQL analytics, wireframe layout, agile pipelines.
    """.trimIndent()

    val presetSEJob = "Technical Lead - Core Infrastructure"
    val presetSEText = """
        Devon Vance
        SUMMARY:
        Technical Lead with 9+ years managing cloud-native high throughput backend components, gRPC endpoints, and service orchestration layer frameworks. High proficiency in scaling distributed architectures.
        
        EXPERIENCE:
        Technical Lead @ AlphaStack (2021 - Present)
        - Engineered cloud migration orchestration engines to transit 1.2M historical records with zero data losses.
        - Optimized database schema parameters leading to latency reductions of 42% on high-throughput REST transactions.
        - Acted as mentor-advisor to 5 junior server-side associates.
        
        SKILLS:
        Go, Kubernetes, cloud systems design, gRPC routing, PostgreSQL performance optimization.
    """.trimIndent()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text(
                text = "Resume Intelligence Scan",
                style = MaterialTheme.typography.displayLarge,
                color = OnSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Draft your details or paste a copy below. Map it directly against a specific role to get surgical keyword suggestions.",
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurfaceVariant
            )
        }

        if (scanError != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ErrorContainer),
                    border = BorderStroke(1.dp, ErrorRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Error icon",
                            tint = OnErrorContainer
                        )
                        Text(
                            text = scanError,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onClearError) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = OnErrorContainer)
                        }
                    }
                }
            }
        }

        // Autofill Preset Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Quick Demo Templates",
                    style = MaterialTheme.typography.labelLarge,
                    color = NavyPrimary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SuggestionChip(
                        onClick = {
                            resumeName = "Alex PM Resume"
                            targetJob = presetPMJob
                            resumeText = presetPMText
                        },
                        label = { Text("PM Resume Draft", color = OnSurface) }
                    )
                    SuggestionChip(
                        onClick = {
                            resumeName = "Devon Google TechLead"
                            targetJob = presetSEJob
                            resumeText = presetSEText
                        },
                        label = { Text("SL Tech Lead Draft", color = OnSurface) }
                    )
                }
            }
        }

        // Form Fields Container
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title input
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Resume File/Draft Name",
                            style = MaterialTheme.typography.labelLarge,
                            color = OnSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedTextField(
                            value = resumeName,
                            onValueChange = { resumeName = it },
                            placeholder = { Text("e.g. Senior PM Resume v4", color = OnSurfaceVariant.copy(alpha = 0.5f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NavyPrimary,
                                unfocusedBorderColor = OutlineVariant,
                                focusedContainerColor = SurfaceContainerLow,
                                unfocusedContainerColor = SurfaceContainerLow
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    // Target Job input
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Target Position/Job Description",
                            style = MaterialTheme.typography.labelLarge,
                            color = OnSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedTextField(
                            value = targetJob,
                            onValueChange = { targetJob = it },
                            placeholder = { Text("e.g. Senior Product Manager", color = OnSurfaceVariant.copy(alpha = 0.5f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NavyPrimary,
                                unfocusedBorderColor = OutlineVariant,
                                focusedContainerColor = SurfaceContainerLow,
                                unfocusedContainerColor = SurfaceContainerLow
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    // Multi-line resume content input
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Resume Details / Paste text",
                            style = MaterialTheme.typography.labelLarge,
                            color = OnSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedTextField(
                            value = resumeText,
                            onValueChange = { resumeText = it },
                            placeholder = { Text("Paste or type resume professional history segments here...", color = OnSurfaceVariant.copy(alpha = 0.5f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NavyPrimary,
                                unfocusedBorderColor = OutlineVariant,
                                focusedContainerColor = SurfaceContainerLow,
                                unfocusedContainerColor = SurfaceContainerLow
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            minLines = 8
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            onScanTriggered(resumeName, resumeText, targetJob)
                        },
                        enabled = !isScanning,
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("scan_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NavyPrimary,
                            contentColor = OnPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Run Advanced Scan",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

// ---------------- REPORT TAB SCREEN ----------------

@Composable
fun ReportScreen(
    analysis: ResumeAnalysis?,
    onSelectDifferent: () -> Unit,
    onNavigateToCoach: () -> Unit
) {
    if (analysis == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = null,
                    tint = OnSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Resume Selected for Review",
                    style = MaterialTheme.typography.headlineMedium,
                    color = OnSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Please select any scanned history card from your home tab or submit a new resume text in the Upload tab.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onSelectDifferent,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainer, contentColor = OnSurface)
                ) {
                    Text("Go back to Dashboard")
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Back toolbar row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSelectDifferent) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Go back",
                        tint = OnSurface
                    )
                }
                Text(
                    text = "ATS Scanner Report",
                    style = MaterialTheme.typography.headlineLarge,
                    color = OnSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(48.dp)) // Equalizer spacing
            }
        }

        // Header Metadata area
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScoreCircularMeter(
                        score = analysis.overallScore,
                        modifier = Modifier.size(96.dp)
                    )

                    Column {
                        Text(
                            text = analysis.fileName,
                            style = MaterialTheme.typography.headlineMedium,
                            color = OnSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Target Position: ${analysis.targetJob}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NavyPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatScanDate(analysis.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }
        }

        // AI Advice quick entry CTA
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PrimaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clickable { onNavigateToCoach() }
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Stars,
                        contentDescription = "AI Coach Recommendations",
                        tint = WarningAmber,
                        modifier = Modifier.size(28.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Refill gaps on your resume immediately!",
                            style = MaterialTheme.typography.labelLarge,
                            color = OnSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ask our Career AI Coach to draft custom bullet points including missing keywords.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = OnSurface
                    )
                }
            }
        }

        // Missing Keywords Section
        item {
            Text(
                text = "ATS Missing Keywords",
                style = MaterialTheme.typography.headlineMedium,
                color = OnSurface,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "These high-value tags are missing or sparsely placed inside your profile text in compared to industry criteria:",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Display individual nice pills
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                analysis.missingKeywords.split(",").forEach { keyword ->
                    if (keyword.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .border(1.dp, WarningAmber, RoundedCornerShape(16.dp))
                                .background(WarningContainer.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = keyword.trim(),
                                color = OnWarningContainer,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Checklist Layout
        item {
            Text(
                text = "Format & Parser Compliance",
                style = MaterialTheme.typography.headlineMedium,
                color = OnSurface,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        // Passed Items Checklist
        items(analysis.formattingPassed.split(",")) { checkText ->
            if (checkText.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Pass",
                        tint = SuccessGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = checkText.trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurface
                    )
                }
            }
        }

        // Failed Items Checklist
        items(analysis.formattingFailed.split(",")) { checkText ->
            if (checkText.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "Fail / Warning",
                        tint = ErrorRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = checkText.trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                }
            }
        }

        // Strengths & Weaknesses Detailed Bento Cards
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Strengths
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ThumbUp, contentDescription = null, tint = SuccessGreen)
                            Text(
                                text = "Strategic Strengths",
                                style = MaterialTheme.typography.labelLarge,
                                color = SuccessGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        analysis.strengths.split(",").forEach { strength ->
                            if (strength.isNotBlank()) {
                                Text(
                                    text = "• ${strength.trim()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OnSurface,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Weaknesses
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ThumbDown, contentDescription = null, tint = ErrorRed)
                            Text(
                                text = "Areas of Improvement",
                                style = MaterialTheme.typography.labelLarge,
                                color = ErrorRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        analysis.weaknesses.split(",").forEach { weakness ->
                            if (weakness.isNotBlank()) {
                                Text(
                                    text = "• ${weakness.trim()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OnSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Detailed Coach Tactical Actions List
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Tactical Road to 90+ Score",
                        style = MaterialTheme.typography.headlineMedium,
                        color = OnSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = analysis.recommendations,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurface,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

// FlowRow layout helper for keyword pill placement
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = { content() }
    )
}

// ---------------- ADVICE / COACH CHAT TAB SCREEN ----------------

@Composable
fun CoachScreen(viewModel: MainViewModel) {
    val chatHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
    val chatLoading by viewModel.chatLoading.collectAsStateWithLifecycle()
    val selectedAnalysis by viewModel.selectedAnalysis.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    var userMessageText by remember { mutableStateOf("") }

    // Scroll chat list to bottom reactively when new messages drop
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(bottom = 12.dp)
    ) {
        // Welcome and Brief Context banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(PrimaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "Coach Lamp Icon",
                    tint = WarningAmber,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = "AI Career Advisor",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = OnSurface
                )
                Text(
                    text = if (selectedAnalysis != null) {
                        "Analyzing Coach Context: ${selectedAnalysis!!.targetJob}"
                    } else {
                        "Analyzing General Career Coach Context"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }
        }

        Divider(color = OutlineVariant.copy(alpha = 0.2f))

        // Large scrollable Chat Panel area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(chatHistory) { message ->
                    CoachMessageBubble(message = message)
                }

                if (chatLoading) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        color = NavyPrimary,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Typing...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Suggestions/Preset prompts inside Coach tab
        val presets = listOf(
            "Draft interview questions",
            "Rewrite achievements",
            "Help with cover letter"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { text ->
                InputChip(
                    selected = false,
                    onClick = {
                        viewModel.sendChatMessage(text)
                    },
                    label = { Text(text, style = MaterialTheme.typography.labelMedium) },
                    colors = InputChipDefaults.inputChipColors(
                        containerColor = SurfaceContainerHigh.copy(alpha = 0.6f),
                        labelColor = OnSurface
                    )
                )
            }
        }

        // Text input field bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = userMessageText,
                onValueChange = { userMessageText = it },
                placeholder = { Text("Ask your career coach advisor...", color = OnSurfaceVariant.copy(alpha = 0.5f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NavyPrimary,
                    unfocusedBorderColor = OutlineVariant,
                    focusedContainerColor = SurfaceContainerLow,
                    unfocusedContainerColor = SurfaceContainerLow
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (userMessageText.isNotBlank()) {
                        viewModel.sendChatMessage(userMessageText)
                        userMessageText = ""
                        keyboardController?.hide()
                    }
                })
            )

            FloatingActionButton(
                onClick = {
                    if (userMessageText.isNotBlank()) {
                        viewModel.sendChatMessage(userMessageText)
                        userMessageText = ""
                        keyboardController?.hide()
                    }
                },
                containerColor = NavyPrimary,
                contentColor = OnPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("chat_send_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send message",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun CoachMessageBubble(message: ChatMessage) {
    val isUser = message.sender == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = if (isUser) {
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 0.dp)
            } else {
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 16.dp)
            },
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) PrimaryContainer else SurfaceContainerLow
            ),
            border = if (isUser) null else BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Sender label
                Text(
                    text = if (isUser) "You" else "Career Coach AI",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isUser) NavyPrimary else WarningAmber,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Content text
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurface,
                    lineHeight = 22.sp
                )
            }
        }
    }
}
