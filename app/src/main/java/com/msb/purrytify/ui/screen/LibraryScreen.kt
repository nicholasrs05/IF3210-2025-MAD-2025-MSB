package com.msb.purrytify.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.msb.purrytify.R

@Composable
fun LibraryScreen() {
    Column (
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
    ) {
        Header()
    }
}

@Composable
fun Header(){
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Your Library",
            fontSize = 24.sp,
            fontWeight = FontWeight.W800
        )
        IconButton(onClick = { /* ini jan */ }) {
            Icon(
                painter = painterResource(id = R.drawable.add),
                contentDescription = "Search",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Preview
@Composable
fun LibraryScreenPreview() {
    LibraryScreen()
}