package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.BrowserViewModel

data class DrawPath(
    val path: Path,
    val color: Color,
    val strokeWidth: Float = 8f
)

@Composable
fun ScreenshotOverlay(
    viewModel: BrowserViewModel,
    bitmap: Bitmap,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentColor by remember { mutableStateOf(Color.Red) }
    val paths = remember { mutableStateListOf<DrawPath>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }

    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.9f))
            .padding(12.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Headers Controls
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Scribble Board Annotation",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Clear drawings
                IconButton(onClick = { paths.clear() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear Scribble", tint = Color.White)
                }
                // Mock Saving Overlay
                IconButton(onClick = {
                    // In real app we could write to disk, we simulate completed feedback
                    viewModel.clearCapturedBitmap()
                    onClose()
                }) {
                    Icon(Icons.Default.Save, contentDescription = "Save Drawing", tint = Color.Green)
                }
                IconButton(onClick = {
                    viewModel.clearCapturedBitmap()
                    onClose()
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss Board", tint = Color.White)
                }
            }
        }

        // Mid drawing Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.DarkGray)
                .pointerInput(currentColor) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val p = Path().apply {
                                moveTo(startOffset.x, startOffset.y)
                            }
                            currentPath = p
                            paths.add(DrawPath(p, currentColor))
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            currentPath?.lineTo(change.position.x, change.position.y)
                        },
                        onDragEnd = {
                            currentPath = null
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Paint captured webpage bitmap to fit background
                drawImage(
                    image = bitmap.asImageBitmap(),
                    dstSize = IntSize(size.width.toInt(), size.height.toInt())
                )

                // Paint existing annotated scribble strokes
                paths.forEach { drawPath ->
                    drawPath(
                        path = drawPath.path,
                        color = drawPath.color,
                        style = Stroke(
                            width = drawPath.strokeWidth,
                            cap = StrokeCap.Round
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Annotation Pen Palette row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val colorsList = listOf(Color.Red, Color.Yellow, Color.Green, Color.Blue, Color.Magenta, Color.White)
            colorsList.forEach { col ->
                val isSelected = currentColor == col
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(col)
                        .clickable { currentColor = col }
                        .padding(2.dp)
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.4f))
                        )
                    }
                }
            }
        }
    }
}
