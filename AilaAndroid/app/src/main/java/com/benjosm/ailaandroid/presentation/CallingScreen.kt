package com.benjosm.ailaandroid.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benjosm.ailaandroid.domain.Contact
import com.benjosm.ailaandroid.domain.ContactManager
import com.benjosm.ailaandroid.model.Contact
import com.benjosm.ailaandroid.flow.ConversationFlow
import com.benjosm.ailaandroid.speech.ConversationFlow
import com.benjosm.ailaandroid.utils.ContactManager

@Composable
fun CallingScreen(
    contact: Contact,
    conversation: ConversationFlow,
    onCallEnded: () -> Unit
) {
    var speakerOn by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top bar with contact name and language
        Text(
            text = contact.name,
            style = MaterialTheme.typography.h5,
            fontSize = 28.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Speaking ${contact.language}",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Dynamic narrative text
        Text(
            text = ContactManager.generateRingingNarrative(contact),
            color = Color.White,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        // Speaker toggle and hang-up button
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = { speakerOn = !speakerOn },
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = if (speakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    contentDescription = if (speakerOn) "Speaker on" else "Speaker off",
                    tint = if (speakerOn) MaterialTheme.colors.primary else Color.Gray,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.width(32.dp))

            FloatingActionButton(
                onClick = {
                    conversation.endCall()
                    onCallEnded()
                },
                backgroundColor = MaterialTheme.colors.error,
                modifier = Modifier.size(88.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End call",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}
