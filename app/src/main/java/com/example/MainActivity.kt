package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.components.AppTopBar
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ThreatModelDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.JournalViewModel

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val journalViewModel: JournalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val currentUser by authViewModel.currentUser.collectAsState()
                var selectedTab by remember { mutableIntStateOf(0) }
                var showThreatModel by remember { mutableStateOf(false) }

                // Sync current user ID to JournalViewModel for strict data isolation
                LaunchedEffect(currentUser) {
                    journalViewModel.setUserId(currentUser?.uid)
                }

                if (showThreatModel) {
                    ThreatModelDialog(onDismiss = { showThreatModel = false })
                }

                if (currentUser == null) {
                    AuthScreen(
                        onSignInPreset = { preset -> authViewModel.signInPreset(preset) },
                        onSignInCustom = { email, name -> authViewModel.signInCustom(email, name) },
                        onOpenThreatModel = { showThreatModel = true }
                    )
                } else {
                    val user = currentUser!!
                    Scaffold(
                        topBar = {
                            AppTopBar(
                                user = user,
                                onOpenThreatModel = { showThreatModel = true },
                                onSignOut = { authViewModel.signOut() }
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 4.dp
                            ) {
                                NavigationBarItem(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    icon = {
                                        Icon(
                                            imageVector = if (selectedTab == 0) Icons.Filled.EditNote else Icons.Outlined.EditNote,
                                            contentDescription = "New Reflection"
                                        )
                                    },
                                    label = { Text("New Reflection") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )

                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    icon = {
                                        Icon(
                                            imageVector = if (selectedTab == 1) Icons.Filled.MenuBook else Icons.Outlined.MenuBook,
                                            contentDescription = "Past Entries"
                                        )
                                    },
                                    label = { Text("Past Entries") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (selectedTab) {
                                0 -> DashboardScreen(
                                    user = user,
                                    viewModel = journalViewModel
                                )
                                1 -> HistoryScreen(
                                    user = user,
                                    viewModel = journalViewModel,
                                    onSelectEntryForDashboard = { entry ->
                                        journalViewModel.selectedEntry.value = entry
                                        selectedTab = 0
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
