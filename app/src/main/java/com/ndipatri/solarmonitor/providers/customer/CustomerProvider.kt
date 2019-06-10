package com.ndipatri.solarmonitor.providers.customer


import android.content.Context
import com.ndipatri.solarmonitor.persistence.AppDatabase
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Deferred

open class CustomerProvider(var context: Context) {

    val customerDao = AppDatabase.getInstance(context).customerDao()

    suspend fun findCustomerForPanel(panelId: String): Customer {

        var customerName = "Customer $panelId"

        // NJD TODO - In the future, this would be a cloud lookup based on panelId,
        // but for now, we hardcode.. associate our customer with any panel
        // we find.
        Customer(customerName,
                .13671).apply {

            // Because 'insertOrReplaceCustomer' is a suspend function, we know Room
            // will handle switching threads to make this a 'main safe' call.
            customerDao.insertOrReplaceCustomer(this)
        }

        return customerDao.getCustomerByName(customerName)
    }

    open fun deleteAllCustomers(): Completable {
        return Completable.create(){ subscriber ->
            customerDao.deleteAllCustomers()?.let { subscriber.onComplete() }
        }
        .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    companion object {
        private val TAG = CustomerProvider::class.java!!.getSimpleName()
    }
}
