package com.ndipatri.solarmonitor.activities


import android.content.DialogInterface
import android.view.Menu
import android.view.MenuItem

import com.ndipatri.iot.googleproximity.activities.RequirementsActivity
import com.ndipatri.solarmonitor.R
import com.ndipatri.solarmonitor.fragments.ConfigurePanelDialogFragment
import com.ndipatri.solarmonitor.providers.panelScan.PanelScanProvider

import javax.inject.Inject

open class BaseActivity : RequirementsActivity() {

    @Inject
    var panelScanProvider: PanelScanProvider? = null

    //region menuSetup
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId


        if (id == R.id.action_settings) {
            return true
        } else if (id == R.id.action_configure) {
            launchConfigureFragment()

            return true
        }

        return super.onOptionsItemSelected(item)
    }
    //endregion

    protected fun launchConfigureFragment() {
        val dialog = ConfigurePanelDialogFragment()
        dialog.show(supportFragmentManager.beginTransaction(), "configure panel dialog")
        supportFragmentManager.executePendingTransactions()
        dialog.dialog.setOnDismissListener { handleConfigureFragmentDismiss() }
    }

    protected open fun handleConfigureFragmentDismiss() {}

    companion object {

        private val TAG = BaseActivity::class.java!!.getSimpleName()
    }
}
