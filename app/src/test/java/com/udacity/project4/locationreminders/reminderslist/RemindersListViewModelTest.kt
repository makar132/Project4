package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {
    companion object {
        lateinit var dataSource: FakeDataSource
        lateinit var viewModel: RemindersListViewModel

        @BeforeClass
        @JvmStatic
        fun setUp() {
            dataSource = FakeDataSource()
        }

    }

    @get:Rule
    val executorRule = InstantTaskExecutorRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()
    //TODO: provide testing to the RemindersListViewModel and its live data objects


    @After
    fun tearDown() = runBlockingTest {
        stopKoin()
        dataSource.clear()

    }

    @Test
    fun check_loading() = coroutineRule.runBlockingTest {
        //Given
        val reminder = ReminderDTO("title", "description", "Location", 0.0, 0.0, "1")
        dataSource.saveReminder(reminder)
        viewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), dataSource)
        //When
        coroutineRule.pauseDispatcher()
        viewModel.loadReminders()
        val loadingBeforeCoroutine = viewModel.showLoading.getOrAwaitValue()
        coroutineRule.resumeDispatcher()
        val loadingAfterCoroutine = viewModel.showLoading.getOrAwaitValue()
        var viewModelReminder: ReminderDTO? = null

        viewModel.remindersList.getOrAwaitValue()[0].apply {
            viewModelReminder = ReminderDTO(title, description, location, latitude, longitude, id)
        }

        //Then
        assertThat(loadingBeforeCoroutine, `is`(true))
        assertThat(loadingAfterCoroutine, `is`(false))
        assertThat(viewModelReminder, `is`(reminder))


    }

    @Test
    fun loadReminders_shouldReturnError() = runBlockingTest {
        //Given
        viewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), dataSource)
        dataSource.setError(true)
        //When
        viewModel.loadReminders()
        //Then
        assertThat(
            viewModel
                .showSnackBar
                .getOrAwaitValue(),
            `is`("has error")
        )
    }

    @Test
    fun loadReminders_emptyDataSource_showNoDataIsTrue() {
        //Given : empty data source
        viewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), dataSource)
        //When
        viewModel.loadReminders()
        //Then
        val error = viewModel.showNoData.getOrAwaitValue()
        assertThat(error, `is`(true))
    }

    @Test
    fun loadReminders_emptyDataSource_reminderListIsEmpty() {
        //Given
        viewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), dataSource)
        //When
        viewModel.loadReminders()
        //Then
        val reminderList = viewModel.remindersList.getOrAwaitValue()
        assertThat(reminderList).isEmpty()
    }

    @Test
    fun loadReminders_populatedDataSource_reminderListIsNotEmptyAndDataIsCorrect() =
        runBlockingTest {
            //Given : populated data source
            val reminder = ReminderDTO("title", "description", "Location", 0.0, 0.0, "1")
            dataSource.saveReminder(reminder)
            viewModel =
                RemindersListViewModel(ApplicationProvider.getApplicationContext(), dataSource)
            //When
            viewModel.loadReminders()
            //Then
            val reminderList = viewModel.remindersList.getOrAwaitValue()
            assertThat(reminderList).isNotEmpty()
            var firstReminder: ReminderDTO? = null
            reminderList.first().apply {
                firstReminder = ReminderDTO(title, description, location, latitude, longitude, id)
            }
            assertThat(firstReminder).isEqualTo(reminder)
        }

}