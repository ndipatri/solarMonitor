package com.ndipatri.solarmonitor.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ndipatri.iot.googleproximity.GoogleProximity
import com.ndipatri.solarmonitor.*
import com.ndipatri.solarmonitor.fragments.ConfigDialogFragmentViewModel.USER_STATE.*
import kotlinx.android.synthetic.main.fragment_configure_panel.view.*

class ConfigDialogFragment : DialogFragment() {

    private lateinit var viewModel: ConfigDialogFragmentViewModel

    private lateinit var dialogView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This manages all Fragment state.. both storage of state and transitions from one state
        // to another
        viewModel = ViewModelProviders.of(this).get(ConfigDialogFragmentViewModel::class.java)
    }

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

        viewModel.searchForPanel()

        subscribeToUserState()

        subscribeToMessagesForUser()

        subscribeToValidPanelConfigData()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        GoogleProximity.getInstance().redirectToAuthenticationActivityIfNecessary(activity)
    }

    private fun subscribeToUserState() {
        viewModel.userState.observe(this, Observer<ConfigDialogFragmentViewModel.USER_STATE> {

            when (it) {
                SEARCHING_FOR_PANEL -> {
                    dialogView.progressTextView.show().text = getString(R.string.search_for_nearby_panel)

                    dialogView.panelDescriptionEditText.gone()
                    dialogView.panelIdEditText.gone()

                    dialogView.progressBar.show()

                    dialogView.firstUserActionButton.gone()

                    dialogView.secondUserActionButton.gone()
                }

                NO_PANEL_FOUND -> {
                    dialogView.progressTextView.show().text = getString(R.string.no_nearby_panels_were_found)

                    dialogView.panelDescriptionEditText.gone()
                    dialogView.panelIdEditText.gone()

                    dialogView.progressBar.hide()

                    dialogView.firstUserActionButton.show().setText(R.string.search_again)
                    dialogView.firstUserActionButton.setOnClickListener { viewModel.searchForPanel() }

                    dialogView.secondUserActionButton.gone()
                }

                PANEL_FOUND -> {
                    dialogView.panelDescriptionEditText.show().setText(viewModel.scannedPanel.value!!.description)
                    dialogView.panelDescriptionEditText.addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

                        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

                        override fun afterTextChanged(newPanelDescription: Editable) {
                            viewModel.newPanelDescriptionEntered(dialogView.panelDescriptionEditText.text.toString())
                        }
                    })

                    dialogView.panelIdEditText.apply {
                        show()
                        hint = activity!!.getString(R.string.enter_customer_id)
                        setText(viewModel.scannedPanel.value!!.id)
                    }

                    dialogView.panelIdEditText.addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

                        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

                        override fun afterTextChanged(newCustomerId: Editable) {
                            viewModel.newPanelIdEntered(dialogView.panelIdEditText.text.toString())
                        }
                    })

                    dialogView.progressTextView.hide()
                    dialogView.progressBar.hide()

                    dialogView.firstUserActionButton.text = activity!!.getString(R.string.configure_panel)
                    dialogView.firstUserActionButton.show()
                    dialogView.firstUserActionButton.setOnClickListener {
                        viewModel.configurePanel(description = dialogView.panelDescriptionEditText.text.toString(),
                                                 id = dialogView.panelIdEditText.text.toString())
                    }

                    dialogView.secondUserActionButton.text = activity!!.getString(R.string.erase_panel)
                    dialogView.secondUserActionButton.show()
                    dialogView.secondUserActionButton.setOnClickListener { viewModel.eraseNearbyPanel() }
                }

                CONFIGURING_PANEL -> {
                    dialogView.panelDescriptionEditText.gone()
                    dialogView.panelIdEditText.gone()

                    dialogView.progressBar.show()
                    dialogView.progressTextView.show()
                    dialogView.progressTextView.setText(R.string.configuring_nearby_panel)

                    dialogView.firstUserActionButton.gone()
                    dialogView.secondUserActionButton.gone()
                }

                CONFIGURATION_ERROR -> {
                    dialogView.panelDescriptionEditText.hide()
                    dialogView.panelIdEditText.hide()

                    dialogView.progressTextView.show()
                    dialogView.progressTextView.setText(R.string.failed_to_configure_panel)
                    dialogView.progressBar.hide()

                    dialogView.firstUserActionButton.setText(R.string.OK)
                    dialogView.firstUserActionButton.show()
                    dialogView.firstUserActionButton.setOnClickListener { viewModel.configurationError() }
                    dialogView.secondUserActionButton.hide()
                }

                DONE -> {
                    dismiss()
                }
            }
        })
    }

    private fun subscribeToValidPanelConfigData() {
        viewModel.panelConfigDataValid.observe(this, Observer<Boolean> {
            it?.let {
                dialogView.firstUserActionButton.isEnabled = it
            }
        })
    }

    private fun subscribeToMessagesForUser() {
        viewModel.userMessage.observe(this, Observer<String> {
            context!!.toast(it!!)
        })
    }

    companion object {
        private val TAG = ConfigDialogFragment::class.java!!.getSimpleName()
    }
}



