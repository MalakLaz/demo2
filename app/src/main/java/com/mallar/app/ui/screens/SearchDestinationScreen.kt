package com.mallar.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mallar.app.data.model.SearchResult
import com.mallar.app.ui.theme.*

@Composable
fun SearchDestinationScreen(
    searchQuery: String,
    searchResults: List<SearchResult>,
    onQueryChange: (String) -> Unit,
    onStoreSelected: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {

        Column(modifier = Modifier.fillMaxSize()) {

            // Back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(TealPrimary)
                        .clickable { onNavigateBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null,
                        tint = Color.White)
                }
            }

            // Search + Results
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Color.White)
            ) {
                Column {

                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        placeholder = { Text("Where to go...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions {
                            focusManager.clearFocus()
                        },
                        singleLine = true
                    )

                    // Results list
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(searchResults) { result ->
                            SearchResultCard(
                                result = result,
                                onClick = { onStoreSelected(result.store.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultCard(
    result: SearchResult,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ✅ صورة المحل
            AsyncImage(
                model = "file:///android_asset/${result.store.logoColor}",
                contentDescription = result.store.name,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.store.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${result.store.category} • Floor ${result.store.floor}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${result.distanceMeters}m", fontSize = 12.sp)
                    Text("${result.estimatedMinutes} min", fontSize = 12.sp)
                }
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}