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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.FrameWindowScope
import com.teamdev.jxbrowser.browser.Browser
import com.teamdev.jxbrowser.engine.Engine
import com.teamdev.jxbrowser.navigation.event.FrameLoadFinished
import com.teamdev.jxbrowser.view.compose.BrowserView
import java.util.UUID

/**
 * Browser tab data
 */
data class BrowserTabData(
    val id: String = UUID.randomUUID().toString(),
    val browser: Browser,
    var title: String = "New Tab"
)

/**
 * MDI split-screen browser tab content
 * Uses SplitPane style layout with independent TabPane on left and right
 */
@Composable
fun FrameWindowScope.MdiSplitTabContent(
    engine: Engine,
    onStatusChange: (String) -> Unit
) {
    // Left tab list
    val leftTabs = remember { mutableStateListOf<BrowserTabData>() }
    // Right tab list
    val rightTabs = remember { mutableStateListOf<BrowserTabData>() }
    // Currently selected tab index on the left
    var leftSelectedIndex by remember { mutableStateOf(0) }
    // Currently selected tab index on the right
    var rightSelectedIndex by remember { mutableStateOf(0) }
    // Split ratio (0.0 ~ 1.0)
    var splitRatio by remember { mutableStateOf(0.5f) }

    // Initialize default tabs
    LaunchedEffect(Unit) {
        if (leftTabs.isEmpty()) {
            val leftBrowser = engine.newBrowser()
            leftBrowser.navigation().loadUrl("https://www.google.com")
            leftTabs.add(BrowserTabData(browser = leftBrowser, title = "Google"))
            setupTitleListener(leftBrowser, leftTabs)
        }
        if (rightTabs.isEmpty()) {
            val rightBrowser = engine.newBrowser()
            rightBrowser.navigation().loadUrl("https://teamdev.com/")
            rightTabs.add(BrowserTabData(browser = rightBrowser, title = "TeamDev"))
            setupTitleListener(rightBrowser, rightTabs)
        }
        onStatusChange("MDI split-screen browser loaded")
    }

    // Cleanup resources
    DisposableEffect(Unit) {
        onDispose {
            leftTabs.forEach { it.browser.close() }
            rightTabs.forEach { it.browser.close() }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Toolbar
        MdiToolBar(
            onAddLeft = {
                val browser = engine.newBrowser()
                browser.navigation().loadUrl("https://www.google.com")
                val tab = BrowserTabData(browser = browser, title = "Google")
                leftTabs.add(tab)
                leftSelectedIndex = leftTabs.size - 1
                setupTitleListener(browser, leftTabs)
                onStatusChange("Added new tab to left")
            },
            onAddRight = {
                val browser = engine.newBrowser()
                browser.navigation().loadUrl("https://www.google.com")
                val tab = BrowserTabData(browser = browser, title = "Google")
                rightTabs.add(tab)
                rightSelectedIndex = rightTabs.size - 1
                setupTitleListener(browser, rightTabs)
                onStatusChange("Added new tab to right")
            },
            onCloseAll = {
                leftTabs.forEach { it.browser.close() }
                rightTabs.forEach { it.browser.close() }
                leftTabs.clear()
                rightTabs.clear()
                leftSelectedIndex = 0
                rightSelectedIndex = 0
                onStatusChange("All tabs closed")
            }
        )

        // Split area
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Left panel
            BrowserTabPane(
                tabs = leftTabs,
                selectedIndex = leftSelectedIndex,
                onTabSelected = { leftSelectedIndex = it },
                onTabClose = { index ->
                    if (index < leftTabs.size) {
                        leftTabs[index].browser.close()
                        leftTabs.removeAt(index)
                        if (leftSelectedIndex >= leftTabs.size) {
                            leftSelectedIndex = (leftTabs.size - 1).coerceAtLeast(0)
                        }
                        onStatusChange("Closed left tab")
                    }
                },
                modifier = Modifier.weight(splitRatio).fillMaxHeight()
            )

            // Draggable divider
            DraggableDivider(
                onDrag = { delta ->
                    splitRatio = (splitRatio + delta).coerceIn(0.2f, 0.8f)
                }
            )

            // Right panel
            BrowserTabPane(
                tabs = rightTabs,
                selectedIndex = rightSelectedIndex,
                onTabSelected = { rightSelectedIndex = it },
                onTabClose = { index ->
                    if (index < rightTabs.size) {
                        rightTabs[index].browser.close()
                        rightTabs.removeAt(index)
                        if (rightSelectedIndex >= rightTabs.size) {
                            rightSelectedIndex = (rightTabs.size - 1).coerceAtLeast(0)
                        }
                        onStatusChange("Closed right tab")
                    }
                },
                modifier = Modifier.weight(1f - splitRatio).fillMaxHeight()
            )
        }

        // Status bar
        MdiStatusBar(
            leftCount = leftTabs.size,
            rightCount = rightTabs.size
        )
    }
}

/**
 * Setup page title listener
 */
private fun setupTitleListener(browser: Browser, tabs: MutableList<BrowserTabData>) {
    browser.navigation().on(FrameLoadFinished::class.java) { event ->
        if (event.frame().isMain) {
            val title = browser.title()
            if (!title.isNullOrEmpty()) {
                val tab = tabs.find { it.browser == browser }
                tab?.let {
                    val index = tabs.indexOf(it)
                    if (index >= 0) {
                        tabs[index] = it.copy(title = if (title.length > 20) title.take(20) + "..." else title)
                    }
                }
            }
        }
    }
}

/**
 * MDI toolbar
 */
@Composable
private fun MdiToolBar(
    onAddLeft: () -> Unit,
    onAddRight: () -> Unit,
    onCloseAll: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onAddLeft,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Add left side", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = onAddRight,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Add right side", fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.weight(1f))

            TextButton(
                onClick = onCloseAll,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("✖ Close all", fontSize = 12.sp)
            }
        }
    }
}

/**
 * Browser tab panel
 */
@Composable
private fun FrameWindowScope.BrowserTabPane(
    tabs: List<BrowserTabData>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    onTabClose: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.background(Color(50, 50, 50))) {
        // Tab bar
        if (tabs.isNotEmpty()) {
            ScrollableTabRow(
                selectedTabIndex = selectedIndex.coerceIn(0, (tabs.size - 1).coerceAtLeast(0)),
                containerColor = Color(60, 60, 60),
                contentColor = Color.White,
                edgePadding = 0.dp,
                divider = {}
            ) {
                tabs.forEachIndexed { index, tab ->
                    ClosableTab(
                        title = tab.title,
                        selected = index == selectedIndex,
                        onSelect = { onTabSelected(index) },
                        onClose = { onTabClose(index) }
                    )
                }
            }

            // Currently selected BrowserView
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                val currentTab = tabs.getOrNull(selectedIndex.coerceIn(0, (tabs.size - 1).coerceAtLeast(0)))
                currentTab?.let {
                    BrowserView(it.browser)
                }
            }
        } else {
            // Placeholder when no tabs
            Box(
                modifier = Modifier.fillMaxSize().background(Color(70, 70, 70)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tabs",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * Closable tab
 */
@Composable
private fun ClosableTab(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    val backgroundColor = if (selected) Color(80, 80, 80) else Color(60, 60, 60)

    Surface(
        onClick = onSelect,
        color = backgroundColor,
        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 120.dp)
            )

            // Close button
            Surface(
                onClick = onClose,
                color = Color.Transparent,
                shape = RoundedCornerShape(2.dp)
            ) {
                Text(
                    text = "✕",
                    color = Color(180, 180, 180),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(2.dp)
                )
            }
        }
    }
}

/**
 * Draggable divider
 */
@Composable
private fun DraggableDivider(
    onDrag: (Float) -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    val dividerColor = if (isDragging) MaterialTheme.colorScheme.primary else Color(100, 100, 100)

    Box(
        modifier = Modifier
            .width(6.dp)
            .fillMaxHeight()
            .background(dividerColor)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        // Convert pixels to ratio
                        val parentWidth = size.width.toFloat()
                        if (parentWidth > 0) {
                            onDrag(dragAmount.x / 1000f) // Adjust sensitivity
                        }
                    }
                )
            }
    )
}

/**
 * MDI status bar
 */
@Composable
private fun MdiStatusBar(
    leftCount: Int,
    rightCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Left tabs: $leftCount",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Right tabs: $rightCount",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

