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
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

import com.ndipatri.iot.googleproximity.GoogleProximity
import com.ndipatri.solarmonitor.R
import com.ndipatri.solarmonitor.SolarMonitorApp
import com.ndipatri.solarmonitor.providers.panelScan.PanelInfo
import com.ndipatri.solarmonitor.providers.panelScan.PanelScanProvider

import java.util.regex.Pattern

import javax.inject.Inject

import butterknife.BindView
import butterknife.ButterKnife
import io.reactivex.CompletableObserver
import io.reactivex.MaybeObserver
import io.reactivex.disposables.Disposable

import com.ndipatri.solarmonitor.fragments.ConfigurePanelDialogFragment.USER_STATE.CONFIGURATION_ERROR
import com.ndipatri.solarmonitor.fragments.ConfigurePanelDialogFragment.USER_STATE.CONFIGURING_PANEL
import com.ndipatri.solarmonitor.fragments.ConfigurePanelDialogFragment.USER_STATE.NO_PANEL_FOUND
import com.ndipatri.solarmonitor.fragments.ConfigurePanelDialogFragment.USER_STATE.PANEL_FOUND
import com.ndipatri.solarmonitor.fragments.ConfigurePanelDialogFragment.USER_STATE.SEARCHING_FOR_PANEL

class ConfigurePanelDialogFragment : DialogFragment() {
    private var userState: USER_STATE? = null

    @BindView(R.id.panelDescriptionEditText)
    internal var panelDescriptionEditText: EditText? = null

    @BindView(R.id.customerIdEditText)
    internal var customerIdEditText: EditText? = null

    @BindView(R.id.firstUserActionButton)
    internal var firstUserActionButton: Button? = null

    @BindView(R.id.secondUserActionButton)
    internal var secondUserActionButton: Button? = null

    @BindView(R.id.progressBar)
    internal var progressBar: View? = null

    @BindView(R.id.progressTextView)
    internal var progressTextView: TextView? = null

    @Inject
    internal var panelScanProvider: PanelScanProvider? = null

    private var panelDisposable: Disposable? = null

    private var foundPanelInfo: PanelInfo? = null

    private val isEnteredPanelDescriptionValid: Boolean
        get() = PANEL_DESCRIPTION_PATTERN.matcher(panelDescriptionEditText!!.text).matches()

    private val isEnteredCustomerIdValid: Boolean
        get() = CUSTOMER_ID_PATTERN.matcher(customerIdEditText!!.text).matches()

    internal enum class USER_STATE {
        SEARCHING_FOR_PANEL,
        NO_PANEL_FOUND,
        PANEL_FOUND,
        CONFIGURING_PANEL,
        CONFIGURATION_ERROR
    }

    init {
        SolarMonitorApp.instance!!.objectGraph.inject(this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = AlertDialog.Builder(activity)

        val dialogTitle = resources.getString(R.string.configure_panel)
        val titleView = TextView(activity)
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        titleView.text = dialogTitle
        titleView.gravity = Gravity.CENTER

        val dialogView = LayoutInflater.from(activity).inflate(R.layout.fragment_configure_panel, null)

        // Use ButterKnife for view injection (http://jakewharton.github.io/butterknife/)
        ButterKnife.bind(this, dialogView)

        builder.setTitle(dialogTitle)
                //.setCustomTitle(titleView)
                .setView(dialogView)
                .setNeutralButton("Done") { dialog, which -> dismiss() }

        val dialog = builder.create()

        dialog.window!!.attributes.windowAnimations = R.style.slideup_dialog_animation
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

        if (null != panelDisposable) {
            panelDisposable!!.dispose()
        }
    }

    private fun updateStatusViews() {

        when (userState) {
            SEARCHING_FOR_PANEL -> {
                panelDescriptionEditText!!.visibility = View.GONE
                customerIdEditText!!.visibility = View.GONE

                progressBar!!.visibility = View.VISIBLE
                progressTextView!!.visibility = View.VISIBLE
                progressTextView!!.setText(R.string.search_for_nearby_panel)

                firstUserActionButton!!.visibility = View.GONE
                secondUserActionButton!!.visibility = View.GONE
            }

            NO_PANEL_FOUND -> {
                panelDescriptionEditText!!.visibility = View.GONE
                customerIdEditText!!.visibility = View.GONE

                progressTextView!!.visibility = View.VISIBLE
                progressTextView!!.setText(R.string.no_nearby_panels_were_found)
                progressBar!!.visibility = View.INVISIBLE

                firstUserActionButton!!.setText(R.string.search_again)
                firstUserActionButton!!.visibility = View.VISIBLE
                firstUserActionButton!!.setOnClickListener { v -> searchForPanel() }
                secondUserActionButton!!.visibility = View.GONE
            }


            PANEL_FOUND -> {
                panelDescriptionEditText!!.visibility = View.VISIBLE
                panelDescriptionEditText!!.setText(foundPanelInfo!!.description)
                panelDescriptionEditText!!.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

                    override fun afterTextChanged(newPanelDescription: Editable) {
                        if (!isEnteredPanelDescriptionValid) {
                            Toast.makeText(activity, getString(R.string.description_requirements), Toast.LENGTH_SHORT).show()
                        }

                        firstUserActionButton!!.isEnabled = isEnteredCustomerIdValid && isEnteredPanelDescriptionValid
                    }
                })

                customerIdEditText!!.visibility = View.VISIBLE
                if (foundPanelInfo!!.customerId!!.isPresent) {
                    customerIdEditText!!.setText(foundPanelInfo!!.customerId!!.get())
                } else {
                    customerIdEditText!!.hint = activity.getString(R.string.enter_customer_id)
                }
                customerIdEditText!!.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

                    override fun afterTextChanged(newCustomerId: Editable) {
                        if (!isEnteredCustomerIdValid) {
                            Toast.makeText(activity, getString(R.string.customer_id_requirements), Toast.LENGTH_SHORT).show()
                        }

                        firstUserActionButton!!.isEnabled = isEnteredCustomerIdValid && isEnteredPanelDescriptionValid
                    }
                })

                progressTextView!!.visibility = View.INVISIBLE
                progressBar!!.visibility = View.INVISIBLE

                firstUserActionButton!!.text = activity.getString(R.string.configure_panel)
                firstUserActionButton!!.visibility = View.VISIBLE
                firstUserActionButton!!.isEnabled = isEnteredCustomerIdValid && isEnteredPanelDescriptionValid
                firstUserActionButton!!.setOnClickListener { v ->
                    val updatedPanelInfo = PanelInfo(panelDescriptionEditText!!.text.toString(),
                            customerIdEditText!!.text.toString())
                    configurePanel(updatedPanelInfo)
                }

                secondUserActionButton!!.text = activity.getString(R.string.erase_panel)
                secondUserActionButton!!.visibility = View.VISIBLE
                secondUserActionButton!!.setOnClickListener { v ->
                    val newPanelInfo = PanelInfo() // default settings
                    configurePanel(newPanelInfo)
                }
            }

            CONFIGURING_PANEL -> {
                panelDescriptionEditText!!.visibility = View.GONE
                customerIdEditText!!.visibility = View.GONE

                progressBar!!.visibility = View.VISIBLE
                progressTextView!!.visibility = View.VISIBLE
                progressTextView!!.setText(R.string.configuring_nearby_panel)

                firstUserActionButton!!.visibility = View.GONE
                secondUserActionButton!!.visibility = View.GONE
            }

            CONFIGURATION_ERROR -> {
                panelDescriptionEditText!!.visibility = View.INVISIBLE
                customerIdEditText!!.visibility = View.INVISIBLE

                progressTextView!!.visibility = View.VISIBLE
                progressTextView!!.setText(R.string.failed_to_configure_panel)
                progressBar!!.visibility = View.INVISIBLE

                firstUserActionButton!!.setText(R.string.OK)
                firstUserActionButton!!.visibility = View.VISIBLE
                firstUserActionButton!!.setOnClickListener { v ->
                    userState = PANEL_FOUND
                    updateStatusViews()

                }
                secondUserActionButton!!.visibility = View.INVISIBLE
            }
        }
    }

    private fun configurePanel(updatedPanelInfo: PanelInfo) {

        userState = CONFIGURING_PANEL
        updateStatusViews()

        panelScanProvider!!.updateNearbyPanel(updatedPanelInfo).subscribe(object : CompletableObserver {
            override fun onSubscribe(d: Disposable) {
                this@ConfigurePanelDialogFragment.panelDisposable = panelDisposable
            }

            override fun onComplete() {
                if (updatedPanelInfo.customerId!!.isPresent) {
                    SolarMonitorApp.instance!!.setSolarCustomerId(updatedPanelInfo.customerId!!.get())
                } else {
                    SolarMonitorApp.instance!!.solarCustomerId.delete()
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
            override fun onSubscribe(d: Disposable) {
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



