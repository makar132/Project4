package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    //    TODO: Add testing implementation to the RemindersDao.kt
    @get:Rule
    var instantTask = InstantTaskExecutorRule()
    private lateinit var db: RemindersDatabase
    private lateinit var remindersDao: RemindersDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
        remindersDao = db.reminderDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getReminders() = runBlockingTest {
        val reminderList = listOf(
            ReminderDTO("title1", "description1", "location1", null, null),
            ReminderDTO("title2", "description2", "location2", null, null)
        )

        reminderList.forEach {
            remindersDao.saveReminder(it)
        }
        val loaded = remindersDao.getReminders()
        assertThat(loaded, `is`(reminderList))
    }

    @Test
    fun getReminderById() = runBlockingTest {
        val reminder =
            ReminderDTO("title1", "description1", "location1", null, null)


        remindersDao.saveReminder(reminder)

        val loaded = remindersDao.getReminderById(reminder.id)
        assertThat(loaded, `is`(reminder))
    }
    @Test
    fun getReminderById_notFound_shouldReturnError() = runBlockingTest {
        val reminder =
            ReminderDTO("title1", "description1", "location1", null, null)


        val loaded = remindersDao.getReminderById(reminder.id)
        assertThat(loaded, `is`(nullValue()))
    }
    @Test
    fun deleteAllReminders() = runBlockingTest {
        remindersDao.deleteAllReminders()

        val loaded = remindersDao.getReminders()
        assertThat(loaded, empty())

    }

}