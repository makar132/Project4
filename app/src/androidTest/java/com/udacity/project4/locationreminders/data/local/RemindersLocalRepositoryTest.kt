package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.utils.EspressoIdlingResource
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
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    //    TODO: Add testing implementation to the RemindersLocalRepository.kt
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @get:Rule
    var instantTask = InstantTaskExecutorRule()

    private lateinit var db: RemindersDatabase
    private lateinit var remindersDao: RemindersDao
    private lateinit var remindersLocalRepository: RemindersLocalRepository

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
        remindersDao = db.reminderDao()
        remindersLocalRepository = RemindersLocalRepository(remindersDao, coroutineRule.dispatcher)
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
            remindersLocalRepository.saveReminder(it)
        }
        val result = remindersLocalRepository.getReminders()
        var loaded: List<ReminderDTO> = listOf()
        if (result is Result.Success<*>) {
            val dataList = ArrayList<ReminderDTO>()
            dataList.addAll((result.data as List<ReminderDTO>).map { reminder ->
                ReminderDTO(
                    reminder.title,
                    reminder.description,
                    reminder.location,
                    reminder.latitude,
                    reminder.longitude,
                    reminder.id
                )
            })
            loaded = dataList
        }
        assertThat(loaded, `is`(reminderList))
    }

    @Test
    fun getReminderById_dataNotFound_shouldReturnError() = runBlockingTest {
        val reminder =
            ReminderDTO("title1", "description1", "location1", null, null)

        //try to get a reminder that isn't inserted in the database
        val result = remindersLocalRepository.getReminder(reminder.id)
        var loaded: ReminderDTO? = null
        if (result is Result.Success<*>) {
            loaded = (result.data as ReminderDTO)
        }
        //testing that loaded is null is enough
        // as if the result was success loaded will contain the retrieved data
        //but as i got a bad comment on this part in last review i added it :)
        assertThat(loaded, `is`(nullValue()))
        assertThat(result.toString(), `is`(Result.Error("Reminder not found!").toString()))

    }

    @Test
    fun getReminderById() = runBlockingTest {
        val reminder =
            ReminderDTO("title1", "description1", "location1", null, null)


        remindersLocalRepository.saveReminder(reminder)

        val result = remindersLocalRepository.getReminder(reminder.id)
        var loaded: ReminderDTO? = null
        if (result is Result.Success<*>) {
            loaded = (result.data as ReminderDTO)
        }
        assertThat(loaded, `is`(reminder))
    }


    @Test
    fun deleteAllReminders() = runBlockingTest {
        remindersLocalRepository.deleteAllReminders()

        val result = remindersLocalRepository.getReminders()
        var loaded: List<ReminderDTO> = listOf()
        if (result is Result.Success<*>) {
            val dataList = ArrayList<ReminderDTO>()
            dataList.addAll((result.data as List<ReminderDTO>).map { reminder ->
                ReminderDTO(
                    reminder.title,
                    reminder.description,
                    reminder.location,
                    reminder.latitude,
                    reminder.longitude,
                    reminder.id
                )
            })
            loaded = dataList
        }
        assertThat(loaded, empty())

    }

}