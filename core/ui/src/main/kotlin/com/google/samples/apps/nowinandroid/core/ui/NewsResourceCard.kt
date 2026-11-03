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

package com.google.samples.apps.nowinandroid.core.ui

import android.content.ClipData
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.google.samples.apps.nowinandroid.core.designsystem.R.drawable
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaIconToggleButton
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaTopicTag
import com.google.samples.apps.nowinandroid.core.designsystem.icon.NiaIcons
import com.google.samples.apps.nowinandroid.core.designsystem.theme.NiaTheme
import com.google.samples.apps.nowinandroid.core.model.data.FollowableTopic
import com.google.samples.apps.nowinandroid.core.model.data.NewsResource
import com.google.samples.apps.nowinandroid.core.model.data.UserNewsResource
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toJavaZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * [NewsResource] card used on the following screens: For You, Saved
 *
 * @param note The user's personal note, displayed below the title when not null.
 * @param onNoteClick Called when the note is tapped. When provided and there is no [note], an
 * "Add a note" prompt is displayed instead. Pass null to make the note read-only.
 * @param onLongClick Called when the card is long-pressed, or null to disable long-press.
 * @param isSelectable Whether the card is in selection mode, where a checkbox reflecting
 * [isSelected] replaces the bookmark button.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NewsResourceCardExpanded(
    userNewsResource: UserNewsResource,
    isBookmarked: Boolean,
    hasBeenViewed: Boolean,
    onToggleBookmark: () -> Unit,
    onClick: () -> Unit,
    onTopicClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    note: String? = null,
    onNoteClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    isSelectable: Boolean = false,
    isSelected: Boolean = false,
) {
    val clickActionLabel = if (isSelectable) {
        stringResource(R.string.core_ui_card_select_action)
    } else {
        stringResource(R.string.core_ui_card_tap_action)
    }
    val longClickActionLabel = stringResource(R.string.core_ui_card_select_action)
    val sharingLabel = stringResource(R.string.core_ui_feed_sharing)
    val sharingContent = stringResource(
        R.string.core_ui_feed_sharing_data,
        userNewsResource.title,
        userNewsResource.url,
    )

    val dragAndDropFlags = if (VERSION.SDK_INT >= VERSION_CODES.N) {
        View.DRAG_FLAG_GLOBAL
    } else {
        0
    }

    val shape = RoundedCornerShape(16.dp)
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        // Use custom labels for accessibility services to communicate the card's actions to user.
        modifier = modifier
            .clip(shape)
            .combinedClickable(
                onClickLabel = clickActionLabel,
                onClick = onClick,
                onLongClickLabel = if (onLongClick != null) longClickActionLabel else null,
                onLongClick = onLongClick,
            )
            .semantics {
                if (isSelectable) selected = isSelected
            }
            .testTag("newsResourceCard:${userNewsResource.id}"),
    ) {
        Column {
            if (!userNewsResource.headerImageUrl.isNullOrEmpty()) {
                Row {
                    NewsResourceHeaderImage(userNewsResource.headerImageUrl)
                }
            }
            Box(
                modifier = Modifier.padding(16.dp),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row {
                        NewsResourceTitle(
                            userNewsResource.title,
                            modifier = Modifier
                                .fillMaxWidth((.8f))
                                .then(
                                    // The drag source consumes taps on the title, which must toggle
                                    // the selection instead while selecting.
                                    if (isSelectable) {
                                        Modifier
                                    } else {
                                        Modifier.dragAndDropSource { _ ->
                                            DragAndDropTransferData(
                                                ClipData.newPlainText(
                                                    sharingLabel,
                                                    sharingContent,
                                                ),
                                                flags = dragAndDropFlags,
                                            )
                                        }
                                    },
                                ),
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (isSelectable) {
                            // The whole card toggles the selection, so the checkbox is display-only.
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("newsResourceCard:checkbox"),
                            )
                        } else {
                            BookmarkButton(isBookmarked, onToggleBookmark)
                        }
                    }
                    if (note != null || onNoteClick != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        BookmarkNote(note = note, onClick = onNoteClick)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!hasBeenViewed) {
                            NotificationDot(
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(8.dp),
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                        }
                        NewsResourceMetaData(userNewsResource.publishDate, userNewsResource.type)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    NewsResourceShortDescription(userNewsResource.content)
                    Spacer(modifier = Modifier.height(12.dp))
                    NewsResourceTopics(
                        topics = userNewsResource.followableTopics,
                        onTopicClick = onTopicClick,
                    )
                }
            }
        }
    }
}

@Composable
fun NewsResourceHeaderImage(
    headerImageUrl: String?,
) {
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    val imageLoader = rememberAsyncImagePainter(
        model = headerImageUrl,
        onState = { state ->
            isLoading = state is AsyncImagePainter.State.Loading
            isError = state is AsyncImagePainter.State.Error
        },
    )
    val isLocalInspection = LocalInspectionMode.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            // Display a progress bar while loading
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(80.dp),
                color = MaterialTheme.colorScheme.tertiary,
            )
        }

        Image(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentScale = ContentScale.Crop,
            painter = if (isError.not() && !isLocalInspection) {
                imageLoader
            } else {
                painterResource(drawable.core_designsystem_ic_placeholder_default)
            },
            // TODO b/226661685: Investigate using alt text of  image to populate content description
            // decorative image,
            contentDescription = null,
        )
    }
}

@Composable
fun NewsResourceTitle(
    newsResourceTitle: String,
    modifier: Modifier = Modifier,
) {
    Text(newsResourceTitle, style = MaterialTheme.typography.headlineSmall, modifier = modifier)
}

/**
 * The user's personal [note] on a bookmarked news resource, or an "Add a note" prompt when [note]
 * is null. Tapping it calls [onClick]; pass null to make it read-only.
 */
@Composable
fun BookmarkNote(
    note: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        onClickLabel = stringResource(R.string.core_ui_bookmark_note_edit_action),
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("newsResourceCard:note"),
    ) {
        Icon(
            imageVector = if (note != null) NiaIcons.ShortText else NiaIcons.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = note ?: stringResource(R.string.core_ui_bookmark_note_add),
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = if (note != null) FontStyle.Italic else FontStyle.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun BookmarkButton(
    isBookmarked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NiaIconToggleButton(
        checked = isBookmarked,
        onCheckedChange = { onClick() },
        modifier = modifier,
        icon = {
            Icon(
                imageVector = NiaIcons.BookmarkBorder,
                contentDescription = stringResource(R.string.core_ui_bookmark),
            )
        },
        checkedIcon = {
            Icon(
                imageVector = NiaIcons.Bookmark,
                contentDescription = stringResource(R.string.core_ui_unbookmark),
            )
        },
    )
}

@Composable
fun NotificationDot(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.core_ui_unread_resource_dot_content_description)
    Canvas(
        modifier = modifier
            .semantics { contentDescription = description },
        onDraw = {
            drawCircle(
                color,
                radius = size.minDimension / 2,
            )
        },
    )
}

@Composable
fun dateFormatted(publishDate: Instant): String = DateTimeFormatter
    .ofLocalizedDate(FormatStyle.MEDIUM)
    .withLocale(Locale.getDefault())
    .withZone(LocalTimeZone.current.toJavaZoneId())
    .format(publishDate.toJavaInstant())

@Composable
fun NewsResourceMetaData(
    publishDate: Instant,
    resourceType: String,
) {
    val formattedDate = dateFormatted(publishDate)
    Text(
        if (resourceType.isNotBlank()) {
            stringResource(R.string.core_ui_card_meta_data_text, formattedDate, resourceType)
        } else {
            formattedDate
        },
        style = MaterialTheme.typography.labelSmall,
    )
}

@Composable
fun NewsResourceShortDescription(
    newsResourceShortDescription: String,
) {
    Text(newsResourceShortDescription, style = MaterialTheme.typography.bodyLarge)
}

@Composable
fun NewsResourceTopics(
    topics: List<FollowableTopic>,
    onTopicClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        // causes narrow chips
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (followableTopic in topics) {
            NiaTopicTag(
                followed = followableTopic.isFollowed,
                onClick = { onTopicClick(followableTopic.topic.id) },
                text = {
                    val contentDescription = if (followableTopic.isFollowed) {
                        stringResource(
                            R.string.core_ui_topic_chip_content_description_when_followed,
                            followableTopic.topic.name,
                        )
                    } else {
                        stringResource(
                            R.string.core_ui_topic_chip_content_description_when_not_followed,
                            followableTopic.topic.name,
                        )
                    }
                    Text(
                        text = followableTopic.topic.name.uppercase(Locale.getDefault()),
                        modifier = Modifier
                            .semantics {
                                this.contentDescription = contentDescription
                            }
                            .testTag("topicTag:${followableTopic.topic.id}"),
                    )
                },
            )
        }
    }
}

@Preview("Bookmark Button")
@Composable
private fun BookmarkButtonPreview() {
    NiaTheme {
        Surface {
            BookmarkButton(isBookmarked = false, onClick = { })
        }
    }
}

@Preview("Bookmark Button Bookmarked")
@Composable
private fun BookmarkButtonBookmarkedPreview() {
    NiaTheme {
        Surface {
            BookmarkButton(isBookmarked = true, onClick = { })
        }
    }
}

@Preview("NewsResourceCardExpanded")
@Composable
private fun ExpandedNewsResourcePreview(
    @PreviewParameter(UserNewsResourcePreviewParameterProvider::class)
    userNewsResources: List<UserNewsResource>,
) {
    CompositionLocalProvider(
        LocalInspectionMode provides true,
    ) {
        NiaTheme {
            Surface {
                NewsResourceCardExpanded(
                    userNewsResource = userNewsResources[0],
                    isBookmarked = true,
                    hasBeenViewed = false,
                    onToggleBookmark = {},
                    onClick = {},
                    onTopicClick = {},
                )
            }
        }
    }
}
