package com.ndipatri.solarmonitor.providers.customer


import android.content.Context
import com.ndipatri.solarmonitor.persistence.AppDatabase
import com.ndipatri.solarmonitor.providers.CustomIdlingResource
import com.ndipatri.solarmonitor.providers.panelScan.Panel
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

open class CustomerProvider(var context: Context) {

    val idlingResource = CustomIdlingResource("customerProviderResource")

    val customerDao = AppDatabase.getInstance(context).customerDao()

    fun findCustomerForPanel(panelId: String): Single<Customer> {

        return Single.create { subscribe: SingleEmitter<Customer> ->

            idlingResource.updateIdleState(CustomIdlingResource.IS_NOT_IDLE)

            var customerName = "Customer $panelId"

            // NJD TODO - In the future, this would be a cloud lookup based on panelId,
            // but for now, we hardcode.. associate our customer with any panel
            // we find.
            Customer(customerName,
                    .13671).apply {

                customerDao.insertOrReplaceCustomer(this)
            }

            subscribe.onSuccess(customerDao.getCustomerByName(customerName))

            idlingResource.updateIdleState(CustomIdlingResource.IS_IDLE)
        }
        .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
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
