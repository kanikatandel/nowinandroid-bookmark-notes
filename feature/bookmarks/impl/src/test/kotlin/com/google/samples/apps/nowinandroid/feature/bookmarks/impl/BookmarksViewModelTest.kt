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

import com.google.samples.apps.nowinandroid.core.data.repository.CompositeUserNewsResourceRepository
import com.google.samples.apps.nowinandroid.core.testing.data.newsResourcesTestData
import com.google.samples.apps.nowinandroid.core.testing.repository.TestNewsRepository
import com.google.samples.apps.nowinandroid.core.testing.repository.TestUserDataRepository
import com.google.samples.apps.nowinandroid.core.testing.repository.emptyUserData
import com.google.samples.apps.nowinandroid.core.testing.util.MainDispatcherRule
import com.google.samples.apps.nowinandroid.core.ui.NewsFeedUiState.Loading
import com.google.samples.apps.nowinandroid.core.ui.NewsFeedUiState.Success
import com.google.samples.apps.nowinandroid.feature.bookmarks.impl.BookmarksViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * To learn more about how this test handles Flows created with stateIn, see
 * https://developer.android.com/kotlin/flow/test#statein
 */
class BookmarksViewModelTest {
    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val userDataRepository = TestUserDataRepository()
    private val newsRepository = TestNewsRepository()
    private val userNewsResourceRepository = CompositeUserNewsResourceRepository(
        newsRepository = newsRepository,
        userDataRepository = userDataRepository,
    )
    private lateinit var viewModel: BookmarksViewModel

    @Before
    fun setup() {
        viewModel = BookmarksViewModel(
            userDataRepository = userDataRepository,
            userNewsResourceRepository = userNewsResourceRepository,
        )
    }

    @Test
    fun stateIsInitiallyLoading() = runTest {
        assertEquals(Loading, viewModel.feedUiState.value)
    }

    @Test
    fun oneBookmark_showsInFeed() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.feedUiState.collect() }

        newsRepository.sendNewsResources(newsResourcesTestData)
        userDataRepository.setNewsResourceBookmarked(newsResourcesTestData[0].id, true)
        val item = viewModel.feedUiState.value
        assertIs<Success>(item)
        assertEquals(item.feed.size, 1)
    }

    @Test
    fun oneBookmark_whenRemoving_removesFromFeed() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.feedUiState.collect() }
        // Set the news resources to be used by this test
        newsRepository.sendNewsResources(newsResourcesTestData)
        // Start with the resource saved
        userDataRepository.setNewsResourceBookmarked(newsResourcesTestData[0].id, true)
        // Use viewModel to remove saved resource
        viewModel.removeFromSavedResources(newsResourcesTestData[0].id)
        // Verify list of saved resources is now empty
        val item = viewModel.feedUiState.value
        assertIs<Success>(item)
        assertEquals(item.feed.size, 0)
        assertTrue(viewModel.shouldDisplayUndoBookmark)
    }

    @Test
    fun feedUiState_resourceIsViewed_setResourcesViewed() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.feedUiState.collect() }

        // Given
        newsRepository.sendNewsResources(newsResourcesTestData)
        userDataRepository.setNewsResourceBookmarked(newsResourcesTestData[0].id, true)
        val itemBeforeViewed = viewModel.feedUiState.value
        assertIs<Success>(itemBeforeViewed)
        assertFalse(itemBeforeViewed.feed.first().hasBeenViewed)

        // When
        viewModel.setNewsResourceViewed(newsResourcesTestData[0].id, true)

        // Then
        val item = viewModel.feedUiState.value
        assertIs<Success>(item)
        assertTrue(item.feed.first().hasBeenViewed)
    }

    @Test
    fun feedUiState_undoneBookmarkRemoval_bookmarkIsRestored() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.feedUiState.collect() }

        // Given
        newsRepository.sendNewsResources(newsResourcesTestData)
        userDataRepository.setNewsResourceBookmarked(newsResourcesTestData[0].id, true)
        viewModel.removeFromSavedResources(newsResourcesTestData[0].id)
        assertTrue(viewModel.shouldDisplayUndoBookmark)
        val itemBeforeUndo = viewModel.feedUiState.value
        assertIs<Success>(itemBeforeUndo)
        assertEquals(0, itemBeforeUndo.feed.size)

        // When
        viewModel.undoBookmarkRemoval()

        // Then
        assertFalse(viewModel.shouldDisplayUndoBookmark)
        val item = viewModel.feedUiState.value
        assertIs<Success>(item)
        assertEquals(1, item.feed.size)
    }

    @Test
    fun bookmarkNote_isShownInFeed() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.feedUiState.collect() }
        newsRepository.sendNewsResources(newsResourcesTestData)
        userDataRepository.setNewsResourceBookmarked(newsResourcesTestData[0].id, true)

        viewModel.setBookmarkNote(newsResourcesTestData[0].id, "Read later")

        val item = viewModel.feedUiState.value
        assertIs<Success>(item)
        assertEquals("Read later", item.feed.single().note)
    }

    @Test
    fun bookmarkNote_whenDeleted_isRemovedFromFeed() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.feedUiState.collect() }
        newsRepository.sendNewsResources(newsResourcesTestData)
        userDataRepository.setNewsResourceBookmarked(newsResourcesTestData[0].id, true)
        viewModel.setBookmarkNote(newsResourcesTestData[0].id, "Read later")

        viewModel.setBookmarkNote(newsResourcesTestData[0].id, null)

        val item = viewModel.feedUiState.value
        assertIs<Success>(item)
        assertNull(item.feed.single().note)
    }

    @Test
    fun singleRemoval_undo_restoresBookmarkAndNote() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.feedUiState.collect() }
        newsRepository.sendNewsResources(newsResourcesTestData)
        val id = newsResourcesTestData[0].id
        userDataRepository.setNewsResourceBookmarked(id, true)
        userDataRepository.setBookmarkNote(id, "Keep me")

        viewModel.removeFromSavedResources(id)
        assertEquals(1, viewModel.removedBookmarkCount)
        assertEquals(emptyMap(), userDataRepository.userData.first().bookmarkNotes)

        viewModel.undoBookmarkRemoval()

        val item = viewModel.feedUiState.value
        assertIs<Success>(item)
        assertEquals("Keep me", item.feed.single().note)
    }

    @Test
    fun selectionMode_enterWithItem_selectsIt() = runTest {
        bookmarkAll()

        viewModel.enterSelectionMode(newsResourcesTestData[0].id)

        val selection = viewModel.selectionUiState.value
        assertTrue(selection.isSelectionMode)
        assertEquals(setOf(newsResourcesTestData[0].id), selection.selectedIds)
        assertEquals(newsResourcesTestData.size, selection.totalCount)
    }

    @Test
    fun selectionMode_toggleSelection_selectsAndDeselects() = runTest {
        bookmarkAll()
        viewModel.enterSelectionMode()

        viewModel.toggleSelection(newsResourcesTestData[0].id)
        viewModel.toggleSelection(newsResourcesTestData[1].id)
        assertEquals(2, viewModel.selectionUiState.value.selectedCount)

        viewModel.toggleSelection(newsResourcesTestData[0].id)
        assertEquals(
            setOf(newsResourcesTestData[1].id),
            viewModel.selectionUiState.value.selectedIds,
        )
    }

    @Test
    fun selectionMode_toggleSelectAll_selectsAllThenNone() = runTest {
        bookmarkAll()
        viewModel.enterSelectionMode()

        viewModel.toggleSelectAll()
        assertTrue(viewModel.selectionUiState.value.isAllSelected)
        assertEquals(newsResourcesTestData.size, viewModel.selectionUiState.value.selectedCount)

        viewModel.toggleSelectAll()
        assertEquals(0, viewModel.selectionUiState.value.selectedCount)
    }

    @Test
    fun selectionMode_exit_clearsSelection() = runTest {
        bookmarkAll()
        viewModel.enterSelectionMode(newsResourcesTestData[0].id)

        viewModel.exitSelectionMode()

        val selection = viewModel.selectionUiState.value
        assertFalse(selection.isSelectionMode)
        assertEquals(emptySet(), selection.selectedIds)
        // Exiting without acting leaves bookmarks untouched.
        assertEquals(newsResourcesTestData.size, userDataRepository.userData.first().bookmarkedNewsResources.size)
    }

    @Test
    fun removeSelected_removesBookmarksAndNotes_andExitsSelectionMode() = runTest {
        bookmarkAll()
        val (first, second) = newsResourcesTestData.take(2).map { it.id }
        userDataRepository.setBookmarkNote(first, "Note 1")
        viewModel.enterSelectionMode(first)
        viewModel.toggleSelection(second)

        viewModel.removeSelectedBookmarks()

        assertFalse(viewModel.selectionUiState.value.isSelectionMode)
        assertEquals(2, viewModel.removedBookmarkCount)
        assertTrue(viewModel.shouldDisplayUndoBookmark)
        val userData = userDataRepository.userData.first()
        assertFalse(first in userData.bookmarkedNewsResources)
        assertFalse(second in userData.bookmarkedNewsResources)
        assertEquals(emptyMap(), userData.bookmarkNotes)
        val item = viewModel.feedUiState.value
        assertIs<Success>(item)
        assertEquals(newsResourcesTestData.size - 2, item.feed.size)
    }

    @Test
    fun removeSelected_undo_restoresAllBookmarksAndNotes() = runTest {
        bookmarkAll()
        val ids = newsResourcesTestData.map { it.id }
        userDataRepository.setBookmarkNote(ids[0], "Note 1")
        userDataRepository.setBookmarkNote(ids[1], "Note 2")
        viewModel.enterSelectionMode()
        viewModel.toggleSelectAll()
        viewModel.removeSelectedBookmarks()
        val itemBeforeUndo = viewModel.feedUiState.value
        assertIs<Success>(itemBeforeUndo)
        assertEquals(0, itemBeforeUndo.feed.size)

        viewModel.undoBookmarkRemoval()

        assertFalse(viewModel.shouldDisplayUndoBookmark)
        val userData = userDataRepository.userData.first()
        assertEquals(ids.toSet(), userData.bookmarkedNewsResources)
        assertEquals(mapOf(ids[0] to "Note 1", ids[1] to "Note 2"), userData.bookmarkNotes)
    }

    @Test
    fun removeSelected_withNothingSelected_removesNothing() = runTest {
        bookmarkAll()
        viewModel.enterSelectionMode()

        viewModel.removeSelectedBookmarks()

        assertFalse(viewModel.shouldDisplayUndoBookmark)
        assertEquals(newsResourcesTestData.size, userDataRepository.userData.first().bookmarkedNewsResources.size)
    }

    @Test
    fun selection_whenSelectedBookmarkIsRemovedElsewhere_isDropped() = runTest {
        bookmarkAll()
        viewModel.enterSelectionMode(newsResourcesTestData[0].id)

        userDataRepository.setNewsResourceBookmarked(newsResourcesTestData[0].id, false)

        assertEquals(emptySet(), viewModel.selectionUiState.value.selectedIds)
    }

    private fun TestScope.bookmarkAll() {
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.feedUiState.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.selectionUiState.collect() }
        newsRepository.sendNewsResources(newsResourcesTestData)
        userDataRepository.setUserData(
            emptyUserData.copy(bookmarkedNewsResources = newsResourcesTestData.map { it.id }.toSet()),
        )
    }
}
