package com.ndipatri.solarmonitor.container

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ndipatri.solarmonitor.activities.MainActivityViewModel
import dagger.Provides
import javax.inject.Singleton


@dagger.Module
class ViewModelModule(internal var context: Context) {

    @Provides
    @Singleton
    internal fun provideMainActivityViewModel(context: Context): MainActivityViewModelFactory {
        return MainActivityViewModelFactory(context)
    }
}

open class MainActivityViewModelFactory(var context: Context): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MainActivityViewModel(context as Application) as T
    }
}
