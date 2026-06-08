package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.BrowserViewModel
import com.example.ui.ChatMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPanel(
    viewModel: BrowserViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val aiMessages by viewModel.aiMessages.collectAsState()
    val summary by viewModel.aiWebpageSummary.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val pageTitle by viewModel.pageTitle.collectAsState()

    var inputMsg by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    var activeTabSubIndex by remember { mutableIntStateOf(0) } // 0 = Chat, 1 = Summary, 2 = Translate

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // Co-Pilot Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = "AI CoPilot",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Gemini Assistant",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close Assistant")
            }
        }

        // Navigation Mode Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val modes = listOf("Co-pilot Chat", "Summarizer", "Quick Translate")
            modes.forEachIndexed { idx, title ->
                val isSelected = activeTabSubIndex == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { activeTabSubIndex = idx }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // AI processing Loader bar
        AnimatedVisibility(visible = isAiLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Active Dashboard Sections
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (activeTabSubIndex) {
                0 -> { // Chat Client Content
                    Column(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            reverseLayout = false
                        ) {
                            items(aiMessages) { msg ->
                                ChatBubble(msg)
                            }
                        }

                        // Quick AI Action Suggestion Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val promptChips = listOf(
                                "Summarize Page" to "Summarize this page in a concise bullet-point summary.",
                                "Explain Key Terms" to "List and explain the main ideas and technical terms mentioned here.",
                                "Is this link safe?" to "Review the current webpage address and tell me if there are any privacy or security risks."
                            )
                            promptChips.forEach { (label, actionPrompt) ->
                                AssistChip(
                                    onClick = { viewModel.sendCopilotMessage(actionPrompt) },
                                    label = { Text(label, fontSize = 10.sp) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Chat entry box
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inputMsg,
                                onValueChange = { inputMsg = it },
                                placeholder = { Text("Ask question...", fontSize = 13.sp) },
                                modifier = Modifier.weight(1f),
                                maxLines = 3,
                                textStyle = MaterialTheme.typography.bodyMedium,
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    imeAction = ImeAction.Send
                                ),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (inputMsg.isNotBlank()) {
                                            viewModel.sendCopilotMessage(inputMsg)
                                            inputMsg = ""
                                            keyboardController?.hide()
                                        }
                                    }
                                ),
                                shape = RoundedCornerShape(26.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.Transparent,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            FloatingActionButton(
                                onClick = {
                                    if (inputMsg.isNotBlank()) {
                                        viewModel.sendCopilotMessage(inputMsg)
                                        inputMsg = ""
                                        keyboardController?.hide()
                                    }
                                },
                                shape = RoundedCornerShape(100.dp),
                                containerColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
                1 -> { // Page Summarizer
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                item {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                                .wrapContentSize(Alignment.Center)
                                        ) {
                                            Icon(
                                                Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Gemini Assistant",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = summary ?: "This page discusses loaded site contents. Key points can include structured breakdowns synthesized instantly.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        lineHeight = 20.sp,
                                        color = MaterialTheme.colorScheme.onSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.summarizeCurrentPage() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Icon(Icons.Default.Summarize, contentDescription = "Summarize")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Summarize Full Site")
                        }
                    }
                }
                2 -> { // Translator Configuration
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Choose Translation Target",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Redirect active tab page using Google Translate co-operative wrapper.",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        val languages = listOf(
                            "Spanish" to "es",
                            "French" to "fr",
                            "German" to "de",
                            "Chinese" to "zh-CN",
                            "Hindi" to "hi",
                            "Japanese" to "ja",
                            "Arabic" to "ar",
                            "Portuguese" to "pt"
                        )

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(languages) { (name, langCode) ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.translateCurrentPage(langCode)
                                            onClose()
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(name, fontWeight = FontWeight.SemiBold)
                                        Icon(
                                            Icons.Default.Translate,
                                            contentDescription = "Translate to $name",
                                            tint = MaterialTheme.colorScheme.primary
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
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    val containerColor = if (msg.isFromUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor = if (msg.isFromUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val horizontalAlign = if (msg.isFromUser) Alignment.End else Alignment.Start
    val shape = if (msg.isFromUser) {
        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = horizontalAlign
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
                .background(containerColor)
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(
                text = msg.text,
                fontSize = 13.sp,
                color = contentColor,
                lineHeight = 18.sp
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (msg.isFromUser) "You" else "Copilot",
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}
