package com.udacity.project4.locationreminders.reminderslist

import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import org.mockito.Mockito
import org.mockito.Mockito.verify


@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest : AutoCloseKoinTest() {
    lateinit var dataSource: ReminderDataSource

    @get:Rule
    var instantTask = InstantTaskExecutorRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }

    @Before
    fun init() = runBlockingTest {
        stopKoin()

        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    getApplicationContext(),
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    getApplicationContext(),
                    get() as ReminderDataSource
                )
            }

            single {
                RemindersLocalRepository(get()) as ReminderDataSource
            }
            single {
                LocalDB.createRemindersDao(getApplicationContext())
            }
        }

        startKoin {
            modules(listOf(myModule))
        }


        dataSource = get()

        runBlocking {
            dataSource.deleteAllReminders()
        }
    }


    //    TODO: test the navigation of the fragments.
    @Test
    fun navigationTest() = runBlockingTest {
        val navController = Mockito.mock(NavController::class.java)
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme).onFragment {
            it.view?.let { view ->
                Navigation.setViewNavController(view, navController)
            }

        }
        //SystemClock.sleep(2000)
        onView(withId(R.id.addReminderFAB))
            .perform(click())
        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
        //SystemClock.sleep(2000)

    }

    //    TODO: test the displayed data on the UI.
    @Test
    fun emptyDataSource_noDataDisplayed() {
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme).onFragment { }
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
        //SystemClock.sleep(2000)
    }

    @Test
    fun populatedDataSource_dataDisplayed() = runBlockingTest {
        val reminderList = listOf(
            ReminderDTO("title1", "description1", "location1", null, null),
            ReminderDTO("title2", "description2", "location2", null, null)
        )
        var myActivity: FragmentActivity? = null
        runBlocking {
            reminderList.forEach {
                dataSource.saveReminder(it)
            }
        }

        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme).onFragment {
            myActivity = it.requireActivity()
        }
        onView(withId(R.id.noDataTextView)).check(matches(not(isDisplayed())))
        onView(withId(R.id.reminderssRecyclerView)).check(matches(isDisplayed()))


        //SystemClock.sleep(2000)
    }

    //    TODO: add testing for the error messages.
    @Test
    fun checkErrorMessageDisplayed() = runBlockingTest {

        var myActivity: FragmentActivity? = null
        val errorMessage = "Error"
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme).onFragment {
            it._viewModel.showErrorMessage.postValue(errorMessage)
            myActivity = it.requireActivity()
        }
        onView(withText(errorMessage)).inRoot(
            withDecorView(
                not(
                    `is`(
                        myActivity?.window?.decorView
                    )
                )
            )
        ).check(
            matches(
                isDisplayed()
            )
        )


        //SystemClock.sleep(2000)
    }


}