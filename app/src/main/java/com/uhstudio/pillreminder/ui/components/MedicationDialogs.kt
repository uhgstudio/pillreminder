package com.uhstudio.pillreminder.ui.components

import android.content.Context
import android.media.RingtoneManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uhstudio.pillreminder.R
import com.uhstudio.pillreminder.ui.theme.StitchDeepBrown
import com.uhstudio.pillreminder.ui.theme.WarmPrimary

@Composable
fun DosagePickerDialog(
    initialDosage: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var tempDosage by remember { mutableStateOf(initialDosage) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color.White,
        shape = RoundedCornerShape(32.dp),
        title = {
            Text(stringResource(R.string.set_dosage_title), fontWeight = FontWeight.Bold, color = StitchDeepBrown)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = tempDosage,
                    onValueChange = { tempDosage = it },
                    placeholder = { Text(stringResource(R.string.set_dosage_hint), color = Color.LightGray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF5F5F5),
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = WarmPrimary
                    ),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(tempDosage) },
                colors = ButtonDefaults.buttonColors(containerColor = WarmPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.ok), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.btn_cancel), color = Color.Gray)
            }
        }
    )
}

@Composable
fun SoundPickerDialog(
    context: Context,
    currentUri: String?,
    onDismissRequest: () -> Unit,
    onSoundSelected: (uri: String?, name: String) -> Unit
) {
    val defaultSoundName = stringResource(R.string.default_sound)
    val ringtoneManager = remember { RingtoneManager(context) }
    val alarmRingtones = remember(defaultSoundName) {
        ringtoneManager.setType(RingtoneManager.TYPE_ALARM)
        val cursor = ringtoneManager.cursor
        val ringtones = mutableListOf<Pair<String?, String>>()

        // 기본 알람음 추가
        ringtones.add(Pair(null, defaultSoundName))

        // 시스템 알람음 추가
        while (cursor.moveToNext()) {
            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            val uri = ringtoneManager.getRingtoneUri(cursor.position).toString()
            ringtones.add(Pair(uri, title))
        }
        ringtones
    }

    var selectedUri by remember { mutableStateOf(currentUri) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color.White,
        shape = RoundedCornerShape(32.dp),
        title = { Text(stringResource(R.string.select_alarm_sound_title), fontWeight = FontWeight.Bold, color = StitchDeepBrown) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                items(alarmRingtones.size) { index ->
                    val (uri, name) = alarmRingtones[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedUri = uri
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedUri == uri,
                            onClick = { selectedUri = uri },
                            colors = RadioButtonDefaults.colors(selectedColor = WarmPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge.copy(color = StitchDeepBrown)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val selectedName = alarmRingtones.find { it.first == selectedUri }?.second ?: defaultSoundName
                    onSoundSelected(selectedUri, selectedName)
                },
                colors = ButtonDefaults.buttonColors(containerColor = WarmPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.ok), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.btn_cancel), color = Color.Gray)
            }
        }
    )
}
