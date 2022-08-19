package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import androidx.test.InstrumentationRegistry
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.nullValue
import org.junit.*
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {
    companion object {
         val dataSource=FakeDataSource()
        lateinit var viewModel: SaveReminderViewModel
        lateinit var appContext: Application



    }

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Before
    fun setUp(){

        appContext = ApplicationProvider.getApplicationContext()
    }
    @After
    fun tearDown() = runBlockingTest {
        stopKoin()
        dataSource.clear()

    }

    @Test
    fun check_loading() = coroutineRule.runBlockingTest {
        //Given
        viewModel = SaveReminderViewModel(appContext, dataSource)
        val reminder = ReminderDataItem("title", "description", "location", 0.0, 0.0)
        //When
        coroutineRule.pauseDispatcher()
        viewModel.saveReminder(reminder)
        val loadingBeforeCoroutine = viewModel.showLoading.getOrAwaitValue()
        coroutineRule.resumeDispatcher()
        val loadingAfterCoroutine = viewModel.showLoading.getOrAwaitValue()
        var dataSourceReminder: ReminderDataItem? = null
        val dataSourceReminderList = (dataSource.getReminders())

        if (dataSourceReminderList is Result.Success<*>) {
            val dataList = ArrayList<ReminderDataItem>()
            dataList.addAll((dataSourceReminderList.data as List<ReminderDTO>).map { reminder ->
                ReminderDataItem(
                    reminder.title,
                    reminder.description,
                    reminder.location,
                    reminder.latitude,
                    reminder.longitude,
                    reminder.id
                )
            })
            dataSourceReminder = dataList[0]
        }


        //Then
        assertThat(loadingBeforeCoroutine, `is`(true))
        assertThat(loadingAfterCoroutine, `is`(false))
        assertThat(reminder, `is`(dataSourceReminder))


    }

    @Test
    fun onClear_allLiveDataObjectsAreClear() {
        //Given
        viewModel = SaveReminderViewModel(appContext, dataSource)
        //When
        viewModel.onClear()
        //Then
        assertThat(viewModel.reminderTitle.getOrAwaitValue(), `is`(nullValue()))
        assertThat(viewModel.reminderDescription.getOrAwaitValue(), `is`(nullValue()))
        assertThat(viewModel.reminderSelectedLocationStr.getOrAwaitValue(), `is`(nullValue()))
        assertThat(viewModel.selectedPOI.getOrAwaitValue(), `is`(nullValue()))
        assertThat(viewModel.latitude.getOrAwaitValue(), `is`(nullValue()))
        assertThat(viewModel.longitude.getOrAwaitValue(), `is`(nullValue()))

    }

    @Test
    fun saveReminder_reminderDataItem_reminderSaveSuccess() {
        //Given
        viewModel = SaveReminderViewModel(appContext, dataSource)
        dataSource.setError(true)
        val reminder = ReminderDataItem("title", "description", "location", 0.0, 0.0)
        //When
        viewModel.saveReminder(reminder)
        //Then
        val toast = viewModel.showToast.getOrAwaitValue() ?: null
        val expectedToast =
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.reminder_saved)
        assertThat(toast, `is`(expectedToast))


    }

    //TODO: provide testing to the SaveReminderView and its live data objects
    @Test
    fun validateEnteredData_nullTitle_showSnackBarPleaseEnterTitle() {
        //Given
        viewModel = SaveReminderViewModel(appContext, dataSource)
        val reminder = ReminderDataItem(null, "description", "location", 0.0, 0.0)
        //When
        viewModel.validateEnteredData(reminder)
        //Then
        val snackBarInt = viewModel.showSnackBarInt.getOrAwaitValue()

        assertThat(snackBarInt, `is`(R.string.err_enter_title))

    }

    @Test
    fun validateEnteredData_emptyTitle_showSnackBarPleaseEnterTitle() {
        //Given
        viewModel = SaveReminderViewModel(appContext, dataSource)
        val reminder = ReminderDataItem("", "description", "location", 0.0, 0.0)
        //When
        viewModel.validateEnteredData(reminder)
        //Then
        val snackBarInt = viewModel.showSnackBarInt.getOrAwaitValue()

        assertThat(snackBarInt, `is`(R.string.err_enter_title))


    }

    @Test
    fun validateEnteredData_nullLocation_showSnackBarPleaseSelectLocation() {
        //Given
        viewModel = SaveReminderViewModel(appContext, dataSource)
        val reminder = ReminderDataItem("title", "description", null, 0.0, 0.0)
        //When
        viewModel.validateEnteredData(reminder)
        //Then
        val snackBarInt = viewModel.showSnackBarInt.getOrAwaitValue()

        assertThat(snackBarInt, `is`(R.string.err_select_location))

    }

    @Test
    fun validateEnteredData_emptyLocation_showSnackBarPleaseSelectLocation() {
        //Given
        viewModel = SaveReminderViewModel(appContext, dataSource)
        val reminder = ReminderDataItem("title", "description", "", 0.0, 0.0)
        //When
        viewModel.validateEnteredData(reminder)
        //Then
        val snackBarInt = viewModel.showSnackBarInt.getOrAwaitValue()

        assertThat(snackBarInt, `is`(R.string.err_select_location))


    }

    @Test
    fun validateAndSaveEnteredData_validData_showToast() {
        //Given
        viewModel = SaveReminderViewModel(appContext, dataSource)
        val reminder = ReminderDataItem("title", "description", "location", 0.0, 0.0)
        //When
        viewModel.validateAndSaveReminder(reminder)
        //Then
        val toastMessage = viewModel.showToast.getOrAwaitValue()

        assertThat(toastMessage, `is`(appContext.getString(R.string.reminder_saved)))


    }

}