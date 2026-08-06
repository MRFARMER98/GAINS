package com.example.gains.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun GainsCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val cardShape = RoundedCornerShape(16.dp) // Upgraded to 16.dp rounding
    val baseModifier = modifier
        .fillMaxWidth()
        .clip(cardShape)
        .then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        )
        .border(
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline), // Uses SeparatorGray (#EFEFEF)
            cardShape
        )

    Card(
        modifier = baseModifier,
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = { content() }
    )
}
