package com.axiom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axiom.ui.viewmodel.BottomNavItem

@Composable
fun AxiomBottomNavigationBar(
    selectedItem: BottomNavItem,
    onItemSelected: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(96.dp),
        color = Color(0xFF1B1B23).copy(alpha = 0.9f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Store
            BottomNavItemButton(
                icon = Icons.Default.Folder,
                label = "STORE",
                isSelected = selectedItem == BottomNavItem.Store,
                onClick = { onItemSelected(BottomNavItem.Store) },
                modifier = Modifier.offset(x = 37.dp)
            )
            
            // Downloads
            BottomNavItemButton(
                icon = Icons.Default.Download,
                label = "DOWNLOADS",
                isSelected = selectedItem == BottomNavItem.Downloads,
                onClick = { onItemSelected(BottomNavItem.Downloads) },
                modifier = Modifier.offset(x = 117.dp)
            )
            
            // My Models (highlighted)
            BottomNavItemButton(
                icon = Icons.Default.Storage,
                label = "MY MODELS",
                isSelected = selectedItem == BottomNavItem.MyModels,
                onClick = { onItemSelected(BottomNavItem.MyModels) },
                isHighlighted = true,
                modifier = Modifier.offset(x = 231.dp)
            )
        }
    }
}

@Composable
private fun BottomNavItemButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isHighlighted) {
        Color(0xFFC0C1FF)
    } else {
        Color.Transparent
    }
    
    val contentColor = if (isHighlighted) {
        Color(0xFF07006C)
    } else {
        if (isSelected) Color(0xFFC0C1FF) else Color(0xFFC7C4D7)
    }
    
    val alpha = if (isSelected || isHighlighted) 1f else 0.6f
    
    Box(
        modifier = modifier
            .width(if (isHighlighted) 124.dp else 60.dp)
            .height(54.dp)
            .background(
                color = backgroundColor,
                shape = if (isHighlighted) RoundedCornerShape(16.dp) else CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor.copy(alpha = alpha),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                color = contentColor.copy(alpha = alpha),
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
