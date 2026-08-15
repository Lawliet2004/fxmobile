package com.fxsound.mobile.ui.presets

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fxsound.mobile.R
import com.fxsound.mobile.audio.AudioEngineController
import com.fxsound.mobile.core.presets.PresetRepository
import com.fxsound.mobile.ui.components.FxPresetCard
import com.fxsound.mobile.ui.theme.FxAccentCoral
import com.fxsound.mobile.ui.theme.FxBgDark
import com.fxsound.mobile.ui.theme.FxBorder
import com.fxsound.mobile.ui.theme.FxCardSurface
import com.fxsound.mobile.ui.theme.FxTextPrimary
import com.fxsound.mobile.ui.theme.FxTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PresetsScreen(
    audioController: AudioEngineController,
    presetRepository: PresetRepository,
    modifier: Modifier = Modifier
) {
    val presets by presetRepository.presets.collectAsState()
    val activePreset by presetRepository.activePreset.collectAsState()
    val canUndo by presetRepository.canUndo.collectAsState()
    val currentParams by audioController.parameters.collectAsState()

    var showSaveDialog by remember { mutableStateOf(false) }
    var renameTargetId by remember { mutableStateOf<String?>(null) }
    var newPresetName by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(presetRepository.exportUserPresetsJson().toByteArray())
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (!text.isNullOrBlank()) presetRepository.importUserPresetsJson(text)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FxBgDark)
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.presets_library).uppercase(),
            color = FxTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Select, save, rename, or overwrite profiles",
            color = FxTextSecondary,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (canUndo) {
                TextButton(onClick = { presetRepository.undoLastMutation() }) {
                    Text(stringResource(R.string.undo_preset), color = FxTextSecondary, fontSize = 12.sp)
                }
            }
            TextButton(onClick = { exportLauncher.launch("fxmobile-presets.json") }) {
                Text(stringResource(R.string.export_presets), color = FxTextSecondary, fontSize = 12.sp)
            }
            TextButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }) {
                Text(stringResource(R.string.import_presets), color = FxTextSecondary, fontSize = 12.sp)
            }
            Button(
                onClick = {
                    nameError = null
                    newPresetName = ""
                    showSaveDialog = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = FxAccentCoral,
                    contentColor = FxTextPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, stringResource(R.string.save), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(R.string.save), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(presets, key = { it.id }) { preset ->
                Column {
                    FxPresetCard(
                        preset = preset,
                        isActive = activePreset.id == preset.id,
                        onSelect = {
                            presetRepository.selectPreset(preset)
                            audioController.applyPreset(preset)
                        },
                        onDelete = if (!preset.isBuiltIn) {
                            { presetRepository.deleteCustomPreset(preset.id) }
                        } else null
                    )
                    if (!preset.isBuiltIn) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                nameError = null
                                newPresetName = preset.name
                                renameTargetId = preset.id
                            }) {
                                Text(stringResource(R.string.rename_preset), color = FxTextSecondary, fontSize = 12.sp)
                            }
                            TextButton(onClick = {
                                presetRepository.overwriteCustomPreset(preset.id, currentParams)
                            }) {
                                Text(stringResource(R.string.overwrite_preset), color = FxTextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog || renameTargetId != null) {
        val renaming = renameTargetId != null
        AlertDialog(
            onDismissRequest = {
                showSaveDialog = false
                renameTargetId = null
            },
            title = {
                Text(
                    text = if (renaming) stringResource(R.string.rename_preset) else stringResource(R.string.save_preset),
                    color = FxTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPresetName,
                        onValueChange = {
                            newPresetName = it
                            nameError = null
                        },
                        label = { Text(stringResource(R.string.preset_name)) },
                        isError = nameError != null,
                        supportingText = nameError?.let { { Text(it, color = FxAccentCoral) } },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FxAccentCoral,
                            unfocusedBorderColor = FxBorder,
                            focusedTextColor = FxTextPrimary,
                            unfocusedTextColor = FxTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val current = presets.find { it.id == renameTargetId }?.name
                        val result = presetRepository.validateName(newPresetName, current)
                        if (!result.ok) {
                            nameError = result.message
                            return@Button
                        }
                        if (renaming) {
                            presetRepository.renameCustomPreset(renameTargetId!!, result.sanitized)
                        } else {
                            val saved = presetRepository.saveCustomPreset(result.sanitized, currentParams)
                            audioController.applyPreset(saved)
                        }
                        newPresetName = ""
                        showSaveDialog = false
                        renameTargetId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FxAccentCoral)
                ) {
                    Text(stringResource(R.string.save), color = FxTextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSaveDialog = false
                    renameTargetId = null
                }) {
                    Text(stringResource(R.string.cancel), color = FxTextSecondary)
                }
            },
            containerColor = FxCardSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
