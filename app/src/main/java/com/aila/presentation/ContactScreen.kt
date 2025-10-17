package com.aila.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aila.R
import com.aila.data.ContactEntity
import com.aila.viewModel.ContactViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ContactListScreen(
    viewModel: ContactViewModel,
    onEditContact: (ContactEntity) -> Unit,
    onAddContact: () -> Unit
) {
    val contacts by viewModel.contacts.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddContact,
                contentColor = MaterialTheme.colors.onPrimary,
                backgroundColor = MaterialTheme.colors.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Contact")
            }
        },
        content = { padding ->
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(contacts) { contact ->
                    ContactItem(
                        contact = contact,
                        onCall = { /* Handle call */ },
                        onEdit = { onEditContact(contact) },
                        onDelete = { viewModel.deleteContact(contact) }
                    )
                    Divider()
                }
            }
        }
    )
}

@Composable
private fun ContactItem(
    contact: ContactEntity,
    onCall: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier.size(56.dp),
            elevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = contact.name.take(1).uppercase(),
                    style = MaterialTheme.typography.h6
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Text(text = contact.name, style = MaterialTheme.typography.h6)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = getLanguageIcon(contact.language),
                    contentDescription = contact.language,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = contact.language,
                    style = MaterialTheme.typography.caption
                )
            }
        }

        ActionButton(icon = Icons.Filled.Call, onClick = onCall)
        Spacer(modifier = Modifier.width(8.dp))
        ActionButton(icon = Icons.Filled.Edit, onClick = onEdit)
        Spacer(modifier = Modifier.width(8.dp))
        ActionButton(icon = Icons.Filled.Delete, onClick = onDelete)
    }
}

@Composable
private fun ActionButton(icon: ImageVector, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp)
    ) {
        Icon(icon, contentDescription = null)
    }
}

private fun getLanguageIcon(language: String): ImageVector =
    when (language.lowercase()) {
        "english" -> Icons.Filled.Flag
        "spanish" -> Icons.Filled.Flag
        "french" -> Icons.Filled.Flag
        else -> Icons.Filled.Language
    }

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ContactDetailModal(
    contact: ContactEntity?,
    onSave: (ContactEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(contact?.name ?: "") }
    var birthday by remember { mutableStateOf(contact?.birthday ?: "") }
    var personality by remember { mutableStateOf(contact?.personality ?: "") }
    var selectedVoice by remember { mutableStateOf(contact?.voice ?: "default") }
    var selectedLanguage by remember { mutableStateOf(contact?.language ?: "English") }

    val isoDateRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    val isBirthdayValid by remember(birthday) { derivedStateOf { birthday.matches(isoDateRegex) } }

    val languages = listOf("English", "Spanish", "French", "German", "Italian")

    BottomSheetDialog(onDismiss = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (contact == null) "Add Contact" else "Edit Contact",
                    style = MaterialTheme.typography.h5,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = birthday,
                    onValueChange = { birthday = it },
                    label = { Text("Birthday (YYYY-MM-DD)") },
                    isError = birthday.isNotEmpty() && !isBirthdayValid,
                    modifier = Modifier.fillMaxWidth()
                )

                if (birthday.isNotEmpty() && !isBirthdayValid) {
                    Text(
                        text = "Please enter a valid date in YYYY-MM-DD format",
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = personality,
                    onValueChange = { personality = it },
                    label = { Text("Personality") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Voice", style = MaterialTheme.typography.subtitle1)
                RadioGroup(
                    options = listOf("Default", "Warm", "Energetic", "Calm"),
                    selected = selectedVoice,
                    onSelect = { selectedVoice = it.lowercase() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Language", style = MaterialTheme.typography.subtitle1)
                ExposedDropdownMenuBox(
                    expanded = false,
                    onExpandedChange = { /* Handle expansion */ }
                ) {
                    TextField(
                        readOnly = true,
                        value = selectedLanguage,
                        onValueChange = { },
                        label = { Text("Select Language") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = false) }
                    )
                    ExposedDropdownMenu(
                        expanded = false,
                        onDismissRequest = { }
                    ) {
                        languages.forEach { lang ->
                            DropdownMenuItem(
                                onClick = {
                                    selectedLanguage = lang
                                }
                            ) {
                                Text(text = lang)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (isBirthdayValid || birthday.isEmpty()) {
                                val updatedContact = (contact ?: ContactEntity(
                                    id = UUID.randomUUID().toString()
                                )).copy(
                                    name = name,
                                    birthday = birthday,
                                    personality = personality,
                                    voice = selectedVoice,
                                    language = selectedLanguage
                                )
                                onSave(updatedContact)
                            }
                        },
                        enabled = name.isNotBlank() && (isBirthdayValid || birthday.isEmpty())
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun RadioGroup(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column {
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(option) }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected.lowercase() == option.lowercase(),
                    onClick = { onSelect(option) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(option)
            }
        }
    }
}

@Composable
private fun BottomSheetDialog(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            contentAlignment = Alignment.BottomCenter
        ) {
            content()
        }
    }
}
