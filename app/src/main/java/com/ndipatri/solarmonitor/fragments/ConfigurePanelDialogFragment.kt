package com.ndipatri.solarmonitor.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.ndipatri.iot.googleproximity.GoogleProximity
import com.ndipatri.solarmonitor.*
import com.ndipatri.solarmonitor.fragments.ConfigurePanelDialogFragment.USER_STATE.*
import com.ndipatri.solarmonitor.providers.panelScan.PanelInfo
import com.ndipatri.solarmonitor.providers.panelScan.PanelScanProvider
import io.reactivex.CompletableObserver
import io.reactivex.MaybeObserver
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_configure_panel.*
import kotlinx.android.synthetic.main.fragment_configure_panel.view.*
import java.util.regex.Pattern
import javax.inject.Inject

class ConfigurePanelDialogFragment : DialogFragment() {

    init {
        SolarMonitorApp.instance.objectGraph.inject(this)
    }

    @Inject
    lateinit var panelScanProvider: PanelScanProvider

    private var panelDisposable: Disposable? = null

    private lateinit var foundPanelInfo: PanelInfo

    private val isEnteredPanelDescriptionValid: Boolean
        get() = PANEL_DESCRIPTION_PATTERN.matcher(panelDescriptionEditText.text).matches()

    private val isEnteredCustomerIdValid: Boolean
        get() = CUSTOMER_ID_PATTERN.matcher(customerIdEditText.text).matches()

    private var userState: USER_STATE? = null
    internal enum class USER_STATE {
        SEARCHING_FOR_PANEL,
        NO_PANEL_FOUND,
        PANEL_FOUND,
        CONFIGURING_PANEL,
        CONFIGURATION_ERROR
    }

    private lateinit var dialogView: View

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = AlertDialog.Builder(activity)

        val dialogTitle = resources.getString(R.string.configure_panel)
        val titleView = TextView(activity)
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        titleView.text = dialogTitle
        titleView.gravity = Gravity.CENTER

        dialogView = LayoutInflater.from(activity).inflate(R.layout.fragment_configure_panel, null)

        builder.setTitle(dialogTitle)
                //.setCustomTitle(titleView)
                .setView(dialogView)
                .setNeutralButton("Done") { _, _ -> dismiss() }

        val dialog = builder.create()

        dialog.window.attributes.windowAnimations = R.style.slideup_dialog_animation
        dialog.setCanceledOnTouchOutside(false)

        return dialog
    }

    override fun onResume() {
        super.onResume()

        // NJD TODO - need to add a manual search button maybe?
        searchForPanel()

        updateStatusViews()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        GoogleProximity.getInstance().redirectToAuthenticationActivityIfNecessary(activity)
    }

    override fun onDetach() {
        super.onDetach()

        panelDisposable?.dispose()
    }

    private fun updateStatusViews() {

        when (userState) {
            SEARCHING_FOR_PANEL -> {
                progressTextView.show().text = getString(R.string.search_for_nearby_panel)

                dialogView.panelDescriptionEditText.gone()
                dialogView.customerIdEditText.gone()

                dialogView.progressBar.show()

                dialogView.firstUserActionButton.gone()

                dialogView.secondUserActionButton.gone()
            }

            NO_PANEL_FOUND -> {
                dialogView.progressTextView.show().text = getString(R.string.no_nearby_panels_were_found)

                dialogView.panelDescriptionEditText.gone()
                dialogView.customerIdEditText.gone()

                dialogView.progressBar.hide()

                dialogView.firstUserActionButton.show().setText(R.string.search_again)
                dialogView.firstUserActionButton.setOnClickListener( { searchForPanel()} )

                dialogView.secondUserActionButton.gone()
            }

            PANEL_FOUND -> {
                dialogView.panelDescriptionEditText.show().setText(foundPanelInfo.description)
                dialogView.panelDescriptionEditText.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

                    override fun afterTextChanged(newPanelDescription: Editable) {
                        if (!isEnteredPanelDescriptionValid) {
                            context.toast(getString(R.string.description_requirements))
                        }

                        dialogView.firstUserActionButton.isEnabled = isEnteredCustomerIdValid && isEnteredPanelDescriptionValid
                    }
                })

                dialogView.customerIdEditText.apply {
                    show()
                    hint = activity.getString(R.string.enter_customer_id)
                    takeIf { foundPanelInfo.customerId != null } ?.setText(foundPanelInfo.customerId)
                }

                customerIdEditText.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

                    override fun afterTextChanged(newCustomerId: Editable) {
                        if (!isEnteredCustomerIdValid) {
                            Toast.makeText(activity, getString(R.string.customer_id_requirements), Toast.LENGTH_SHORT).show()
                        }

                        firstUserActionButton.isEnabled = isEnteredCustomerIdValid && isEnteredPanelDescriptionValid
                    }
                })

                progressTextView.hide()
                progressBar.hide()

                firstUserActionButton.text = activity.getString(R.string.configure_panel)
                firstUserActionButton.show()
                firstUserActionButton.isEnabled = isEnteredCustomerIdValid && isEnteredPanelDescriptionValid
                firstUserActionButton.setOnClickListener {
                    val updatedPanelInfo = PanelInfo(panelDescriptionEditText.text.toString(),
                            customerIdEditText.text.toString())
                    configurePanel(updatedPanelInfo)
                }

                secondUserActionButton.text = activity.getString(R.string.erase_panel)
                secondUserActionButton.show()
                secondUserActionButton.setOnClickListener {
                    val newPanelInfo = PanelInfo() // default settings
                    configurePanel(newPanelInfo)
                }
            }

            CONFIGURING_PANEL -> {
                panelDescriptionEditText.gone()
                customerIdEditText.gone()

                progressBar.show()
                progressTextView.show()
                progressTextView.setText(R.string.configuring_nearby_panel)

                firstUserActionButton.gone()
                secondUserActionButton.gone()
            }

            CONFIGURATION_ERROR -> {
                panelDescriptionEditText.hide()
                customerIdEditText.hide()

                progressTextView.show()
                progressTextView.setText(R.string.failed_to_configure_panel)
                progressBar.hide()

                firstUserActionButton.setText(R.string.OK)
                firstUserActionButton.show()
                firstUserActionButton.setOnClickListener {
                    userState = PANEL_FOUND
                    updateStatusViews()

                }
                secondUserActionButton.hide()
            }
        }
    }

    private fun configurePanel(updatedPanelInfo: PanelInfo) {

        userState = CONFIGURING_PANEL
        updateStatusViews()

        panelScanProvider.updateNearbyPanel(updatedPanelInfo).subscribe(object : CompletableObserver {
            override fun onSubscribe(d: Disposable) {
                this@ConfigurePanelDialogFragment.panelDisposable = panelDisposable
            }

            override fun onComplete() {

                SolarMonitorApp.instance.apply {
                    if (null != updatedPanelInfo.customerId) {
                        setSolarCustomerId(updatedPanelInfo.customerId)
                    } else {
                        solarCustomerId.delete()
                    }
                }

                dismiss()
            }

            override fun onError(e: Throwable) {
                userState = CONFIGURATION_ERROR
                updateStatusViews()
            }
        })
    }

    private fun searchForPanel() {

        userState = SEARCHING_FOR_PANEL
        updateStatusViews()

        panelScanProvider!!.scanForNearbyPanel().subscribe(object : MaybeObserver<PanelInfo> {
            override fun onSubscribe(panelDisposable: Disposable) {
                this@ConfigurePanelDialogFragment.panelDisposable = panelDisposable
            }

            override fun onSuccess(foundPanelInfo: PanelInfo) {
                Log.d(TAG, "Panel found.  Trying to configure ...")

                this@ConfigurePanelDialogFragment.foundPanelInfo = foundPanelInfo

                userState = PANEL_FOUND
                updateStatusViews()
            }

            override fun onError(e: Throwable) {
                Log.e(TAG, "Error while searching for panel.", e)

                userState = NO_PANEL_FOUND
                updateStatusViews()
            }

            override fun onComplete() {
                userState = NO_PANEL_FOUND
                updateStatusViews()
            }
        })
    }

    companion object {

        private val TAG = ConfigurePanelDialogFragment::class.java!!.getSimpleName()

        private val PANEL_DESCRIPTION_PATTERN = Pattern.compile("[\\S ]{3,20}")
        private val CUSTOMER_ID_PATTERN = Pattern.compile("\\d{6}")
    }
}



