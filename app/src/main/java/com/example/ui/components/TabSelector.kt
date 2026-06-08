package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.BrowserViewModel
import com.example.ui.TabState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabSelector(
    viewModel: BrowserViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs by viewModel.tabs.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val groupOptions by viewModel.tabGroups.collectAsState()

    var isIncognitoModeSelected by remember { mutableStateOf(false) }
    var selectedTabForGrouping by remember { mutableStateOf<TabState?>(null) }

    val filteredTabs = tabs.filter { it.isIncognito == isIncognitoModeSelected }

    val backgroundColor = if (isIncognitoModeSelected) {
        Color(0xFF1E1332) // Cosmic private tone
    } else {
        MaterialTheme.colorScheme.surface
    }

    val contentColor = if (isIncognitoModeSelected) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier
            .background(backgroundColor)
            .padding(12.dp)
            .fillMaxSize()
    ) {
        // Tab Header Navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { isIncognitoModeSelected = false }) {
                    Icon(
                        Icons.Default.Tab,
                        contentDescription = "Standard Tabs",
                        tint = if (!isIncognitoModeSelected) MaterialTheme.colorScheme.primary else contentColor.copy(alpha = 0.5f)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${tabs.filter { !it.isIncognito }.size}",
                    fontWeight = FontWeight.Bold,
                    color = if (!isIncognitoModeSelected) MaterialTheme.colorScheme.primary else contentColor.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.width(20.dp))

                IconButton(onClick = { isIncognitoModeSelected = true }) {
                    Icon(
                        Icons.Default.PrivateConnectivity,
                        contentDescription = "Incognito Tabs",
                        tint = if (isIncognitoModeSelected) Color(0xFFFFB4AB) else contentColor.copy(alpha = 0.5f)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${tabs.filter { it.isIncognito }.size}",
                    fontWeight = FontWeight.Bold,
                    color = if (isIncognitoModeSelected) Color(0xFFFFB4AB) else contentColor.copy(alpha = 0.5f)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Add tab shortcut
                IconButton(onClick = {
                    viewModel.addNewTab(isIncognito = isIncognitoModeSelected)
                    onClose()
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Tab", tint = contentColor)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = contentColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isIncognitoModeSelected) "Incognito/Private Browser Workspace" else "Regular Workspace Tabs",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (filteredTabs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (isIncognitoModeSelected) Icons.Default.VisibilityOff else Icons.Default.Layers,
                        contentDescription = "Empty Workspace",
                        modifier = Modifier.size(64.dp),
                        tint = contentColor.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No open tabs in this profile.",
                        color = contentColor.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            // Lazy vertical grid display
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTabs, key = { it.id }) { tab ->
                    val isActive = tab.id == activeTabId
                    val borderColor = if (isActive) {
                        if (isIncognitoModeSelected) Color(0xFFFFB4AB) else MaterialTheme.colorScheme.primary
                    } else {
                        contentColor.copy(alpha = 0.15f)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .combinedClickable(
                                onClick = {
                                    viewModel.switchTab(tab.id)
                                    onClose()
                                },
                                onLongClick = {
                                    selectedTabForGrouping = tab
                                }
                            )
                            .border(
                                width = if (isActive) 2.dp else 1.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) {
                                if (isIncognitoModeSelected) Color(0xFF2E1C4E) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            } else {
                                if (isIncognitoModeSelected) Color(0xFF2D1E42) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tab.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = contentColor
                                    )
                                    Text(
                                        text = tab.url,
                                        fontSize = 10.sp,
                                        color = contentColor.copy(alpha = 0.6f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.closeTab(tab.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Cancel,
                                        contentDescription = "Close Tab",
                                        tint = contentColor.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Center content decoration (Mock favicon)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(contentColor.copy(alpha = 0.05f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Language,
                                    contentDescription = "Webpage Preview",
                                    tint = contentColor.copy(alpha = 0.2f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Dynamic Tab Group label/badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (tab.tabGroup != null) {
                                    val groupColor = Color(tab.tabGroupColor ?: MaterialTheme.colorScheme.primary.toArgb())
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(groupColor.copy(alpha = 0.2f))
                                            .border(1.dp, groupColor, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            tab.tabGroup,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = groupColor
                                        )
                                    }
                                } else {
                                    Text(
                                        "Hold to group",
                                        fontSize = 8.sp,
                                        color = contentColor.copy(alpha = 0.4f)
                                    )
                                }

                                if (isActive) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Active",
                                        tint = if (isIncognitoModeSelected) Color(0xFFFFB4AB) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add regular/incognito tab buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    viewModel.addNewTab(isIncognito = false)
                    onClose()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Tab")
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Regular Tab", fontSize = 12.sp)
            }

            Button(
                onClick = {
                    viewModel.addNewTab(isIncognito = true)
                    onClose()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4F378B) // Cosmic purple button
                )
            ) {
                Icon(Icons.Default.VisibilityOff, contentDescription = "Incognito Tab")
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Incognito Tab", fontSize = 12.sp)
            }
        }
    }

    // Tab Grouping Context Option Modal
    selectedTabForGrouping?.let { groupingTab ->
        AlertDialog(
            onDismissRequest = { selectedTabForGrouping = null },
            title = { Text("Assign Tab to Group") },
            text = {
                Column {
                    Text("Select a custom smart category tag for \"${groupingTab.title}\":")
                    Spacer(modifier = Modifier.height(12.dp))
                    // Option to ungroup
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.assignGroupToTab(groupingTab.id, null)
                                selectedTabForGrouping = null
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LayersClear, contentDescription = "Ungroup", tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("No Group (Ungroup)", color = MaterialTheme.colorScheme.error)
                    }

                    groupOptions.forEachIndexed { idx, group ->
                        val groupColor = when (idx % 4) {
                            0 -> Color(0xFF1B5E20)
                            1 -> Color(0xFF0D47A1)
                            2 -> Color(0xFFE65100)
                            else -> Color(0xFF4A148C)
                        }
                        Row(
                            modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.assignGroupToTab(groupingTab.id, group, groupColor.toArgb())
                                                selectedTabForGrouping = null
                                            }
                                            .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                                .size(16.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(groupColor)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(group, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedTabForGrouping = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
