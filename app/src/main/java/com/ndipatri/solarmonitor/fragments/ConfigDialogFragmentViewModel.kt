package com.ndipatri.solarmonitor.fragments

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ndipatri.solarmonitor.R
import com.ndipatri.solarmonitor.SolarMonitorApp
import com.ndipatri.solarmonitor.providers.panelScan.Panel
import com.ndipatri.solarmonitor.providers.panelScan.PanelProvider
import io.reactivex.CompletableObserver
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.launch
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * Notice this class imports nothing from 'android.widget': it implies no view technology
 */

open class ConfigDialogFragmentViewModel(context: Application) : AndroidViewModel(context) {

    init {
        SolarMonitorApp.instance.objectGraph.inject(this)
    }

    @Inject
    lateinit var panelProvider: PanelProvider

    var userState = MutableLiveData<USER_STATE>().also { it.setValue(USER_STATE.SEARCHING_FOR_PANEL) }

    var scannedPanel = MutableLiveData<Panel>()

    var userMessage = MutableLiveData<String>()

    var isPanelDescriptionValid: Boolean = false
    var isPanelIdValid: Boolean = false
    var panelConfigDataValid = MutableLiveData<Boolean>()

    private var disposable: Disposable? = null

    enum class USER_STATE {
        SEARCHING_FOR_PANEL,
        NO_PANEL_FOUND,
        PANEL_FOUND,
        CONFIGURING_PANEL,
        CONFIGURATION_ERROR,
        DONE
    } // user is viewing loaded solar output


    fun newPanelDescriptionEntered(candidateDescription: String, sendUserMessage: Boolean = true) {
        isPanelDescriptionValid(candidateDescription).let {
            isPanelDescriptionValid = it

            if (sendUserMessage) {
                userMessage.value = getApplication<Application>().getString(if (it) R.string.description_is_valid else R.string.description_requirements)
            }

            panelConfigDataValid.value = isPanelDescriptionValid && isPanelIdValid
        }
    }

    fun newPanelIdEntered(candidateId: String, sendUserMessage: Boolean = true) {
        isPanelIdValid(candidateId).let {
            isPanelIdValid = it

            if (sendUserMessage) {
                userMessage.value = getApplication<Application>().getString(if (it) R.string.panel_id_is_valid else R.string.panel_id_requirements)
            }

            panelConfigDataValid.value = isPanelDescriptionValid && isPanelIdValid
        }
    }

    fun configurationError() {
        userState.value = USER_STATE.PANEL_FOUND
    }

    fun searchForPanel() {

        viewModelScope.launch {

            userState.value = USER_STATE.SEARCHING_FOR_PANEL

            try {
                panelProvider.scanForNearbyPanel()?.apply {
                    Log.d(TAG, "Panel found.  Trying to configure ...")

                    this@ConfigDialogFragmentViewModel.scannedPanel.value = this

                    newPanelDescriptionEntered(description, sendUserMessage = false)
                    newPanelIdEntered(id, sendUserMessage = false)

                    userState.value = USER_STATE.PANEL_FOUND
                } ?: let {
                    userState.value = USER_STATE.NO_PANEL_FOUND
                }
            } catch(e: Exception) {
                Log.e(TAG, "Error while searching for panel.", e)

                userState.value = USER_STATE.NO_PANEL_FOUND
            }
        }
    }

    fun configurePanel(description: String, id: String) {

        val updatedPanel = Panel(id, description)

        userState.value = USER_STATE.CONFIGURING_PANEL

        panelProvider.updateNearbyPanel(configPanel = updatedPanel).subscribe(object: CompletableObserver {
            override fun onComplete() {
                userState.value = USER_STATE.DONE
            }

            override fun onSubscribe(disposable: Disposable) {
                this@ConfigDialogFragmentViewModel.disposable = disposable
            }

            override fun onError(e: Throwable) {
                userState.value = USER_STATE.CONFIGURATION_ERROR
            }
        })
    }

    fun eraseNearbyPanel() {

        userState.value = USER_STATE.CONFIGURING_PANEL

        panelProvider
                .eraseNearbyPanel()
                .subscribe(object : CompletableObserver {
                    override fun onSubscribe(disposable: Disposable) {
                        this@ConfigDialogFragmentViewModel.disposable = disposable
                    }

                    override fun onComplete() {
                        userState.value = USER_STATE.DONE
                    }

                    override fun onError(e: Throwable) {
                        userState.value = USER_STATE.CONFIGURATION_ERROR
                    }
                })
    }


    private val PANEL_DESCRIPTION_PATTERN = Pattern.compile("[\\S ]{3,20}")
    private fun isPanelDescriptionValid(panelDescriptionCandidate: String): Boolean {
        return PANEL_DESCRIPTION_PATTERN.matcher(panelDescriptionCandidate).matches()
    }

    private val CUSTOMER_ID_PATTERN = Pattern.compile("\\d{6}")
    private fun isPanelIdValid(panelIdCandidate: String): Boolean {
        return CUSTOMER_ID_PATTERN.matcher(panelIdCandidate).matches()
    }

    override fun onCleared() {
        super.onCleared()

        disposable?.dispose()
    }

    companion object {
        private val TAG = ConfigDialogFragmentViewModel::class.java.simpleName
    }
}