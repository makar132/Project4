package com.udacity.project4.locationreminders

import java.util.concurrent.*
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.*

@VisibleForTesting(otherwise = VisibleForTesting.NONE)
fun <T> LiveData<T>.getOrAwaitValue(
    timer: Long = 2,
    timerUnit: TimeUnit = TimeUnit.SECONDS,
    after: () -> Unit = {}
): T {
    var data: T? = null
    val countDownLatch = CountDownLatch(1)
    val observer = object : Observer<T> {

        override fun onChanged(changed: T?) {
            data = changed
            countDownLatch.countDown()
            this@getOrAwaitValue.removeObserver(this)
        }
    }
    this.observeForever(observer)

    try {
        after.invoke()

        when (!countDownLatch.await(timer, timerUnit)) {
            true -> throw TimeoutException("LiveData value no setter.")
        }

    } finally {
        this.removeObserver(observer)
    }

    return data as T
}