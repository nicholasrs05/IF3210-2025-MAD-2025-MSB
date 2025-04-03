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
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 2.dp),
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

        Row (
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            FilterButton(label = "All")
            FilterButton(label = "Liked")
        }
    }

}

@Composable
fun FilterButton(label: String) {
    Button(
        onClick = { /* filter nanti */ },
        modifier = Modifier
            .padding(4.dp)
    ) {
        Text(text = label)
    }
}

@Preview
@Composable
fun LibraryScreenPreview() {
    LibraryScreen()
}