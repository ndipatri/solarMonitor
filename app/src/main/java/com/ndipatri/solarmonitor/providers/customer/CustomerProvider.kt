package com.ndipatri.solarmonitor.providers.customer


import android.content.Context
import com.ndipatri.solarmonitor.persistence.AppDatabase

open class CustomerProvider(var context: Context) {

    val customerDao = AppDatabase.getInstance(context).customerDao()

    // When we convert this over Coroutines, we make this a suspend function instead
// including the actual launcher.  This is because this is NOT a good place to launch
// as we are unaware of the lifecycle of the component using this Customer provider.
//
// Backgrounding is therefore deferred and we do NOT need RxJava here.
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

    open suspend fun deleteAllCustomers() {
        customerDao.deleteAllCustomers()
    }

    companion object {
        private val TAG = CustomerProvider::class.java!!.getSimpleName()
    }
}
