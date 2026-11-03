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

package com.google.samples.apps.nowinandroid.core.datastore

import com.google.samples.apps.nowinandroid.core.datastore.test.InMemoryDataStore
import com.google.samples.apps.nowinandroid.core.model.data.BOOKMARK_NOTE_MAX_LENGTH
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NiaPreferencesDataSourceTest {

    private val testScope = TestScope(UnconfinedTestDispatcher())

    private lateinit var subject: NiaPreferencesDataSource

    @Before
    fun setup() {
        subject = NiaPreferencesDataSource(InMemoryDataStore(UserPreferences.getDefaultInstance()))
    }

    @Test
    fun shouldHideOnboardingIsFalseByDefault() = testScope.runTest {
        assertFalse(subject.userData.first().shouldHideOnboarding)
    }

    @Test
    fun userShouldHideOnboardingIsTrueWhenSet() = testScope.runTest {
        subject.setShouldHideOnboarding(true)
        assertTrue(subject.userData.first().shouldHideOnboarding)
    }

    @Test
    fun userShouldHideOnboarding_unfollowsLastTopic_shouldHideOnboardingIsFalse() =
        testScope.runTest {
            // Given: user completes onboarding by selecting a single topic.
            subject.setTopicIdFollowed("1", true)
            subject.setShouldHideOnboarding(true)

            // When: they unfollow that topic.
            subject.setTopicIdFollowed("1", false)

            // Then: onboarding should be shown again
            assertFalse(subject.userData.first().shouldHideOnboarding)
        }

    @Test
    fun userShouldHideOnboarding_unfollowsAllTopics_shouldHideOnboardingIsFalse() =
        testScope.runTest {
            // Given: user completes onboarding by selecting several topics.
            subject.setFollowedTopicIds(setOf("1", "2"))
            subject.setShouldHideOnboarding(true)

            // When: they unfollow those topics.
            subject.setFollowedTopicIds(emptySet())

            // Then: onboarding should be shown again
            assertFalse(subject.userData.first().shouldHideOnboarding)
        }

    @Test
    fun shouldUseDynamicColorFalseByDefault() = testScope.runTest {
        assertFalse(subject.userData.first().useDynamicColor)
    }

    @Test
    fun userShouldUseDynamicColorIsTrueWhenSet() = testScope.runTest {
        subject.setDynamicColorPreference(true)
        assertTrue(subject.userData.first().useDynamicColor)
    }

    @Test
    fun bookmarkNote_whenBookmarked_isStored() = testScope.runTest {
        subject.setNewsResourceBookmarked("1", true)

        subject.setBookmarkNote("1", "Read before the review")

        assertEquals(mapOf("1" to "Read before the review"), subject.userData.first().bookmarkNotes)
    }

    @Test
    fun bookmarkNote_whenNotBookmarked_isIgnored() = testScope.runTest {
        subject.setBookmarkNote("1", "Orphan note")

        assertEquals(emptyMap(), subject.userData.first().bookmarkNotes)
    }

    @Test
    fun bookmarkNote_isTrimmedAndLimitedToMaxLength() = testScope.runTest {
        subject.setNewsResourceBookmarked("1", true)

        subject.setBookmarkNote("1", "  " + "a".repeat(BOOKMARK_NOTE_MAX_LENGTH + 20))

        assertEquals(
            "a".repeat(BOOKMARK_NOTE_MAX_LENGTH),
            subject.userData.first().bookmarkNotes["1"],
        )
    }

    @Test
    fun bookmarkNote_whenSetBlankOrNull_isDeleted() = testScope.runTest {
        subject.setNewsResourceBookmarked("1", true)
        subject.setNewsResourceBookmarked("2", true)
        subject.setBookmarkNote("1", "Note 1")
        subject.setBookmarkNote("2", "Note 2")

        subject.setBookmarkNote("1", "   ")
        subject.setBookmarkNote("2", null)

        assertEquals(emptyMap(), subject.userData.first().bookmarkNotes)
        assertEquals(setOf("1", "2"), subject.userData.first().bookmarkedNewsResources)
    }

    @Test
    fun unbookmarking_deletesNote() = testScope.runTest {
        subject.setNewsResourceBookmarked("1", true)
        subject.setBookmarkNote("1", "Note")

        subject.setNewsResourceBookmarked("1", false)

        // Re-bookmarking must not bring the deleted note back.
        subject.setNewsResourceBookmarked("1", true)
        assertEquals(emptyMap(), subject.userData.first().bookmarkNotes)
    }

    @Test
    fun rebookmarking_keepsExistingNote() = testScope.runTest {
        subject.setNewsResourceBookmarked("1", true)
        subject.setBookmarkNote("1", "Note")

        subject.setNewsResourceBookmarked("1", true)

        assertEquals(mapOf("1" to "Note"), subject.userData.first().bookmarkNotes)
    }

    @Test
    fun removeBookmarks_removesBookmarksAndNotesAtOnce() = testScope.runTest {
        subject.setNewsResourceBookmarked("1", true)
        subject.setNewsResourceBookmarked("2", true)
        subject.setNewsResourceBookmarked("3", true)
        subject.setBookmarkNote("1", "Note 1")
        subject.setBookmarkNote("3", "Note 3")

        subject.removeBookmarks(setOf("1", "2"))

        val userData = subject.userData.first()
        assertEquals(setOf("3"), userData.bookmarkedNewsResources)
        assertEquals(mapOf("3" to "Note 3"), userData.bookmarkNotes)
    }

    @Test
    fun restoreBookmarks_restoresBookmarksAndNotes() = testScope.runTest {
        subject.restoreBookmarks(mapOf("1" to "Note 1", "2" to null))

        val userData = subject.userData.first()
        assertEquals(setOf("1", "2"), userData.bookmarkedNewsResources)
        assertEquals(mapOf("1" to "Note 1"), userData.bookmarkNotes)
    }
}
