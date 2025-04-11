package com.msb.purrytify.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight

@Composable
fun NetworkPopUp(
    isConnected: Boolean,
    isMiniPlayerVisible: Boolean = false
) {
    if (!isConnected) {
        val bottomPadding = if (isMiniPlayerVisible) 160.dp else 80.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = bottomPadding)
                .zIndex(999f),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .background(Color(0xFFf44848), RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "No internet connection",
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }
    }
}