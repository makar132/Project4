package com.udacity.project4

import android.app.Application
import android.os.SystemClock
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.R.id.*
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers
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

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    @get:Rule
    var instantTask = InstantTaskExecutorRule()

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }


    //    TODO: add End to End testing to the app

    @Test
    fun test() = runBlocking {
        var myActivity: RemindersActivity? = null
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        onView(withId(remindersActivity)).check(matches(isDisplayed()))
        onView(withId(noDataTextView)).check(matches(isDisplayed()))
        onView(withId(addReminderFAB)).perform(click())
        onView(withId(saveReminderFragment)).check(matches(isDisplayed()))
        onView(withId(reminderTitle)).check(matches(withText("")))
        onView(withId(reminderDescription)).check(matches(withText("")))
        val saveButtonViewInteraction = onView(withId(saveReminder))

        saveButtonViewInteraction.perform(click())
        onView(withId(snackbar_text)).check(matches(isDisplayed()))
        onView(withId(snackbar_text)).check(matches(withText(R.string.err_enter_title)))
        SystemClock.sleep(3000)
        onView(withId(reminderTitle)).perform(typeText("title"))
            .check(matches(withText("title")))
        onView(withId(reminderDescription)).perform(typeText("description"))
            .check(matches(withText("description")))

        saveButtonViewInteraction.perform(click())
        onView(withId(snackbar_text)).check(matches(isDisplayed()))
        onView(withId(snackbar_text)).check(matches(withText(R.string.err_select_location)))
        SystemClock.sleep(3000)

        onView(withId(selectLocation)).perform(click())
        SystemClock.sleep(6000)
        onView(withId(save_button)).perform(click())
        onView(withId(saveReminderFragment)).check(matches(isDisplayed()))
        onView(withId(reminderTitle)).check(matches(withText("title")))
        onView(withId(reminderDescription)).check(matches(withText("description")))
        SystemClock.sleep(2000)
        onView(withId(saveReminder)).perform(click())
        SystemClock.sleep(1000)
        onView(withId(remindersActivity)).check(matches(isDisplayed()))
        onView(withText("title")).check(matches(isDisplayed()))
        onView(withText("description")).check(matches(isDisplayed()))
        onView(withText(R.string.reminder_saved)).inRoot(
            RootMatchers.withDecorView(
                Matchers.not(
                    Matchers.`is`(
                        myActivity?.window?.decorView
                    )
                )
            )
        ).check(
            matches(
                isDisplayed()
            )
        )
        //SystemClock.sleep(5000)
        activityScenario.close()
    }

}
