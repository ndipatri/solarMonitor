package com.ndipatri.solarmonitor.activities

import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.util.Log
import android.widget.Toast
import com.ndipatri.solarmonitor.R
import com.ndipatri.solarmonitor.SolarMonitorApp
import com.ndipatri.solarmonitor.activities.MainActivity.USER_STATE.*
import com.ndipatri.solarmonitor.gone
import com.ndipatri.solarmonitor.providers.panelScan.PanelInfo
import com.ndipatri.solarmonitor.providers.panelScan.PanelScanProvider
import com.ndipatri.solarmonitor.providers.solarUpdate.SolarOutputProvider
import com.ndipatri.solarmonitor.providers.solarUpdate.dto.PowerOutput
import com.ndipatri.solarmonitor.show
import io.reactivex.MaybeObserver
import io.reactivex.SingleObserver
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import javax.inject.Inject

class MainActivity : BaseActivity() {

    init {
        SolarMonitorApp.instance.objectGraph.inject(this)
    }

    @Inject
    lateinit var solarOutputProvider: SolarOutputProvider

    @Inject
    lateinit var panelScanProvider: PanelScanProvider

    private var disposable: Disposable? = null

    private var currentPowerOutput: PowerOutput? = null

    private lateinit var userState: USER_STATE

    internal enum class USER_STATE {
        SCAN, // user has yet to find nearby panel
        SCANNING, // user is scanning for nearby panel
        CONFIGURE, // panel found but it needs to be configured.
        LOAD, // user has nearby panel, but hasn't loaded output
        LOADING, // user is loading solar output
        LOADED
    }// user is viewing loaded solar output


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar as Toolbar)
    }

    override fun onResume() {
        super.onResume()

        configureClickListeners()
        resetToSteadyState()
    }

    override fun onPause() {
        super.onPause()

        disposable?.dispose()
    }

    private fun resetToSteadyState() {

        if (SolarMonitorApp.instance.solarCustomerId.isSet) {
            userState = LOAD
        } else {
            userState = SCAN
        }

        updateStatusViews()
    }

    override fun handleConfigureFragmentDismiss() {
        resetToSteadyState()
    }

    private fun updateStatusViews() {

        when (userState) {
            SCAN -> {
                mainTextView.show().text = getString(R.string.click_to_find_nearby_solar_panel)
                detailTextView.gone()

                refreshProgressBar.gone()

                scanFAB.show()
                loadFAB.hide()
                configureFAB.hide()
            }

            SCANNING -> {
                mainTextView.show().text = getString(R.string.finding_nearby_solar_panel)
                detailTextView.gone()

                refreshProgressBar.show()

                scanFAB.hide()
                loadFAB.hide()
                configureFAB.hide()
            }

            CONFIGURE -> {
                mainTextView.show().text = getString(R.string.click_to_configure_nearby_panel)
                detailTextView.gone()

                refreshProgressBar.gone()

                scanFAB.hide()
                loadFAB.hide()
                configureFAB.show()
            }

            LOAD -> {
                mainTextView.show().text = getString(R.string.click_to_load_solar_output)
                detailTextView.show().text = "solar panel (" + SolarMonitorApp.instance.solarCustomerId.get() + ")"

                refreshProgressBar.gone()

                scanFAB.show()
                loadFAB.show()
                configureFAB.hide()
            }

            LOADING -> {
                mainTextView.show().text = getString(R.string.loading_solar_output)
                detailTextView.show().text = "solar panel (" + SolarMonitorApp.instance.solarCustomerId.get() + ")"

                refreshProgressBar.show()

                scanFAB.hide()
                loadFAB.hide()
                configureFAB.hide()
            }

            LOADED -> {

                mainTextView.show().text =

                "current: ${currentPowerOutput!!.currentPowerInWatts} watts," +
                "lifetime: ${currentPowerOutput!!.lifeTimeEnergyInWattHours} wattsHours."

                detailTextView.show().text =

                "solar panel (${SolarMonitorApp.instance.solarCustomerId.get()})"

                refreshProgressBar.gone()

                scanFAB.show()
                loadFAB.show()
                configureFAB.hide()
            }
        }
    }

    private fun configureClickListeners() {
        loadFAB.setOnClickListener {
            userState = LOADING
            loadSolarOutput()
            updateStatusViews()
        }

        scanFAB.setOnClickListener {
            userState = SCANNING
            scanForNearbyPanel()
            updateStatusViews()
        }

        configureFAB.setOnClickListener {
            userState = SCAN
            launchConfigureFragment()
        }
    }

    private fun scanForNearbyPanel() {

        panelScanProvider.scanForNearbyPanel().subscribe(object : MaybeObserver<PanelInfo> {
            override fun onSubscribe(disposable: Disposable) {
                this@MainActivity.disposable = disposable
            }

            override fun onSuccess(panelInfo: PanelInfo) {
                if (panelInfo.customerId?.length == 6) {
                    Log.d(TAG, "Panel found with customerId configured ${panelInfo.customerId}.")

                    SolarMonitorApp.instance.setSolarCustomerId(panelInfo.customerId)

                    this@MainActivity.disposable = null
                    this@MainActivity.currentPowerOutput = null

                    userState = LOAD
                    updateStatusViews()
                } else {
                    Log.d(TAG, "Panel found, but it needs to be configured.")

                    this@MainActivity.disposable = null
                    this@MainActivity.currentPowerOutput = null

                    userState = CONFIGURE
                    updateStatusViews()
                }
            }

            override fun onError(e: Throwable) {
                Toast.makeText(this@MainActivity, getString(R.string.error_please_try_again), Toast.LENGTH_SHORT).show()

                if (SolarMonitorApp.instance.solarCustomerId.isSet) {
                    userState = LOAD
                } else {
                    userState = SCAN
                }
                updateStatusViews()
            }

            override fun onComplete() {
                Toast.makeText(this@MainActivity, getString(R.string.no_nearby_panels_were_found), Toast.LENGTH_SHORT).show()

                if (SolarMonitorApp.instance.solarCustomerId.isSet) {
                    userState = LOAD
                } else {
                    userState = SCAN
                }
                updateStatusViews()
            }
        })
    }

    private fun loadSolarOutput() {
        if (SolarMonitorApp.instance.solarCustomerId.isSet) {

            solarOutputProvider.getSolarOutput(SolarMonitorApp.instance.solarCustomerId.get()!!).subscribe(object : SingleObserver<PowerOutput> {
                override fun onSubscribe(disposable: Disposable) {
                    this@MainActivity.disposable = disposable
                }

                override fun onSuccess(currentPowerOutput: PowerOutput) {
                    this@MainActivity.disposable = null
                    this@MainActivity.currentPowerOutput = currentPowerOutput

                    userState = LOADED
                    updateStatusViews()
                }

                override fun onError(e: Throwable) {
                    Toast.makeText(this@MainActivity, getString(R.string.error_please_try_again), Toast.LENGTH_SHORT).show()

                    userState = LOAD
                    updateStatusViews()
                }
            })
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}
