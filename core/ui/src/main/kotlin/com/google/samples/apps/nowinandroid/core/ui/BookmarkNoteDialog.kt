/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.nowinandroid.core.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaTextButton
import com.google.samples.apps.nowinandroid.core.designsystem.theme.NiaTheme
import com.google.samples.apps.nowinandroid.core.model.data.BOOKMARK_NOTE_MAX_LENGTH

/**
 * Dialog for adding, editing or deleting the personal note on a bookmarked news resource.
 *
 * When [initialNote] is null the dialog offers to add an optional note, and dismissing it skips
 * the note. Otherwise it edits [initialNote], and [onDelete], if provided, removes it.
 * The note is limited to [BOOKMARK_NOTE_MAX_LENGTH] characters.
 */
@Composable
fun BookmarkNoteDialog(
    initialNote: String?,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var note by rememberSaveable { mutableStateOf(initialNote.orEmpty()) }
    val isEditing = initialNote != null
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("bookmarkNoteDialog"),
        title = {
            Text(
                text = stringResource(
                    if (isEditing) {
                        R.string.core_ui_bookmark_note_dialog_title_edit
                    } else {
                        R.string.core_ui_bookmark_note_dialog_title_add
                    },
                ),
            )
        },
        text = {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(BOOKMARK_NOTE_MAX_LENGTH) },
                placeholder = { Text(stringResource(R.string.core_ui_bookmark_note_placeholder)) },
                supportingText = {
                    Text(
                        text = stringResource(
                            R.string.core_ui_bookmark_note_character_count,
                            note.length,
                            BOOKMARK_NOTE_MAX_LENGTH,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                    )
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
                minLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag("bookmarkNoteDialog:textField"),
            )
        },
        confirmButton = {
            NiaTextButton(onClick = { onSave(note) }) {
                Text(stringResource(R.string.core_ui_bookmark_note_save))
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    NiaTextButton(onClick = onDelete) {
                        Text(stringResource(R.string.core_ui_bookmark_note_delete))
                    }
                }
                NiaTextButton(onClick = onDismiss) {
                    Text(
                        stringResource(
                            if (isEditing) {
                                R.string.core_ui_bookmark_note_cancel
                            } else {
                                R.string.core_ui_bookmark_note_skip
                            },
                        ),
                    )
                }
            }
        },
    )
}

/**
 * State of the prompt offering to add a note to a news resource the user just bookmarked.
 */
@Stable
class BookmarkNotePromptState(newsResourceId: String? = null) {
    /**
     * The just-bookmarked news resource the prompt is shown for, or null when hidden.
     */
    var newsResourceId by mutableStateOf(newsResourceId)
        private set

    /**
     * Shows the prompt for [newsResourceId] if it was just bookmarked.
     */
    fun onBookmarkChanged(newsResourceId: String, bookmarked: Boolean) {
        if (bookmarked) this.newsResourceId = newsResourceId
    }

    fun dismiss() {
        newsResourceId = null
    }

    companion object {
        val Saver: Saver<BookmarkNotePromptState, String> = Saver(
            save = { it.newsResourceId },
            restore = ::BookmarkNotePromptState,
        )
    }
}

@Composable
fun rememberBookmarkNotePromptState(): BookmarkNotePromptState =
    rememberSaveable(saver = BookmarkNotePromptState.Saver) { BookmarkNotePromptState() }

/**
 * Shows a [BookmarkNoteDialog] offering to add an optional note while [state] holds a
 * just-bookmarked news resource. Saving passes the note to [onSaveNote]; skipping adds no note.
 */
@Composable
fun BookmarkNotePrompt(
    state: BookmarkNotePromptState,
    onSaveNote: (newsResourceId: String, note: String) -> Unit,
) {
    val newsResourceId = state.newsResourceId ?: return
    key(newsResourceId) {
        BookmarkNoteDialog(
            initialNote = null,
            onSave = { note ->
                onSaveNote(newsResourceId, note)
                state.dismiss()
            },
            onDismiss = state::dismiss,
        )
    }
}

@Preview
@Composable
private fun BookmarkNoteDialogAddPreview() {
    NiaTheme {
        BookmarkNoteDialog(initialNote = null, onSave = {}, onDismiss = {})
    }
}

@Preview
@Composable
private fun BookmarkNoteDialogEditPreview() {
    NiaTheme {
        BookmarkNoteDialog(
            initialNote = "Revisit before the architecture review",
            onSave = {},
            onDismiss = {},
            onDelete = {},
        )
    }
}
