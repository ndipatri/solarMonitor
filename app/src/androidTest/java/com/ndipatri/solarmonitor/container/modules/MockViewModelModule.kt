package com.ndipatri.solarmonitor.container.modules

import android.arch.lifecycle.ViewModel
import android.content.Context
import com.ndipatri.solarmonitor.activities.MainActivityViewModel
import com.ndipatri.solarmonitor.container.MainActivityViewModelFactory
import dagger.Provides
import org.mockito.Mockito.mock
import javax.inject.Singleton


@dagger.Module
class MockViewModelModule(internal var context: Context) {

    @Provides
    @Singleton
    internal fun provideMainActivityViewModelFactory(context: Context): MainActivityViewModelFactory {
        return MockMainActivityViewModelFactory(context)
    }
}

class MockMainActivityViewModelFactory(context: Context): MainActivityViewModelFactory(context) {
    val mockMainActivityViewModel = mock(MainActivityViewModel::class.java)

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return mockMainActivityViewModel as T
    }
}
