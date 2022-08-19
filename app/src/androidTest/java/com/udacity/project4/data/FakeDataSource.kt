package com.udacity.project4.data

import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(
    var reminders: MutableList<ReminderDTO>? = mutableListOf(),
    var hasError: Boolean = false
) :
    ReminderDataSource {

//    TODO: Create a fake data source to act as a double to the real data source

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        // TODO("Return the reminders")
        if (hasError) {
            return Result.Error(message = "has error")
        }
        reminders?.let {
            return Result.Success(ArrayList(it))
        }
        return Result.Error(message = "no reminders found")

    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        //TODO("save the reminder")
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        //TODO("return the reminder with the id")
        if (hasError) {
            return Result.Error(message = "has error")
        }
        reminders?.let {
            return Result.Success(data = reminders!!.first { it.id == id })
        }
        return Result.Error("Reminder not found")
    }

    override suspend fun deleteAllReminders() {
        //TODO("delete all the reminders")
        reminders?.clear()
    }

    fun setError(errorBoolean: Boolean) {
        hasError = errorBoolean
    }
    suspend fun clear(){
        this.deleteAllReminders()
        setError(false)
    }

}