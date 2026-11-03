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

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.samples.apps.nowinandroid.core.analytics.LocalAnalyticsHelper
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaLoadingWheel
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaTextButton
import com.google.samples.apps.nowinandroid.core.designsystem.component.scrollbar.DraggableScrollbar
import com.google.samples.apps.nowinandroid.core.designsystem.component.scrollbar.rememberDraggableScroller
import com.google.samples.apps.nowinandroid.core.designsystem.component.scrollbar.scrollbarState
import com.google.samples.apps.nowinandroid.core.designsystem.icon.NiaIcons
import com.google.samples.apps.nowinandroid.core.designsystem.theme.LocalTintTheme
import com.google.samples.apps.nowinandroid.core.designsystem.theme.NiaTheme
import com.google.samples.apps.nowinandroid.core.model.data.UserNewsResource
import com.google.samples.apps.nowinandroid.core.ui.BookmarkNoteDialog
import com.google.samples.apps.nowinandroid.core.ui.NewsFeedUiState
import com.google.samples.apps.nowinandroid.core.ui.NewsFeedUiState.Loading
import com.google.samples.apps.nowinandroid.core.ui.NewsFeedUiState.Success
import com.google.samples.apps.nowinandroid.core.ui.NewsResourceCardExpanded
import com.google.samples.apps.nowinandroid.core.ui.TrackScreenViewEvent
import com.google.samples.apps.nowinandroid.core.ui.TrackScrollJank
import com.google.samples.apps.nowinandroid.core.ui.UserNewsResourcePreviewParameterProvider
import com.google.samples.apps.nowinandroid.core.ui.launchCustomChromeTab
import com.google.samples.apps.nowinandroid.core.ui.logNewsResourceOpened
import com.google.samples.apps.nowinandroid.feature.bookmarks.api.R

@Composable
internal fun BookmarksScreen(
    onTopicClick: (String) -> Unit,
    onShowSnackbar: suspend (String, String?) -> Boolean,
    modifier: Modifier = Modifier,
    viewModel: BookmarksViewModel = hiltViewModel(),
) {
    val feedState by viewModel.feedUiState.collectAsStateWithLifecycle()
    val selectionState by viewModel.selectionUiState.collectAsStateWithLifecycle()
    BookmarksScreen(
        feedState = feedState,
        onShowSnackbar = onShowSnackbar,
        removeFromBookmarks = viewModel::removeFromSavedResources,
        onNewsResourceViewed = { viewModel.setNewsResourceViewed(it, true) },
        onTopicClick = onTopicClick,
        modifier = modifier,
        removedBookmarkCount = viewModel.removedBookmarkCount,
        undoBookmarkRemoval = viewModel::undoBookmarkRemoval,
        clearUndoState = viewModel::clearUndoState,
        onBookmarkNoteChange = viewModel::setBookmarkNote,
        selectionState = selectionState,
        onEnterSelectionMode = viewModel::enterSelectionMode,
        onExitSelectionMode = viewModel::exitSelectionMode,
        onToggleSelection = viewModel::toggleSelection,
        onToggleSelectAll = viewModel::toggleSelectAll,
        onRemoveSelected = viewModel::removeSelectedBookmarks,
    )
}

/**
 * Displays the user's bookmarked articles. Includes support for loading and empty states, personal
 * notes on bookmarks, and a selection mode for removing several bookmarks at once.
 *
 * @param removedBookmarkCount Number of bookmarks just removed that can be undone, or 0 if none.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
@Composable
internal fun BookmarksScreen(
    feedState: NewsFeedUiState,
    onShowSnackbar: suspend (String, String?) -> Boolean,
    removeFromBookmarks: (String) -> Unit,
    onNewsResourceViewed: (String) -> Unit,
    onTopicClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    removedBookmarkCount: Int = 0,
    undoBookmarkRemoval: () -> Unit = {},
    clearUndoState: () -> Unit = {},
    onBookmarkNoteChange: (String, String?) -> Unit = { _, _ -> },
    selectionState: BookmarksSelectionUiState = BookmarksSelectionUiState(),
    onEnterSelectionMode: (String?) -> Unit = {},
    onExitSelectionMode: () -> Unit = {},
    onToggleSelection: (String) -> Unit = {},
    onToggleSelectAll: () -> Unit = {},
    onRemoveSelected: () -> Unit = {},
) {
    val bookmarkRemovedMessage = pluralStringResource(
        id = R.plurals.feature_bookmarks_api_removed,
        count = removedBookmarkCount,
        removedBookmarkCount,
    )
    val undoText = stringResource(id = R.string.feature_bookmarks_api_undo)

    LaunchedEffect(removedBookmarkCount) {
        if (removedBookmarkCount > 0) {
            val snackBarResult = onShowSnackbar(bookmarkRemovedMessage, undoText)
            if (snackBarResult) {
                undoBookmarkRemoval()
            } else {
                clearUndoState()
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        clearUndoState()
    }

    BackHandler(enabled = selectionState.isSelectionMode, onBack = onExitSelectionMode)

    when (feedState) {
        Loading -> LoadingState(modifier)
        is Success -> if (feedState.feed.isNotEmpty()) {
            BookmarksGrid(
                feedState = feedState,
                removeFromBookmarks = removeFromBookmarks,
                onNewsResourceViewed = onNewsResourceViewed,
                onTopicClick = onTopicClick,
                onBookmarkNoteChange = onBookmarkNoteChange,
                selectionState = selectionState,
                onEnterSelectionMode = onEnterSelectionMode,
                onExitSelectionMode = onExitSelectionMode,
                onToggleSelection = onToggleSelection,
                onToggleSelectAll = onToggleSelectAll,
                onRemoveSelected = onRemoveSelected,
                modifier = modifier,
            )
        } else {
            EmptyState(modifier)
        }
    }

    TrackScreenViewEvent(screenName = "Saved")
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    NiaLoadingWheel(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize()
            .testTag("forYou:loading"),
        contentDesc = stringResource(id = R.string.feature_bookmarks_api_loading),
    )
}

@Composable
private fun BookmarksGrid(
    feedState: Success,
    removeFromBookmarks: (String) -> Unit,
    onNewsResourceViewed: (String) -> Unit,
    onTopicClick: (String) -> Unit,
    onBookmarkNoteChange: (String, String?) -> Unit,
    selectionState: BookmarksSelectionUiState,
    onEnterSelectionMode: (String?) -> Unit,
    onExitSelectionMode: () -> Unit,
    onToggleSelection: (String) -> Unit,
    onToggleSelectAll: () -> Unit,
    onRemoveSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var noteEditorNewsResourceId by rememberSaveable { mutableStateOf<String?>(null) }
    val scrollableState = rememberLazyStaggeredGridState()
    TrackScrollJank(scrollableState = scrollableState, stateName = "bookmarks:grid")
    Column(modifier = modifier.fillMaxSize()) {
        BookmarksToolbar(
            selectionState = selectionState,
            onEnterSelectionMode = { onEnterSelectionMode(null) },
            onExitSelectionMode = onExitSelectionMode,
            onToggleSelectAll = onToggleSelectAll,
            onRemoveSelected = onRemoveSelected,
        )
        Box(modifier = Modifier.fillMaxSize()) {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(300.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalItemSpacing = 24.dp,
                state = scrollableState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("bookmarks:feed"),
            ) {
                bookmarksFeed(
                    feed = feedState.feed,
                    selectionState = selectionState,
                    removeFromBookmarks = removeFromBookmarks,
                    onNewsResourceViewed = onNewsResourceViewed,
                    onTopicClick = onTopicClick,
                    onNoteClick = { noteEditorNewsResourceId = it },
                    onEnterSelectionMode = onEnterSelectionMode,
                    onToggleSelection = onToggleSelection,
                )
                item(span = StaggeredGridItemSpan.FullLine) {
                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.safeDrawing))
                }
            }
            val itemsAvailable = feedState.feed.size
            val scrollbarState = scrollableState.scrollbarState(
                itemsAvailable = itemsAvailable,
            )
            scrollableState.DraggableScrollbar(
                modifier = Modifier
                    .fillMaxHeight()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(horizontal = 2.dp)
                    .align(Alignment.CenterEnd),
                state = scrollbarState,
                orientation = Orientation.Vertical,
                onThumbMoved = scrollableState.rememberDraggableScroller(
                    itemsAvailable = itemsAvailable,
                ),
            )
        }
    }

    // Close the editor if its bookmark disappears, e.g. when it was removed on another screen.
    val noteEditorResource = feedState.feed.find { it.id == noteEditorNewsResourceId }
    if (noteEditorResource != null && !selectionState.isSelectionMode) {
        key(noteEditorResource.id) {
            val closeEditor = { noteEditorNewsResourceId = null }
            BookmarkNoteDialog(
                initialNote = noteEditorResource.note,
                onSave = { note ->
                    onBookmarkNoteChange(noteEditorResource.id, note)
                    closeEditor()
                },
                onDismiss = closeEditor,
                onDelete = if (noteEditorResource.note != null) {
                    {
                        onBookmarkNoteChange(noteEditorResource.id, null)
                        closeEditor()
                    }
                } else {
                    null
                },
            )
        }
    }
}

/**
 * Bookmarked news resource cards. Outside selection mode, tapping a card opens it and tapping its
 * note edits the note. In selection mode, tapping anywhere on a card toggles its selection.
 */
private fun LazyStaggeredGridScope.bookmarksFeed(
    feed: List<UserNewsResource>,
    selectionState: BookmarksSelectionUiState,
    removeFromBookmarks: (String) -> Unit,
    onNewsResourceViewed: (String) -> Unit,
    onTopicClick: (String) -> Unit,
    onNoteClick: (String) -> Unit,
    onEnterSelectionMode: (String?) -> Unit,
    onToggleSelection: (String) -> Unit,
) {
    items(
        items = feed,
        key = { it.id },
        contentType = { "newsFeedItem" },
    ) { userNewsResource ->
        val context = LocalContext.current
        val analyticsHelper = LocalAnalyticsHelper.current
        val backgroundColor = MaterialTheme.colorScheme.background.toArgb()
        val isSelectionMode = selectionState.isSelectionMode

        NewsResourceCardExpanded(
            userNewsResource = userNewsResource,
            isBookmarked = userNewsResource.isSaved,
            hasBeenViewed = userNewsResource.hasBeenViewed,
            onToggleBookmark = { removeFromBookmarks(userNewsResource.id) },
            onClick = {
                if (isSelectionMode) {
                    onToggleSelection(userNewsResource.id)
                } else {
                    analyticsHelper.logNewsResourceOpened(newsResourceId = userNewsResource.id)
                    launchCustomChromeTab(
                        context,
                        Uri.parse(userNewsResource.url),
                        backgroundColor,
                    )
                    onNewsResourceViewed(userNewsResource.id)
                }
            },
            onTopicClick = { if (!isSelectionMode) onTopicClick(it) },
            note = userNewsResource.note,
            onNoteClick = if (isSelectionMode) null else ({ onNoteClick(userNewsResource.id) }),
            onLongClick = {
                if (isSelectionMode) {
                    onToggleSelection(userNewsResource.id)
                } else {
                    onEnterSelectionMode(userNewsResource.id)
                }
            },
            isSelectable = isSelectionMode,
            isSelected = userNewsResource.id in selectionState.selectedIds,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .animateItem(),
        )
    }
}

/**
 * Offers to enter selection mode, or, in selection mode, shows the selected count along with the
 * select all, remove and cancel actions.
 */
@Composable
private fun BookmarksToolbar(
    selectionState: BookmarksSelectionUiState,
    onEnterSelectionMode: () -> Unit,
    onExitSelectionMode: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onRemoveSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("bookmarks:toolbar"),
    ) {
        if (!selectionState.isSelectionMode) {
            NiaTextButton(onClick = onEnterSelectionMode) {
                Text(stringResource(R.string.feature_bookmarks_api_select))
            }
            return@Row
        }
        IconButton(onClick = onExitSelectionMode) {
            Icon(
                imageVector = NiaIcons.Close,
                contentDescription = stringResource(R.string.feature_bookmarks_api_cancel_selection),
            )
        }
        Text(
            text = pluralStringResource(
                R.plurals.feature_bookmarks_api_selected_count,
                selectionState.selectedCount,
                selectionState.selectedCount,
            ),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        NiaTextButton(onClick = onToggleSelectAll) {
            Text(
                stringResource(
                    if (selectionState.isAllSelected) {
                        R.string.feature_bookmarks_api_deselect_all
                    } else {
                        R.string.feature_bookmarks_api_select_all
                    },
                ),
            )
        }
        NiaTextButton(
            onClick = onRemoveSelected,
            enabled = selectionState.selectedCount > 0,
        ) {
            Text(stringResource(R.string.feature_bookmarks_api_remove_selected))
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
            .testTag("bookmarks:empty"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val iconTint = LocalTintTheme.current.iconTint
        Image(
            modifier = Modifier.fillMaxWidth(),
            painter = painterResource(id = R.drawable.feature_bookmarks_api_mg_empty_bookmarks),
            colorFilter = if (iconTint != Color.Unspecified) ColorFilter.tint(iconTint) else null,
            contentDescription = null,
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = stringResource(id = R.string.feature_bookmarks_api_empty_error),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = R.string.feature_bookmarks_api_empty_description),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Preview
@Composable
private fun LoadingStatePreview() {
    NiaTheme {
        LoadingState()
    }
}

@Preview
@Composable
private fun BookmarksGridPreview(
    @PreviewParameter(UserNewsResourcePreviewParameterProvider::class)
    userNewsResources: List<UserNewsResource>,
) {
    NiaTheme {
        BookmarksScreen(
            feedState = Success(userNewsResources),
            onShowSnackbar = { _, _ -> false },
            removeFromBookmarks = {},
            onNewsResourceViewed = {},
            onTopicClick = {},
        )
    }
}

@Preview
@Composable
private fun BookmarksGridSelectionModePreview(
    @PreviewParameter(UserNewsResourcePreviewParameterProvider::class)
    userNewsResources: List<UserNewsResource>,
) {
    NiaTheme {
        BookmarksScreen(
            feedState = Success(userNewsResources),
            onShowSnackbar = { _, _ -> false },
            removeFromBookmarks = {},
            onNewsResourceViewed = {},
            onTopicClick = {},
            selectionState = BookmarksSelectionUiState(
                isSelectionMode = true,
                selectedIds = setOf(userNewsResources.first().id),
                totalCount = userNewsResources.size,
            ),
        )
    }
}

@Preview
@Composable
private fun EmptyStatePreview() {
    NiaTheme {
        EmptyState()
    }
}
