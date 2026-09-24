package dev.ericferguson.watertracker.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import dev.ericferguson.watertracker.R

private enum class Tab(val label: String, @param:DrawableRes val icon: Int) {
    TODAY("Today", R.drawable.ic_water_drop),
    HISTORY("History", R.drawable.ic_bar_chart),
}

/**
 * Bottom-bar navigation between the two tabs, with Settings shown full screen on top.
 * With this few screens, saved state is simpler than a navigation library; each screen
 * draws its own top bar.
 */
@Composable
fun AppRoot() {
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    if (showSettings) {
        BackHandler { showSettings = false }
        SettingsScreen(onBack = { showSettings = false })
        return
    }

    // Back from History returns to Today instead of closing the app.
    BackHandler(enabled = tab != Tab.TODAY) { tab = Tab.TODAY }

    Scaffold(
        // The bottom bar handles the navigation-bar inset and each screen handles the status bar.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(painterResource(item.icon), contentDescription = null) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
            when (tab) {
                Tab.TODAY -> TodayScreen(onOpenSettings = { showSettings = true })
                Tab.HISTORY -> HistoryScreen()
            }
        }
    }
}
