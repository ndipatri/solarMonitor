package com.ndipatri.solarmonitor.activities

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.widget.Toolbar
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
    }

    private fun subscribeToUserState() {
        viewModel.userState.observe(this, Observer<MainActivityViewModel.USER_STATE> {

            when (it) {
                IDLE -> {
                    mainTextView.show().text = getString(R.string.click_to_find_nearby_solar_panel)
                    detailTextView.hide()

                    refreshProgressBar.gone()

                    scanFAB._show()
                    loadFAB._hide()
                    configureFAB._hide()
                }

                SCANNING -> {
                    mainTextView.show().text = getString(R.string.finding_nearby_solar_panel)
                    detailTextView.hide()

                    refreshProgressBar.show()

                    scanFAB._hide()
                    loadFAB._hide()
                    configureFAB._hide()
                }

                CONFIGURE -> {
                    mainTextView.show().text = getString(R.string.click_to_configure_nearby_panel)
                    detailTextView.gone()

                    refreshProgressBar.gone()

                    scanFAB._hide()
                    loadFAB._hide()
                    configureFAB._show()
                }

                LOAD -> {
                    mainTextView.show().text = getString(R.string.click_to_load_solar_output)

                    detailTextView.text = "solar panel (${viewModel.scannedPanel.value!!.id})"
                    detailTextView.show()

                    refreshProgressBar.gone()

                    scanFAB._show()
                    loadFAB._show()
                    configureFAB._hide()
                }

                LOADING -> {
                    mainTextView.show().text = getString(R.string.loading_solar_output)
                    detailTextView.show()

                    detailTextView.text = "solar panel ${viewModel.scannedPanel.value!!.id}"
                    refreshProgressBar.show()

                    scanFAB._hide()
                    loadFAB._hide()
                    configureFAB._hide()
                }

                LOADED -> {

                    mainTextView.show().text = viewModel.powerOutputMessage.value

                    detailTextView.text = "solar panel ${viewModel.scannedPanel.value!!.id}"

                    refreshProgressBar.gone()

                    scanFAB._show()
                    loadFAB._show()
                    configureFAB._hide()
                }
            }
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
