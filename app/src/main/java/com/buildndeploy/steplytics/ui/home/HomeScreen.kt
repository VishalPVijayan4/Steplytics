package com.buildndeploy.steplytics.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buildndeploy.steplytics.domain.model.UserProfile
import com.buildndeploy.steplytics.ui.theme.AppBackground
import com.buildndeploy.steplytics.ui.theme.CardBackground
import com.buildndeploy.steplytics.ui.theme.CardBorder
import com.buildndeploy.steplytics.ui.theme.PrimaryBlue
import com.buildndeploy.steplytics.ui.theme.PrimaryGreen
import com.buildndeploy.steplytics.ui.theme.TextSecondary

private enum class DashboardTab(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Home("Home", Icons.Outlined.Home),
    Calendar("Calendar", Icons.Outlined.CalendarMonth),
    Reports("Reports", Icons.Outlined.QueryStats),
    Profile("Profile", Icons.Outlined.PersonOutline)
}

private data class StatCardUi(
    val title: String,
    val value: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val background: Color
)

@Composable
fun HomeScreen(
    profile: UserProfile?,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(DashboardTab.Home) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppBackground,
        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                DashboardTab.Home -> DashboardOverviewScreen(profile = profile)
                DashboardTab.Calendar -> CalendarScreen()
                DashboardTab.Reports -> ReportsScreen()
                DashboardTab.Profile -> ProfileScreen(profile = profile)
            }
        }
    }
}

@Composable
private fun DashboardOverviewScreen(profile: UserProfile?) {
    val headerName = "Welcome Back!"
    val cards = listOf(
        StatCardUi("Steps", "0", "steps", Icons.Outlined.Straighten, Color(0xFF172446)),
        StatCardUi("Calories", "0", "kcal", Icons.Outlined.Whatshot, Color(0xFF112D32)),
        StatCardUi("Distance", "0.0", "km", Icons.Outlined.Timelapse, Color(0xFF2A1A3A))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = headerName,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Let's crush your goals today",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )

        StartActivityButton()

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            cards.forEach { card ->
                MetricCard(
                    card = card,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        ChartContainer(
            title = "Weekly Progress",
            trailingLabel = "Last 7 days"
        ) {
            LineChart(
                points = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f),
                labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun CalendarScreen() {
    val highlightedDays = setOf(3, 8, 15, 22, 28)
    val days = (1..31).toList()
    val leadingBlanks = 3

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Activity Calendar",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        SurfaceCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CalendarArrow("‹")
                Text(
                    text = "March 2026",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                CalendarArrow("›")
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                    Text(
                        text = day,
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(32.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            repeat(5) { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(7) { dayIndex ->
                        val cellIndex = week * 7 + dayIndex
                        val dayNumber = cellIndex - leadingBlanks + 1
                        if (dayNumber in days) {
                            DayBubble(
                                day = dayNumber,
                                highlighted = dayNumber in highlightedDays,
                                selected = dayNumber == 15
                            )
                        } else {
                            Spacer(modifier = Modifier.size(36.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        SurfaceCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "March 15, 2026",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "No Activity Yet",
                    color = PrimaryGreen,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            StatsGrid(
                items = listOf(
                    "0" to "Steps",
                    "0.0 km" to "Distance",
                    "0 kcal" to "Calories",
                    "0 min" to "Duration"
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun ReportsScreen() {
    var selectedRange by remember { mutableStateOf("Weekly") }
    val ranges = listOf("Daily", "Weekly", "Monthly")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Performance Reports",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground, RoundedCornerShape(18.dp))
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ranges.forEach { range ->
                val selected = range == selectedRange
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            brush = if (selected) Brush.horizontalGradient(listOf(PrimaryBlue, PrimaryGreen)) else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { selectedRange = range }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = range,
                        color = if (selected) Color.White else TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        StatsGrid(
            items = listOf(
                "0" to "Avg Steps",
                "0" to "Avg Calories",
                "0.0 km" to "Total Distance",
                "0/7" to "Active Days"
            ),
            useFullWidth = true
        )

        ChartContainer(title = "Steps Overview") {
            BarChart(
                values = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f),
                labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            )
        }

        ChartContainer(title = "Calories Burned") {
            LineChart(
                points = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f),
                labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun ProfileScreen(profile: UserProfile?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        SurfaceCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            brush = Brush.horizontalGradient(listOf(PrimaryBlue, PrimaryGreen)),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PersonOutline,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Steplytics User",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = profile?.let { "${it.age} yrs • ${it.height} cm • ${it.weight} kg" }
                            ?: "Complete your first activity to unlock insights.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF0D5C53), RoundedCornerShape(999.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "New Member",
                            color = PrimaryGreen,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            StatsGrid(
                items = listOf(
                    "0" to "Total Workouts",
                    "0 km" to "Total Distance",
                    "0" to "Total Calories",
                    "0" to "Active Days"
                ),
                useFullWidth = true
            )
        }

        SectionTitle("Preferences")
        PreferenceList(
            rows = listOf(
                Triple(Icons.Outlined.Settings, "Units", "Metric (km, kg)"),
                Triple(Icons.Outlined.NotificationsNone, "Notifications", "Enabled")
            )
        )

        SectionTitle("Data")
        PreferenceList(
            rows = listOf(
                Triple(Icons.Outlined.Download, "Export Data", "No data available yet")
            )
        )

        SurfaceCard {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(
                            brush = Brush.horizontalGradient(listOf(PrimaryBlue, PrimaryGreen)),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "App Version",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Steplytics v1.0.0",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Last updated: March 2026",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun StartActivityButton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(listOf(PrimaryBlue, PrimaryGreen)),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(Color.White.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = "Start Activity",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MetricCard(
    card: StatCardUi,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(card.background, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = card.icon,
            contentDescription = null,
            tint = PrimaryBlue,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = card.value,
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = card.title,
                color = TextSecondary,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = card.subtitle,
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ChartContainer(
    title: String,
    trailingLabel: String? = null,
    content: @Composable () -> Unit
) {
    SurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (trailingLabel != null) {
                Text(
                    text = trailingLabel,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        content()
    }
}

@Composable
private fun LineChart(
    points: List<Float>,
    labels: List<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            if (points.isEmpty()) return@Canvas
            val maxValue = (points.maxOrNull() ?: 0f).coerceAtLeast(1f)
            val stepX = if (points.size == 1) size.width else size.width / (points.size - 1)
            val path = Path()
            val fillPath = Path()

            points.forEachIndexed { index, value ->
                val x = index * stepX
                val normalized = value / maxValue
                val y = size.height - (normalized * (size.height * 0.7f) + size.height * 0.15f)
                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, size.height)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }
            fillPath.lineTo(size.width, size.height)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(PrimaryBlue.copy(alpha = 0.32f), Color.Transparent)
                )
            )
            drawPath(
                path = path,
                color = PrimaryBlue,
                style = Stroke(width = 6f, cap = StrokeCap.Round)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEach { label ->
                Text(text = label, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun BarChart(
    values: List<Float>,
    labels: List<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val maxValue = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
            val barWidth = size.width / (values.size * 1.6f)
            val spacing = barWidth * 0.6f
            values.forEachIndexed { index, value ->
                val left = index * (barWidth + spacing) + spacing
                val barHeight = (value / maxValue) * (size.height * 0.8f)
                drawRoundRect(
                    color = PrimaryBlue,
                    topLeft = Offset(left, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(16f, 16f)
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEach { label ->
                Text(text = label, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(
    selectedTab: DashboardTab,
    onTabSelected: (DashboardTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0E1426))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DashboardTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            Column(
                modifier = Modifier
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            if (selected) PrimaryBlue.copy(alpha = 0.2f) else Color.Transparent,
                            CircleShape
                        )
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (selected) PrimaryBlue else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = tab.label,
                    color = if (selected) PrimaryBlue else TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun StatsGrid(
    items: List<Pair<String, String>>,
    useFullWidth: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { (value, label) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF141C31), RoundedCornerShape(18.dp))
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = value,
                            color = PrimaryBlue,
                            style = if (useFullWidth) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = label,
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SurfaceCard(content: @Composable Column.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        content = content
    )
}

@Composable
private fun CalendarArrow(symbol: String) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(Color(0xFF1A2137), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text = symbol, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    }
}

@Composable
private fun DayBubble(
    day: Int,
    highlighted: Boolean,
    selected: Boolean
) {
    Box(
        modifier = Modifier.size(36.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            selected -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.horizontalGradient(listOf(PrimaryBlue, PrimaryGreen)), CircleShape)
            )

            highlighted -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PrimaryBlue.copy(alpha = 0.35f), CircleShape)
            )
        }
        Text(
            text = day.toString(),
            color = if (selected || highlighted) Color.White else TextSecondary,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.widthIn(min = 24.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = TextSecondary,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun PreferenceList(
    rows: List<Triple<androidx.compose.ui.graphics.vector.ImageVector, String, String>>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(24.dp))
    ) {
        rows.forEachIndexed { index, (icon, title, subtitle) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xFF16254A), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = PrimaryBlue)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Text(text = "›", color = TextSecondary, fontSize = 24.sp)
            }
            if (index != rows.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(CardBorder)
                )
            }
        }
    }
}
