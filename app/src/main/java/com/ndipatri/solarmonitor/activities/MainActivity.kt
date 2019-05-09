package com.ndipatri.solarmonitor.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ndipatri.solarmonitor.*
import com.ndipatri.solarmonitor.activities.MainActivityViewModel.USER_STATE.*
import com.ndipatri.solarmonitor.container.MainActivityViewModelFactory
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import javax.inject.Inject

class MainActivity : BaseActivity() {

    init {
        SolarMonitorApp.instance.objectGraph.inject(this)
    }

    @Inject
    lateinit var viewModelFactory: MainActivityViewModelFactory
    lateinit var viewModel: MainActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This manages all Activity state.. both storage of state and transitions from one state
        // to another
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(MainActivityViewModel::class.java)

        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar as Toolbar)
    }

    override fun onResume() {
        super.onResume()

        // This is how users initiate state change
        configureClickListeners()

        // This is how this activity responds to these state changes
        subscribeToUserState()

        // This is how the activity responds to user feedback from business logic
        subscribeToUserMessages()

        initializeUserState()
    }

    private fun initializeUserState() {
        viewModel.resetToSteadyState()
    }

    private fun subscribeToUserState() {
        viewModel.userState.observe(this, Observer<MainActivityViewModel.USER_STATE> {

            when (it) {
                IDLE -> {
                    mainTextView.show().text = getString(R.string.click_to_find_nearby_solar_panel)
                    detailTextView.hide()

                    refreshProgressBar.gone()

                    scanFAB.show()
                    loadFAB.hide()
                    configureFAB.hide()
                }

                SCANNING -> {
                    mainTextView.show().text = getString(R.string.finding_nearby_solar_panel)
                    detailTextView.hide()

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

                    detailTextView.text = "solar panel (${viewModel.scannedPanel!!.id})"
                    detailTextView.show()

                    refreshProgressBar.gone()

                    scanFAB.show()
                    loadFAB.show()
                    configureFAB.hide()
                }

                LOADING -> {
                    mainTextView.show().text = getString(R.string.loading_solar_output)
                    detailTextView.show()

                    detailTextView.text = "solar panel (${viewModel.scannedPanel!!.id})"
                    refreshProgressBar.show()

                    scanFAB.hide()
                    loadFAB.hide()
                    configureFAB.hide()
                }

                LOADED -> {

                    mainTextView.show().text = viewModel.powerOutputMessage.value

                    detailTextView.text = "solar panel (${viewModel.scannedPanel!!.id})"

                    refreshProgressBar.gone()

                    scanFAB.show()
                    loadFAB.show()
                    configureFAB.hide()
                }
            }
        })
    }

    private fun subscribeToUserMessages() {
        viewModel.userMessage.observe(this, Observer<String> {
            Toast.makeText(this@MainActivity.applicationContext, it, Toast.LENGTH_SHORT).show()
        })
    }

    private fun configureClickListeners() {
        scanFAB.setOnClickListener {
            viewModel.scanForNearbyPanel()
        }

        loadFAB.setOnClickListener {
            viewModel.loadSolarOutput()
        }

        configureFAB.setOnClickListener {
            launchConfigureFragment()
        }
    }

    override fun handleConfigureFragmentDismiss() {
        // NJD TODO - theory is this isn't needed anymore.. but not sure.
        // if it isn't, remove this whole stupid method in base class (dismiss listener)
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}
