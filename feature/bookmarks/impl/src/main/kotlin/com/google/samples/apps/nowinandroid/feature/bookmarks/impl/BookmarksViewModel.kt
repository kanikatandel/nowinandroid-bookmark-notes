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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.samples.apps.nowinandroid.core.data.repository.UserDataRepository
import com.google.samples.apps.nowinandroid.core.data.repository.UserNewsResourceRepository
import com.google.samples.apps.nowinandroid.core.model.data.UserNewsResource
import com.google.samples.apps.nowinandroid.core.ui.NewsFeedUiState
import com.google.samples.apps.nowinandroid.core.ui.NewsFeedUiState.Loading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val userDataRepository: UserDataRepository,
    userNewsResourceRepository: UserNewsResourceRepository,
) : ViewModel() {

    /**
     * Number of bookmarks removed by the last remove action, or 0 if there is nothing to undo.
     */
    var removedBookmarkCount by mutableStateOf(0)
        private set

    val shouldDisplayUndoBookmark: Boolean
        get() = removedBookmarkCount > 0

    /**
     * Notes of the bookmarks removed by the last remove action, keyed by news resource ID, so the
     * removal can be undone.
     */
    private var lastRemovedBookmarks: Map<String, String?> = emptyMap()

    val feedUiState: StateFlow<NewsFeedUiState> =
        userNewsResourceRepository.observeAllBookmarked()
            .map<List<UserNewsResource>, NewsFeedUiState>(NewsFeedUiState::Success)
            .onStart { emit(Loading) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = Loading,
            )

    private val selection = MutableStateFlow(BookmarksSelectionUiState())

    /**
     * The multi-select state. Selected IDs are restricted to bookmarks still in the feed, and
     * selection mode ends once there are no bookmarks left to select.
     */
    val selectionUiState: StateFlow<BookmarksSelectionUiState> =
        combine(selection, feedUiState) { current, feedState ->
            val bookmarkedIds = feedState.bookmarkedIds() ?: return@combine current
            BookmarksSelectionUiState(
                isSelectionMode = current.isSelectionMode && bookmarkedIds.isNotEmpty(),
                selectedIds = current.selectedIds intersect bookmarkedIds,
                totalCount = bookmarkedIds.size,
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = BookmarksSelectionUiState(),
            )

    fun removeFromSavedResources(newsResourceId: String) {
        removeBookmarks(setOf(newsResourceId))
    }

    fun setNewsResourceViewed(newsResourceId: String, viewed: Boolean) {
        viewModelScope.launch {
            userDataRepository.setNewsResourceViewed(newsResourceId, viewed)
        }
    }

    /**
     * Sets the personal note on a bookmark. A null or blank [note] deletes it.
     */
    fun setBookmarkNote(newsResourceId: String, note: String?) {
        viewModelScope.launch {
            userDataRepository.setBookmarkNote(newsResourceId, note)
        }
    }

    fun undoBookmarkRemoval() {
        val bookmarksToRestore = lastRemovedBookmarks
        viewModelScope.launch {
            if (bookmarksToRestore.isNotEmpty()) {
                userDataRepository.restoreBookmarks(bookmarksToRestore)
            }
        }
        clearUndoState()
    }

    fun clearUndoState() {
        removedBookmarkCount = 0
        lastRemovedBookmarks = emptyMap()
    }

    /**
     * Enters selection mode, optionally selecting [newsResourceId] straight away.
     */
    fun enterSelectionMode(newsResourceId: String? = null) {
        selection.value = BookmarksSelectionUiState(
            isSelectionMode = true,
            selectedIds = setOfNotNull(newsResourceId),
        )
    }

    fun exitSelectionMode() {
        selection.value = BookmarksSelectionUiState()
    }

    fun toggleSelection(newsResourceId: String) {
        selection.update {
            val selectedIds = if (newsResourceId in it.selectedIds) {
                it.selectedIds - newsResourceId
            } else {
                it.selectedIds + newsResourceId
            }
            it.copy(selectedIds = selectedIds)
        }
    }

    /**
     * Selects every bookmark, or clears the selection if every bookmark is already selected.
     */
    fun toggleSelectAll() {
        val bookmarkedIds = feedUiState.value.bookmarkedIds() ?: return
        selection.update {
            val allSelected = it.selectedIds.containsAll(bookmarkedIds)
            it.copy(selectedIds = if (allSelected) emptySet() else bookmarkedIds)
        }
    }

    /**
     * Removes every selected bookmark, along with its note, as a single undoable action and exits
     * selection mode.
     */
    fun removeSelectedBookmarks() {
        val selectedIds = selectionUiState.value.selectedIds
        exitSelectionMode()
        if (selectedIds.isNotEmpty()) removeBookmarks(selectedIds)
    }

    private fun removeBookmarks(newsResourceIds: Set<String>) {
        viewModelScope.launch {
            // Capture the notes before they're deleted alongside the bookmarks, so undo can
            // restore them.
            val bookmarkNotes = userDataRepository.userData.first().bookmarkNotes
            lastRemovedBookmarks = newsResourceIds.associateWith { bookmarkNotes[it] }
            removedBookmarkCount = newsResourceIds.size
            userDataRepository.removeBookmarks(newsResourceIds)
        }
    }

    private fun NewsFeedUiState.bookmarkedIds(): Set<String>? = when (this) {
        Loading -> null
        is NewsFeedUiState.Success -> feed.mapTo(mutableSetOf(), UserNewsResource::id)
    }
}

/**
 * State of multi-select mode on the bookmarks screen.
 *
 * @param isSelectionMode Whether the screen is in selection mode.
 * @param selectedIds IDs of the selected bookmarked news resources.
 * @param totalCount Total number of bookmarks that can be selected.
 */
data class BookmarksSelectionUiState(
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val totalCount: Int = 0,
) {
    val selectedCount: Int get() = selectedIds.size
    val isAllSelected: Boolean get() = totalCount > 0 && selectedCount == totalCount
}
