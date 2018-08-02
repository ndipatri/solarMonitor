package com.ndipatri.solarmonitor.container

import android.app.Application
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.content.Context
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
