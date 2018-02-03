package com.ndipatri.solarmonitor.activities

import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

import com.f2prateek.rx.preferences2.Preference
import com.ndipatri.solarmonitor.R
import com.ndipatri.solarmonitor.SolarMonitorApp
import com.ndipatri.solarmonitor.providers.panelScan.PanelInfo
import com.ndipatri.solarmonitor.providers.panelScan.PanelScanProvider
import com.ndipatri.solarmonitor.providers.solarUpdate.SolarOutputProvider
import com.ndipatri.solarmonitor.providers.solarUpdate.dto.PowerOutput

import javax.inject.Inject

import butterknife.BindView
import butterknife.ButterKnife
import io.reactivex.MaybeObserver
import io.reactivex.SingleObserver
import io.reactivex.disposables.Disposable

import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import com.ndipatri.solarmonitor.activities.MainActivity.USER_STATE.CONFIGURE
import com.ndipatri.solarmonitor.activities.MainActivity.USER_STATE.LOAD
import com.ndipatri.solarmonitor.activities.MainActivity.USER_STATE.LOADED
import com.ndipatri.solarmonitor.activities.MainActivity.USER_STATE.LOADING
import com.ndipatri.solarmonitor.activities.MainActivity.USER_STATE.SCAN
import com.ndipatri.solarmonitor.activities.MainActivity.USER_STATE.SCANNING

class MainActivity : BaseActivity() {

    //region dependency injection
    @Inject
    internal var solarOutputProvider: SolarOutputProvider

    @Inject
    internal var panelScanProvider: PanelScanProvider
    //endregion

    //region view injection
    @BindView(R.id.refreshProgressBar)
    internal var refreshProgressBar: ProgressBar? = null

    @BindView(R.id.mainTextView)
    internal var mainTextView: TextView? = null

    @BindView(R.id.detailTextView)
    internal var detailTextView: TextView? = null

    @BindView(R.id.loadFAB)
    internal var loadFAB: FloatingActionButton? = null

    @BindView(R.id.scanFAB)
    internal var scanFAB: FloatingActionButton? = null

    @BindView(R.id.configureFAB)
    internal var configureFAB: FloatingActionButton? = null
    //endregion

    //region local state
    private var disposable: Disposable? = null

    private var currentPowerOutput: PowerOutput? = null
    private var userState: USER_STATE? = null
    //endregion

    private val solarCustomerId: Preference<String>
        get() = SolarMonitorApp.instance!!.solarCustomerId

    internal enum class USER_STATE {
        SCAN, // user has yet to find nearby panel
        SCANNING, // user is scanning for nearby panel
        CONFIGURE, // panel found but it needs to be configured.
        LOAD, // user has nearby panel, but hasn't loaded output
        LOADING, // user is loading solar output
        LOADED
    }// user is viewing loaded solar output
    // endregion

    init {
        SolarMonitorApp.instance!!.objectGraph.inject(this)
    }

    //region getSet
    fun setSolarOutputProvider(solarOutputProvider: SolarOutputProvider) {
        this.solarOutputProvider = solarOutputProvider
    }

    fun setPanelScanProvider(panelScanProvider: PanelScanProvider) {
        this.panelScanProvider = panelScanProvider
    }
    //endregion

    //region lifecyle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        ButterKnife.bind(this)

        setSupportActionBar(findViewById(R.id.toolbar) as Toolbar)
    }

    override fun onResume() {
        super.onResume()

        initialize()
    }

    private fun initialize() {

        if (solarCustomerId.isSet) {
            userState = LOAD
        } else {
            userState = SCAN
        }

        updateStatusViews()
    }

    override fun onPause() {
        super.onPause()

        if (null != disposable) {
            disposable!!.dispose()
        }
    }

    override fun handleConfigureFragmentDismiss() {
        initialize()
    }

    //endregion

    //region view processing
    private fun updateStatusViews() {

        when (userState) {
            SCAN -> {
                refreshProgressBar!!.visibility = INVISIBLE

                mainTextView!!.visibility = VISIBLE
                mainTextView!!.text = getString(R.string.click_to_find_nearby_solar_panel)

                detailTextView!!.visibility = INVISIBLE

                showScanButton()
                hideLoadButton()
                hideConfigureButton()
            }

            SCANNING -> {
                refreshProgressBar!!.visibility = VISIBLE

                mainTextView!!.visibility = VISIBLE
                mainTextView!!.text = getString(R.string.finding_nearby_solar_panel)

                detailTextView!!.visibility = INVISIBLE

                hideScanButton()
                hideLoadButton()
                hideConfigureButton()
            }

            CONFIGURE -> {
                refreshProgressBar!!.visibility = INVISIBLE

                mainTextView!!.visibility = VISIBLE
                mainTextView!!.text = getString(R.string.click_to_configure_nearby_panel)

                detailTextView!!.visibility = INVISIBLE

                hideScanButton()
                hideLoadButton()
                showConfigureButton()
            }

            LOAD -> {
                refreshProgressBar!!.visibility = INVISIBLE

                mainTextView!!.visibility = VISIBLE
                mainTextView!!.text = getString(R.string.click_to_load_solar_output)

                detailTextView!!.visibility = VISIBLE
                detailTextView!!.text = "solar panel (" + solarCustomerId.get() + ")"

                showScanButton()
                showLoadButton()
                hideConfigureButton()
            }

            LOADING -> {
                refreshProgressBar!!.visibility = VISIBLE

                mainTextView!!.visibility = VISIBLE
                mainTextView!!.text = getString(R.string.loading_solar_output)

                detailTextView!!.visibility = VISIBLE
                detailTextView!!.text = "solar panel (" + solarCustomerId.get() + ")"

                hideScanButton()
                hideLoadButton()
                hideConfigureButton()
            }

            LOADED -> {
                refreshProgressBar!!.visibility = INVISIBLE

                mainTextView!!.visibility = VISIBLE
                val sbuf = StringBuilder()
                        .append("current: ")
                        .append(currentPowerOutput!!.currentPowerInWatts)
                        .append(" watts, ")

                        .append("lifetime: ")
                        .append(currentPowerOutput!!.lifeTimeEnergyInWattHours)
                        .append(" wattHours.")
                mainTextView!!.text = sbuf.toString()

                detailTextView!!.visibility = VISIBLE
                detailTextView!!.text = "solar panel (" + solarCustomerId.get() + ")"

                showScanButton()
                showLoadButton()
                hideConfigureButton()
            }
        }
    }

    private fun showLoadButton() {
        loadFAB!!.visibility = View.VISIBLE
        loadFAB!!.setOnClickListener { viewClicked ->

            userState = LOADING
            loadSolarOutput()
            updateStatusViews()
        }
    }

    private fun hideLoadButton() {
        loadFAB!!.visibility = View.INVISIBLE
    }

    private fun showScanButton() {
        scanFAB!!.visibility = View.VISIBLE
        scanFAB!!.setOnClickListener { viewClicked ->

            userState = SCANNING
            scanForNearbyPanel()
            updateStatusViews()
        }
    }

    private fun hideScanButton() {
        scanFAB!!.visibility = View.INVISIBLE
    }

    private fun showConfigureButton() {
        configureFAB!!.visibility = View.VISIBLE
        configureFAB!!.setOnClickListener { viewClicked ->

            userState = SCAN
            launchConfigureFragment()
        }
    }

    private fun hideConfigureButton() {
        configureFAB!!.visibility = View.INVISIBLE
    }
    //endregion

    //region background calls
    private fun scanForNearbyPanel() {

        panelScanProvider.scanForNearbyPanel().subscribe(object : MaybeObserver<PanelInfo> {
            override fun onSubscribe(disposable: Disposable) {
                this@MainActivity.disposable = disposable
            }

            override fun onSuccess(panelInfo: PanelInfo) {
                if (panelInfo.customerId!!.isPresent && panelInfo.customerId!!.get().length == 6) {
                    Log.d(TAG, "Panel found with customerId configured (" + panelInfo.customerId!!.get() + ").")

                    SolarMonitorApp.instance!!.setSolarCustomerId(panelInfo.customerId!!.get())

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

                if (solarCustomerId.isSet) {
                    userState = LOAD
                } else {
                    userState = SCAN
                }
                updateStatusViews()
            }

            override fun onComplete() {
                Toast.makeText(this@MainActivity, getString(R.string.no_nearby_panels_were_found), Toast.LENGTH_SHORT).show()

                if (solarCustomerId.isSet) {
                    userState = LOAD
                } else {
                    userState = SCAN
                }
                updateStatusViews()
            }
        })
    }

    private fun loadSolarOutput() {
        if (solarCustomerId.isSet) {

            solarOutputProvider.getSolarOutput(solarCustomerId.get()).subscribe(object : SingleObserver<PowerOutput> {
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

        private val TAG = MainActivity::class.java!!.getSimpleName()
    }
}
