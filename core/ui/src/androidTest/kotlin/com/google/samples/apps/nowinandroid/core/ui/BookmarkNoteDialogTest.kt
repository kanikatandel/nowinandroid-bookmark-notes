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

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.google.samples.apps.nowinandroid.core.model.data.BOOKMARK_NOTE_MAX_LENGTH
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BookmarkNoteDialogTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun addNote_whenSaved_passesNote() {
        var savedNote: String? = null
        composeTestRule.setContent {
            BookmarkNoteDialog(initialNote = null, onSave = { savedNote = it }, onDismiss = {})
        }

        composeTestRule.onNodeWithText(getString(R.string.core_ui_bookmark_note_dialog_title_add))
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEXT_FIELD).performTextInput("Great read")
        composeTestRule.onNodeWithText(getString(R.string.core_ui_bookmark_note_save)).performClick()

        assertEquals("Great read", savedNote)
    }

    @Test
    fun addNote_whenSkipped_dismissesWithoutSaving() {
        var savedNote: String? = null
        var dismissed = false
        composeTestRule.setContent {
            BookmarkNoteDialog(
                initialNote = null,
                onSave = { savedNote = it },
                onDismiss = { dismissed = true },
            )
        }

        composeTestRule.onNodeWithText(getString(R.string.core_ui_bookmark_note_skip)).performClick()

        assertTrue(dismissed)
        assertNull(savedNote)
    }

    @Test
    fun note_isLimitedToMaxLength_andShowsCounter() {
        var savedNote: String? = null
        composeTestRule.setContent {
            BookmarkNoteDialog(initialNote = null, onSave = { savedNote = it }, onDismiss = {})
        }

        composeTestRule.onNodeWithTag(TEXT_FIELD)
            .performTextInput("a".repeat(BOOKMARK_NOTE_MAX_LENGTH + 10))

        composeTestRule.onNodeWithTag(TEXT_FIELD)
            .assertTextContains("$BOOKMARK_NOTE_MAX_LENGTH/$BOOKMARK_NOTE_MAX_LENGTH")
        composeTestRule.onNodeWithText(getString(R.string.core_ui_bookmark_note_save)).performClick()
        assertEquals(BOOKMARK_NOTE_MAX_LENGTH, savedNote?.length)
    }

    @Test
    fun editNote_prefillsNote_andOffersDelete() {
        var deleted = false
        composeTestRule.setContent {
            BookmarkNoteDialog(
                initialNote = "Existing note",
                onSave = {},
                onDismiss = {},
                onDelete = { deleted = true },
            )
        }

        composeTestRule.onNodeWithText(getString(R.string.core_ui_bookmark_note_dialog_title_edit))
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEXT_FIELD).assertTextContains("Existing note")
        composeTestRule.onNodeWithText(getString(R.string.core_ui_bookmark_note_delete)).performClick()

        assertTrue(deleted)
    }

    @Test
    fun prompt_isShownOnlyAfterBookmarking() {
        val state = BookmarkNotePromptState()
        var savedNote: Pair<String, String>? = null
        composeTestRule.setContent {
            BookmarkNotePrompt(state = state, onSaveNote = { id, note -> savedNote = id to note })
        }

        state.onBookmarkChanged("1", bookmarked = false)
        composeTestRule.onNodeWithTag(DIALOG).assertDoesNotExist()

        state.onBookmarkChanged("1", bookmarked = true)
        composeTestRule.onNodeWithTag(TEXT_FIELD).performTextInput("Note")
        composeTestRule.onNodeWithText(getString(R.string.core_ui_bookmark_note_save)).performClick()

        assertEquals("1" to "Note", savedNote)
        composeTestRule.onNodeWithTag(DIALOG).assertDoesNotExist()
    }

    private fun getString(id: Int) = composeTestRule.activity.getString(id)

    private companion object {
        const val DIALOG = "bookmarkNoteDialog"
        const val TEXT_FIELD = "bookmarkNoteDialog:textField"
    }
}
