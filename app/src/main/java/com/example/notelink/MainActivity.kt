package com.example.notelink

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notelink.data.local.AppDatabase
import com.example.notelink.data.model.Link
import com.example.notelink.ui.LinkViewModel
import com.example.notelink.ui.LinkViewModelFactory
import com.example.notelink.ui.BrowserScreen
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.StarBorder
import com.example.notelink.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        val database = AppDatabase.getDatabase(this)
        val linkDao = database.linkDao()
        
        setContent {
            NotelinkTheme {
                val viewModel: LinkViewModel = viewModel(
                    factory = LinkViewModelFactory(linkDao)
                )
                
                val links by viewModel.links.collectAsStateWithLifecycle()
                var currentUrl by rememberSaveable { mutableStateOf<String?>(null) }
                
                // Track last non-null URL to prevent crash during Crossfade exit transition
                var lastUrl by rememberSaveable { mutableStateOf<String?>(null) }
                SideEffect {
                    if (currentUrl != null) {
                        lastUrl = currentUrl
                    }
                }
                
                val isBrowserOpen = currentUrl != null

                Crossfade(targetState = isBrowserOpen, label = "ScreenTransition") { isOpen ->
                    if (!isOpen) {
                        LinkScreen(
                            viewModel = viewModel,
                            links = links,
                            onLoginClick = { 
                                Log.d("NotelinkNav", "Navigating to Login")
                                currentUrl = "https://accounts.google.com/" 
                            },
                            onLinkClick = { selectedUrl -> 
                                Log.d("NotelinkNav", "Navigating to URL: $selectedUrl")
                                currentUrl = selectedUrl 
                            }
                        )
                    } else {
                        // Use lastUrl to ensure BrowserScreen has a valid URL during fade-out
                        BrowserScreen(
                            url = lastUrl ?: "",
                            allLinks = links,
                            onNavigate = { currentUrl = it },
                            onBack = { currentUrl = null }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LinkScreen(
    viewModel: LinkViewModel,
    links: List<Link>,
    onLoginClick: () -> Unit,
    onLinkClick: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var editingLink by remember { mutableStateOf<Link?>(null) }
    var linkToDelete by remember { mutableStateOf<Link?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var isNewestFirst by remember { mutableStateOf(true) }

    val dynamicCategories = remember(links) {
        val distinct = links.map { it.category }.distinct().filter { it.isNotBlank() }
        listOf("All") + distinct.sorted()
    }
    
    val filteredLinks = remember(links, selectedCategory, searchQuery, isNewestFirst) {
        links.filter { 
            (selectedCategory == "All" || it.category == selectedCategory) &&
            (it.name.contains(searchQuery, ignoreCase = true) || it.url.contains(searchQuery, ignoreCase = true))
        }.sortedByDescending { if (isNewestFirst) it.id else -it.id }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBlue,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    editingLink = null
                    showDialog = true 
                },
                containerColor = CyanAccent,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(32.dp))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // Header: Profile + Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Avatar with Settings Gear
                Box(contentAlignment = Alignment.BottomEnd) {
                    Surface(
                        onClick = onLoginClick,
                        shape = RoundedCornerShape(12.dp),
                        color = ProfileRed,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "G", 
                                color = Color.White, 
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = DarkBlue,
                        modifier = Modifier.size(16.dp).offset(x = 4.dp, y = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings, 
                            contentDescription = null, 
                            tint = GreyText,
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                }
                
                Spacer(Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = "NOTELINK",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "Smart Browser & Link Manager",
                        style = MaterialTheme.typography.bodySmall,
                        color = GreyText
                    )
                }
            }

            // Category Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dynamicCategories.forEach { category ->
                    val isSelected = selectedCategory == category
                    Surface(
                        onClick = { selectedCategory = category },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) CyanAccent.copy(alpha = 0.1f) else Color.Transparent,
                        border = if (isSelected) BorderStroke(1.dp, CyanAccent) else BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(
                                text = category,
                                color = if (isSelected) CyanAccent else Color.White,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                placeholder = { Text("Cari bookmark atau URL...", color = GreyText) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GreyText) },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DeepBlue,
                    unfocusedContainerColor = DeepBlue,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                ),
                singleLine = true
            )

            // Section Header: NOTE LINK + Sort
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bookmark, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "NOTE LINK", 
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = GreyText
                    )
                }
                
                Surface(
                    onClick = { isNewestFirst = !isNewestFirst },
                    shape = RoundedCornerShape(8.dp),
                    color = DeepBlue,
                    modifier = Modifier.height(32.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isNewestFirst) "Newest" else "Oldest", 
                            style = MaterialTheme.typography.labelMedium, 
                            color = Color.White
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isNewestFirst) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, 
                            contentDescription = null, 
                            tint = CyanAccent, 
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            if (filteredLinks.isEmpty()) {
                EmptyStateView()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(items = filteredLinks, key = { it.id }) { link ->
                        LinkItem(
                            link = link,
                            onClick = { onLinkClick(link.url) },
                            onEdit = { 
                                editingLink = link
                                showDialog = true
                            },
                            onDelete = { linkToDelete = link }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        LinkDialog(
            link = editingLink,
            onDismiss = { showDialog = false },
            onConfirm = { name, url, category ->
                if (editingLink == null) {
                    viewModel.addLink(name, url, category)
                } else {
                    viewModel.updateLink(editingLink!!.copy(name = name, url = url, category = category))
                }
                showDialog = false
            }
        )
    }

    if (linkToDelete != null) {
        AlertDialog(
            onDismissRequest = { linkToDelete = null },
            title = { Text("Hapus Link") },
            text = { Text("Apakah Anda yakin ingin menghapus link '${linkToDelete?.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteLink(linkToDelete!!)
                        linkToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { linkToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun EmptyStateView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "No links found",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            )
            Text(
                text = "Tap + to add your first link",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun LinkItem(link: Link, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        color = DeepBlue,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon Placeholder (Matching AI Studio / Gemini style)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkBlue),
                contentAlignment = Alignment.Center
            ) {
                // If it's Gemini, use a gradient-like icon
                if (link.url.contains("gemini")) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(32.dp)
                    )
                } else if (link.url.contains("ai.studio")) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = GreyText,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = link.name, 
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = link.url.replace("https://", "").replace("http://", ""), 
                    style = MaterialTheme.typography.bodySmall, 
                    color = GreyText,
                    maxLines = 1
                )
                
                Spacer(Modifier.height(8.dp))
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CyanAccent.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = link.category, 
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = CyanAccent
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Edit, 
                        contentDescription = "Edit",
                        tint = CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Outlined.DeleteOutline, 
                        contentDescription = "Delete",
                        tint = DeleteRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LinkDialog(
    link: Link?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(link?.name ?: "") }
    var url by remember { mutableStateOf(link?.url ?: "") }
    var category by remember { mutableStateOf(link?.category ?: "Common") } // Default ke "Common"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (link == null) "Add New Link" else "Edit Link") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, url, category) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
