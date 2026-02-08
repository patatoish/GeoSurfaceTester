package com.geosurface.tester.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ResultCard(
    title: String,
    status: CardStatus = CardStatus.Info,
    content: @Composable ColumnScope.() -> Unit
) {
    val borderColor = when (status) {
        CardStatus.Success -> Color(0xFF4CAF50)
        CardStatus.Warning -> Color(0xFFFFA726)
        CardStatus.Error -> Color(0xFFF44336)
        CardStatus.Info -> Color(0xFF2196F3)
    }
    
    val backgroundColor = when (status) {
        CardStatus.Success -> Color(0xFF4CAF50).copy(alpha = 0.1f)
        CardStatus.Warning -> Color(0xFFFFA726).copy(alpha = 0.1f)
        CardStatus.Error -> Color(0xFFF44336).copy(alpha = 0.1f)
        CardStatus.Info -> Color(0xFF2196F3).copy(alpha = 0.1f)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, borderColor),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = borderColor
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun DataRow(label: String, value: String?, valueColor: Color? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value ?: "N/A",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

enum class CardStatus {
    Success, Warning, Error, Info
}
