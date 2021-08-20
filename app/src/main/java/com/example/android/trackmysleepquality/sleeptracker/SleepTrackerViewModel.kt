/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import androidx.lifecycle.*
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.*

/**
 * ViewModel for SleepTrackerFragment.
 */
class SleepTrackerViewModel(
    val database: SleepDatabaseDao,
    application: Application
) : AndroidViewModel(application) {

    val viewModelJob = Job()

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
//
//    private val _eventStarted = MutableLiveData<Boolean>(false)
//    val eventStarted: LiveData<Boolean>
//        get() = _eventStarted


    private val _showSnackbarEvent=MutableLiveData<Boolean>()

    val showSnackbarEvent:LiveData<Boolean>
    get() = _showSnackbarEvent

    private val _navigateToSleepQuality=MutableLiveData<SleepNight>()

    val navigateToSleepQuality:LiveData<SleepNight>
    get() = _navigateToSleepQuality

    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private var tonight = MutableLiveData<SleepNight?>()

    private val nights = database.getAllNights()

    val nightsSting = Transformations.map(nights) { nights ->
        formatNights(nights, application.resources)

    }

    init {
        initializeTonight()
    }

    fun doneNavigating(){
        _navigateToSleepQuality.value=null
    }

    private fun initializeTonight() {
        uiScope.launch {
            tonight.value = getTonight()
        }
    }

    val startButtonVisible=Transformations.map(tonight){
        it==null
    }

    val stopButtonVisible=Transformations.map(tonight){
        it!=null
    }

    val clearButtonVisible=Transformations.map(nights){
        it?.isNotEmpty()
    }

    private suspend fun getTonight(): SleepNight? {
        return withContext(Dispatchers.IO) {
            var night = database.getTonight()
            if (night?.endTime != night?.startTime) {
                night = null
            }
            night
        }
    }

    fun onStartTracking() {
        uiScope.launch {

                val newNight = SleepNight()
                insert(newNight)
                tonight.value = getTonight()

        }
    }

    private suspend fun insert(newNight: SleepNight) {
        withContext(Dispatchers.IO) {
            database.insert(newNight)
        }
    }

    fun onStopTracking() {
        uiScope.launch {

                val oldNight = tonight.value ?: return@launch
                oldNight.endTime = System.currentTimeMillis()
                update(oldNight)
                _navigateToSleepQuality.value=oldNight
        }
    }

    private suspend fun update(newNight: SleepNight) {
        withContext(Dispatchers.IO) {
            database.update(newNight)
        }
    }

    fun doneSnackbarShowing(){
        _showSnackbarEvent.value=false
    }

    fun onClear() {
        uiScope.launch {
            clear()
            _showSnackbarEvent.value=true
            tonight.value = null
        }
    }

    private suspend fun clear() {
        withContext(Dispatchers.IO) {
            database.clear()
        }
    }
}

