/*
 *  Copyright 2025, TeamDev. All rights reserved.
 *
 *  Redistribution and use in source and/or binary forms, with or without
 *  modification, must retain the above copyright notice and the following
 *  disclaimer.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.teamdev.jxbrowser.quickstart.gradle.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import com.teamdev.jxbrowser.browser.Browser
import com.teamdev.jxbrowser.dsl.Engine
import com.teamdev.jxbrowser.dsl.browser.navigation
import com.teamdev.jxbrowser.engine.Engine
import com.teamdev.jxbrowser.engine.RenderingMode.OFF_SCREEN
import com.teamdev.jxbrowser.view.compose.BrowserView

/**
 * Tab page data
 */
data class TabItem(
    val title: String,
    val content: @Composable () -> Unit
)

/**
 * This example demonstrates how to embed a BrowserView component
 * into a Compose Desktop application with Tab navigation.
 */
fun main() {
    // Initialize Chromium.
    val engine = Engine(OFF_SCREEN)

    // Create a Browser instance for the first tab.
    val browser = engine.newBrowser()

    singleWindowApplication(
        title = "Compose Desktop BrowserView",
        state = WindowState(width = 1200.dp, height = 800.dp),
    ) {
        // Pass WindowScope and Engine to App content
        AppContent(
            engine = engine,
            browser = browser,
            windowScope = this@singleWindowApplication
        )

        DisposableEffect(Unit) {
            browser.navigation.loadUrl("https://www.google.com")
            onDispose {
                // Shutdown Chromium and release allocated resources.
                engine.close()
            }
        }
    }
}

/**
 * Main application content
 */
@Composable
fun AppContent(
    engine: Engine,
    browser: Browser,
    windowScope: FrameWindowScope
) {
    MaterialTheme {
        // Currently selected tab index
        var selectedTabIndex by remember { mutableStateOf(0) }
        // Status bar message
        var statusMessage by remember { mutableStateOf("Ready") }

        // Define tab pages
        val tabs = listOf(
            TabItem("Browser") {},
            TabItem("MDI Browser") {}
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Tab bar
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                indicator = {},  // Remove default underline indicator
                divider = {}     // Remove default divider
            ) {
                tabs.forEachIndexed { index, tab ->
                    val selected = selectedTabIndex == index
                    Tab(
                        selected = selected,
                        onClick = {
                            selectedTabIndex = index
                            statusMessage = "Switched to tab: ${tab.title}"
                        },
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                            .background(
                                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (index == 0) " " else " ",
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = tab.title,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    )
                }
            }

            // Tab content area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTabIndex) {
                    0 -> {
                        // First tab: BrowserView + floating toolbar
                        // Use with(windowScope) to provide WindowScope context
                        with(windowScope) {
                            BrowserTabContent(
                                browser = browser,
                                onStatusChange = { statusMessage = it }
                            )
                        }
                    }
                    1 -> {
                        // Second tab: MDI split-screen browser
                        with(windowScope) {
                            MdiSplitTabContent(
                                engine = engine,
                                onStatusChange = { statusMessage = it }
                            )
                        }
                    }
                }
            }

            // Status bar
            StatusBar(message = statusMessage)
        }
    }
}

/**
 * Browser tab content: BrowserView + floating toolbar
 * As a FrameWindowScope extension function to correctly call BrowserView
 */
@Composable
fun FrameWindowScope.BrowserTabContent(
    browser: Browser,
    onStatusChange: (String) -> Unit
) {
    // Floating toolbar position state
    var offsetX by remember { mutableStateOf(20f) }
    var offsetY by remember { mutableStateOf(20f) }

    LaunchedEffect(Unit) {
        onStatusChange("Browser page loaded (OFF_SCREEN mode)")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Bottom layer: BrowserView (can be correctly called in WindowScope context)
        BrowserView(browser)

        // Top layer: Floating toolbar (using graphicsLayer for translation)
        FloatingToolPanel(
            browser = browser,
            offsetX = offsetX,
            offsetY = offsetY,
            onDrag = { dragX, dragY ->
                offsetX += dragX
                offsetY += dragY
            },
            onStatusChange = onStatusChange
        )
    }
}

/**
 * Draggable floating tool panel
 * Demonstrates that Compose components can be displayed on top of BrowserView in OFF_SCREEN mode
 */
@Composable
fun FloatingToolPanel(
    browser: Browser,
    offsetX: Float,
    offsetY: Float,
    onDrag: (Float, Float) -> Unit,
    onStatusChange: (String) -> Unit
) {
    // Dark semi-transparent background
    val panelBackground = Color(45, 45, 45, 230)
    val titleBarBackground = Color(60, 60, 60)
    val borderColor = Color(100, 100, 100)

    Surface(
        modifier = Modifier
            .graphicsLayer {
                translationX = offsetX
                translationY = offsetY
            }
            .width(220.dp)
            .wrapContentHeight()
            .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = panelBackground,
        shadowElevation = 8.dp
    ) {
        Column {
            // Title bar (draggable area)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(titleBarBackground, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.x, dragAmount.y)
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Floating Toolbar",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "(Draggable)",
                        color = Color(150, 150, 150),
                        fontSize = 10.sp
                    )
                }
            }

            // Tool button area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // First row of buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ToolButton(
                        text = "⬅ Back",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (browser.navigation().canGoBack()) {
                                browser.navigation().goBack()
                                onStatusChange("Navigated back")
                            }
                        }
                    )
                    ToolButton(
                        text = "➡ Forward",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (browser.navigation().canGoForward()) {
                                browser.navigation().goForward()
                                onStatusChange("Navigated forward")
                            }
                        }
                    )
                }

                // Second row of buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ToolButton(
                        text = "Reload",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            browser.navigation().reload()
                            onStatusChange("Page reloaded")
                        }
                    )
                    ToolButton(
                        text = "Home",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            browser.navigation().loadUrl("https://www.google.com")
                            onStatusChange("Navigated to home")
                        }
                    )
                }
            }

//            // Hint information
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(horizontal = 8.dp, vertical = 6.dp)
//            ) {
//                Text(
//                    text = "💡 In OFF_SCREEN mode, Compose components can be stacked on top of BrowserView",
//                    color = Color(180, 180, 180),
//                    fontSize = 9.sp,
//                    lineHeight = 12.sp
//                )
//            }
        }
    }
}

/**
 * Tool button
 */
@Composable
fun ToolButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(70, 70, 70),
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp
        )
    }
}

/**
 * Status bar component
 */
@Composable
fun StatusBar(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "●",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
