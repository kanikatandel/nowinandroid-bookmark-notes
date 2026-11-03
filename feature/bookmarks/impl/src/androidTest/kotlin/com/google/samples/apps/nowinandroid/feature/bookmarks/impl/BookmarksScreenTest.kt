/*
 * Copyright 2022 The Android Open Source Project
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

package com.google.samples.apps.nowinandroid.feature.bookmarks.impl

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.samples.apps.nowinandroid.core.model.data.UserNewsResource
import com.google.samples.apps.nowinandroid.core.testing.data.newsResourcesTestData
import com.google.samples.apps.nowinandroid.core.testing.data.userNewsResourcesTestData
import com.google.samples.apps.nowinandroid.core.testing.repository.emptyUserData
import com.google.samples.apps.nowinandroid.core.ui.NewsFeedUiState
import com.google.samples.apps.nowinandroid.feature.bookmarks.api.R
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.google.samples.apps.nowinandroid.core.ui.R as UiR

/**
 * UI tests for [BookmarksScreen] composable.
 */
class BookmarksScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun loading_showsLoadingSpinner() {
        composeTestRule.setContent {
            BookmarksScreen(
                feedState = NewsFeedUiState.Loading,
                onShowSnackbar = { _, _ -> false },
                removeFromBookmarks = {},
                onTopicClick = {},
                onNewsResourceViewed = {},
            )
        }

        composeTestRule
            .onNodeWithContentDescription(
                composeTestRule.activity.resources.getString(R.string.feature_bookmarks_api_loading),
            )
            .assertExists()
    }

    @Test
    fun feed_whenHasBookmarks_showsBookmarks() {
        composeTestRule.setContent {
            BookmarksScreen(
                feedState = NewsFeedUiState.Success(
                    userNewsResourcesTestData.take(2),
                ),
                onShowSnackbar = { _, _ -> false },
                removeFromBookmarks = {},
                onTopicClick = {},
                onNewsResourceViewed = {},
            )
        }

        composeTestRule
            .onNodeWithText(
                userNewsResourcesTestData[0].title,
                substring = true,
            )
            .assertExists()
            .assertHasClickAction()

        composeTestRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(
                hasText(
                    userNewsResourcesTestData[1].title,
                    substring = true,
                ),
            )

        composeTestRule
            .onNodeWithText(
                userNewsResourcesTestData[1].title,
                substring = true,
            )
            .assertExists()
            .assertHasClickAction()
    }

    @Test
    fun feed_whenRemovingBookmark_removesBookmark() {
        var removeFromBookmarksCalled = false

        composeTestRule.setContent {
            BookmarksScreen(
                feedState = NewsFeedUiState.Success(
                    userNewsResourcesTestData.take(2),
                ),
                onShowSnackbar = { _, _ -> false },
                removeFromBookmarks = { newsResourceId ->
                    assertEquals(userNewsResourcesTestData[0].id, newsResourceId)
                    removeFromBookmarksCalled = true
                },
                onTopicClick = {},
                onNewsResourceViewed = {},
            )
        }

        composeTestRule
            .onAllNodesWithContentDescription(
                composeTestRule.activity.getString(
                    com.google.samples.apps.nowinandroid.core.ui.R.string.core_ui_unbookmark,
                ),
            ).filter(
                hasAnyAncestor(
                    hasText(
                        userNewsResourcesTestData[0].title,
                        substring = true,
                    ),
                ),
            )
            .assertCountEquals(1)
            .onFirst()
            .performClick()

        assertTrue(removeFromBookmarksCalled)
    }

    @Test
    fun feed_whenHasNoBookmarks_showsEmptyState() {
        composeTestRule.setContent {
            BookmarksScreen(
                feedState = NewsFeedUiState.Success(emptyList()),
                onShowSnackbar = { _, _ -> false },
                removeFromBookmarks = {},
                onTopicClick = {},
                onNewsResourceViewed = {},
            )
        }

        composeTestRule
            .onNodeWithText(
                composeTestRule.activity.getString(R.string.feature_bookmarks_api_empty_error),
            )
            .assertExists()

        composeTestRule
            .onNodeWithText(
                composeTestRule.activity.getString(R.string.feature_bookmarks_api_empty_description),
            )
            .assertExists()
    }

    @Test
    fun feed_whenLifecycleStops_undoBookmarkedStateIsCleared() = runTest {
        var undoStateCleared = false
        val testLifecycleOwner = TestLifecycleOwner(initialState = Lifecycle.State.STARTED)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides testLifecycleOwner) {
                BookmarksScreen(
                    feedState = NewsFeedUiState.Success(emptyList()),
                    onShowSnackbar = { _, _ -> false },
                    removeFromBookmarks = {},
                    onTopicClick = {},
                    onNewsResourceViewed = {},
                    clearUndoState = {
                        undoStateCleared = true
                    },
                )
            }
        }

        assertEquals(false, undoStateCleared)
        testLifecycleOwner.handleLifecycleEvent(event = Lifecycle.Event.ON_STOP)
        assertEquals(true, undoStateCleared)
    }

    @Test
    fun feed_whenBookmarkHasNote_showsNote() {
        composeTestRule.setContent {
            BookmarksScreen(
                feedState = NewsFeedUiState.Success(bookmarksWithNote),
                onShowSnackbar = { _, _ -> false },
                removeFromBookmarks = {},
                onTopicClick = {},
                onNewsResourceViewed = {},
            )
        }

        composeTestRule.onNodeWithText(NOTE).assertIsDisplayed()
    }

    @Test
    fun note_whenTappedAndEdited_savesNote() {
        var savedNote: Pair<String, String?>? = null
        composeTestRule.setContent {
            BookmarksScreen(
                feedState = NewsFeedUiState.Success(bookmarksWithNote),
                onShowSnackbar = { _, _ -> false },
                removeFromBookmarks = {},
                onTopicClick = {},
                onNewsResourceViewed = {},
                onBookmarkNoteChange = { id, note -> savedNote = id to note },
            )
        }

        composeTestRule.onNodeWithText(NOTE).performClick()
        composeTestRule.onNodeWithTag("bookmarkNoteDialog:textField").performTextReplacement("Updated")
        composeTestRule.onNodeWithText(getString(UiR.string.core_ui_bookmark_note_save)).performClick()

        assertEquals(bookmarksWithNote[0].id to "Updated", savedNote)
        composeTestRule.onNodeWithTag("bookmarkNoteDialog").assertDoesNotExist()
    }

    @Test
    fun note_whenDeleted_clearsNote() {
        var savedNote: Pair<String, String?>? = null
        composeTestRule.setContent {
            BookmarksScreen(
                feedState = NewsFeedUiState.Success(bookmarksWithNote),
                onShowSnackbar = { _, _ -> false },
                removeFromBookmarks = {},
                onTopicClick = {},
                onNewsResourceViewed = {},
                onBookmarkNoteChange = { id, note -> savedNote = id to note },
            )
        }

        composeTestRule.onNodeWithText(NOTE).performClick()
        composeTestRule.onNodeWithText(getString(UiR.string.core_ui_bookmark_note_delete)).performClick()

        assertEquals(bookmarksWithNote[0].id to null, savedNote)
    }

    @Test
    fun selectButton_entersSelectionMode() {
        var enteredWith: String? = "not called"
        composeTestRule.setContent {
            BookmarksScreen(
                feedState = NewsFeedUiState.Success(userNewsResourcesTestData.take(2)),
                onShowSnackbar = { _, _ -> false },
                removeFromBookmarks = {},
                onTopicClick = {},
                onNewsResourceViewed = {},
                onEnterSelectionMode = { enteredWith = it },
            )
        }

        composeTestRule.onNodeWithText(getString(R.string.feature_bookmarks_api_select)).performClick()

        assertNull(enteredWith)
    }

    @Test
    fun card_whenLongPressed_entersSelectionModeWithCardSelected() {
        var enteredWith: String? = null
        val resource = userNewsResourcesTestData[0]
        composeTestRule.setContent {
            BookmarksScreen(
                feedState = NewsFeedUiState.Success(userNewsResourcesTestData.take(2)),
                onShowSnackbar = { _, _ -> false },
                removeFromBookmarks = {},
                onTopicClick = {},
                onNewsResourceViewed = {},
                onEnterSelectionMode = { enteredWith = it },
            )
        }

        composeTestRule.onNodeWithTag("newsResourceCard:${resource.id}")
            .performTouchInput { longClick() }

        assertEquals(resource.id, enteredWith)
    }

    @Test
    fun selectionMode_showsCheckboxesAndCounter_insteadOfBookmarkButtons() {
        val feed = userNewsResourcesTestData.take(2)
        composeTestRule.setContent {
            BookmarksScreen(
                feedState = NewsFeedUiState.Success(feed),
                onShowSnackbar = { _, _ -> false },
                removeFromBookmarks = {},
                onTopicClick = {},
                onNewsResourceViewed = {},
                selectionState = BookmarksSelectionUiState(
                    isSelectionMode = true,
                    selectedIds = setOf(feed[0].id),
                    totalCount = feed.size,
                ),
            )
        }

        composeTestRule.onNodeWithText("1 selected").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("newsResourceCard:checkbox", useUnmergedTree = true)
            .assertCountEquals(feed.size)
        composeTestRule
            .onAllNodesWithContentDescription(getString(UiR.string.core_ui_unbookmark))
            .assertCountEquals(0)
        composeTestRule.onNodeWithTag("newsResourceCard:${feed[0].id}").assertIsSelected()
    }

    @Test
    fun selectionMode_tappingCardOrNote_togglesSelection_insteadOfEditingNote() {
        val toggled = mutableListOf<String>()
        composeTestRule.setContent {
            BookmarksScreen(
                feedState = NewsFeedUiState.Success(bookmarksWithNote),
                onShowSnackbar = { _, _ -> false },
                removeFromBookmarks = {},
                onTopicClick = {},
                onNewsResourceViewed = {},
                selectionState = BookmarksSelectionUiState(
                    isSelectionMode = true,
                    totalCount = bookmarksWithNote.size,
                ),
                onToggleSelection = { toggled += it },
            )
        }

        composeTestRule.onNodeWithText(bookmarksWithNote[0].title, substring = true).performClick()
        composeTestRule.onNodeWithText(NOTE).performClick()

        assertEquals(List(2) { bookmarksWithNote[0].id }, toggled)
        composeTestRule.onNodeWithTag("bookmarkNoteDialog").assertDoesNotExist()
    }

    @Test
    fun selectionMode_toolbarActions_invokeCallbacks() {
        var selectAllCalled = false
        var removeCalled = false
        var exitCalled = false
        val feed = userNewsResourcesTestData.take(2)
        composeTestRule.setContent {
            BookmarksScreen(
                feedState = NewsFeedUiState.Success(feed),
                onShowSnackbar = { _, _ -> false },
                removeFromBookmarks = {},
                onTopicClick = {},
                onNewsResourceViewed = {},
                selectionState = BookmarksSelectionUiState(
                    isSelectionMode = true,
                    selectedIds = setOf(feed[0].id),
                    totalCount = feed.size,
                ),
                onToggleSelectAll = { selectAllCalled = true },
                onRemoveSelected = { removeCalled = true },
                onExitSelectionMode = { exitCalled = true },
            )
        }

        composeTestRule.onNodeWithText(getString(R.string.feature_bookmarks_api_select_all)).performClick()
        composeTestRule.onNodeWithText(getString(R.string.feature_bookmarks_api_remove_selected)).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.feature_bookmarks_api_cancel_selection))
            .performClick()

        assertTrue(selectAllCalled)
        assertTrue(removeCalled)
        assertTrue(exitCalled)
    }

    @Test
    fun selectionMode_withNothingSelected_disablesRemove() {
        composeTestRule.setContent {
            BookmarksScreen(
                feedState = NewsFeedUiState.Success(userNewsResourcesTestData.take(2)),
                onShowSnackbar = { _, _ -> false },
                removeFromBookmarks = {},
                onTopicClick = {},
                onNewsResourceViewed = {},
                selectionState = BookmarksSelectionUiState(isSelectionMode = true, totalCount = 2),
            )
        }

        composeTestRule.onNodeWithText(getString(R.string.feature_bookmarks_api_remove_selected))
            .assertIsNotEnabled()
    }

    @Test
    fun selectionMode_whenBackPressed_exitsSelectionMode() {
        var exitCalled = false
        composeTestRule.setContent {
            BookmarksScreen(
                feedState = NewsFeedUiState.Success(userNewsResourcesTestData.take(2)),
                onShowSnackbar = { _, _ -> false },
                removeFromBookmarks = {},
                onTopicClick = {},
                onNewsResourceViewed = {},
                selectionState = BookmarksSelectionUiState(isSelectionMode = true, totalCount = 2),
                onExitSelectionMode = { exitCalled = true },
            )
        }

        composeTestRule.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }

        assertTrue(exitCalled)
    }

    @Test
    fun bulkRemoval_showsSingleUndoSnackbar_thatUndoesRemoval() {
        var snackbarMessage: String? = null
        var undoCalled = false
        composeTestRule.setContent {
            BookmarksScreen(
                feedState = NewsFeedUiState.Success(emptyList()),
                onShowSnackbar = { message, _ ->
                    snackbarMessage = message
                    true
                },
                removeFromBookmarks = {},
                onTopicClick = {},
                onNewsResourceViewed = {},
                removedBookmarkCount = 3,
                undoBookmarkRemoval = { undoCalled = true },
            )
        }

        composeTestRule.waitForIdle()
        assertEquals("3 bookmarks removed", snackbarMessage)
        assertTrue(undoCalled)
    }

    private fun getString(id: Int) = composeTestRule.activity.getString(id)

    private companion object {
        const val NOTE = "Revisit before the architecture review"

        val bookmarksWithNote = listOf(
            UserNewsResource(
                newsResource = newsResourcesTestData[0],
                userData = emptyUserData.copy(
                    bookmarkedNewsResources = setOf(newsResourcesTestData[0].id),
                    bookmarkNotes = mapOf(newsResourcesTestData[0].id to NOTE),
                ),
            ),
        )
    }
}
