package com.fxsound.mobile.ui.presets

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fxsound.mobile.R
import com.fxsound.mobile.audio.AudioEngineController
import com.fxsound.mobile.core.model.Preset
import com.fxsound.mobile.core.presets.PresetRepository
import com.fxsound.mobile.ui.components.FxPresetCard
import com.fxsound.mobile.ui.theme.FxAccent
import com.fxsound.mobile.ui.theme.FxBgDark
import com.fxsound.mobile.ui.theme.FxBorder
import com.fxsound.mobile.ui.theme.FxCardSurface
import com.fxsound.mobile.ui.theme.FxOnAccent
import com.fxsound.mobile.ui.theme.FxTextPrimary
import com.fxsound.mobile.ui.theme.FxTextSecondary
import com.fxsound.mobile.ui.util.UiPacking
import com.fxsound.mobile.ui.util.collectMappedAsState

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
    val lastError by audioController.telemetry.collectMappedAsState { it.lastError }

    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var renameTargetId by rememberSaveable { mutableStateOf<String?>(null) }
    var newPresetName by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var nameError by remember { mutableStateOf<String?>(null) }

    // Confirmations: which preset (by id) is the pending destructive action on?
    var deleteConfirmPreset by remember { mutableStateOf<Preset?>(null) }
    var overwriteConfirmPreset by remember { mutableStateOf<Preset?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
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

    LaunchedEffect(lastError) {
        val err = lastError
        if (!err.isNullOrBlank()) {
            snackbarHostState.showSnackbar(err)
            audioController.clearError()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(FxBgDark),
            contentPadding = PaddingValues(
                start = UiPacking.SCREEN_PAD_DP.dp,
                end = UiPacking.SCREEN_PAD_DP.dp,
                top = 16.dp,
                bottom = UiPacking.settingsBottomClearanceDp().dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column {
                    Text(
                        text = stringResource(R.string.presets_library),
                        color = FxTextPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Select, save, rename, or overwrite profiles",
                        color = FxTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (canUndo) {
                            CompactTextAction(stringResource(R.string.undo_preset)) {
                                presetRepository.undoLastMutation()?.let { restored ->
                                    audioController.applyPreset(restored)
                                }
                            }
                        }
                        CompactTextAction(stringResource(R.string.export_presets)) {
                            exportLauncher.launch("fxmobile-presets.json")
                        }
                        CompactTextAction(stringResource(R.string.import_presets)) {
                            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        }
                        Button(
                            onClick = {
                                nameError = null
                                newPresetName = TextFieldValue("")
                                showSaveDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FxAccent,
                                contentColor = FxOnAccent
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .defaultMinSize(minWidth = 1.dp, minHeight = 32.dp)
                                .heightIn(min = 32.dp)
                        ) {
                            Icon(Icons.Default.Add, stringResource(R.string.save), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.save),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                        }
                    }
                }
            }
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
                            { deleteConfirmPreset = preset }
                        } else null
                    )
                    if (!preset.isBuiltIn) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CompactTextAction(stringResource(R.string.rename_preset)) {
                                nameError = null
                                newPresetName = TextFieldValue(preset.name)
                                renameTargetId = preset.id
                            }
                            CompactTextAction(stringResource(R.string.overwrite_preset)) {
                                overwriteConfirmPreset = preset
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = FxCardSurface,
                contentColor = FxOnAccent
            )
        }
    }

    // Save / rename dialog
    if (showSaveDialog || renameTargetId != null) {
        val renaming = renameTargetId != null
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(showSaveDialog, renameTargetId) {
            focusRequester.requestFocus()
        }
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
                        supportingText = nameError?.let { { Text(it, color = FxAccent) } },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FxAccent,
                            unfocusedBorderColor = FxBorder,
                            focusedTextColor = FxTextPrimary,
                            unfocusedTextColor = FxTextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val current = presets.find { it.id == renameTargetId }?.name
                        val result = presetRepository.validateName(newPresetName.text, current)
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
                        newPresetName = TextFieldValue("")
                        showSaveDialog = false
                        renameTargetId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FxAccent)
                ) {
                    Text(stringResource(R.string.save), color = FxOnAccent, fontWeight = FontWeight.Bold)
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

    // Delete confirmation
    deleteConfirmPreset?.let { preset ->
        AlertDialog(
            onDismissRequest = { deleteConfirmPreset = null },
            title = {
                Text(
                    text = stringResource(R.string.confirm_delete_preset_title),
                    color = FxTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.confirm_delete_preset_message, preset.name),
                    color = FxTextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        presetRepository.deleteCustomPreset(preset.id)?.let { next ->
                            audioController.applyPreset(next)
                        }
                        deleteConfirmPreset = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FxAccent)
                ) {
                    Text(stringResource(R.string.action_delete), color = FxOnAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmPreset = null }) {
                    Text(stringResource(R.string.action_keep), color = FxTextSecondary)
                }
            },
            containerColor = FxCardSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Overwrite confirmation
    overwriteConfirmPreset?.let { preset ->
        AlertDialog(
            onDismissRequest = { overwriteConfirmPreset = null },
            title = {
                Text(
                    text = stringResource(R.string.confirm_overwrite_preset_title),
                    color = FxTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.confirm_overwrite_preset_message, preset.name),
                    color = FxTextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        presetRepository.overwriteCustomPreset(preset.id, currentParams)
                        overwriteConfirmPreset = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FxAccent)
                ) {
                    Text(stringResource(R.string.action_overwrite), color = FxOnAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { overwriteConfirmPreset = null }) {
                    Text(stringResource(R.string.cancel), color = FxTextSecondary)
                }
            },
            containerColor = FxCardSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun CompactTextAction(
    label: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier
            .defaultMinSize(minWidth = 1.dp, minHeight = 32.dp)
            .heightIn(min = 32.dp)
    ) {
        Text(
            text = label,
            color = FxTextSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}
