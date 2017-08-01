package com.ndipatri.solarmonitor.activities;


import android.content.DialogInterface;
import android.view.Menu;
import android.view.MenuItem;

import com.ndipatri.iot.googleproximity.activities.AuthenticationActivity;
import com.ndipatri.iot.googleproximity.activities.RequirementsActivity;
import com.ndipatri.solarmonitor.R;
import com.ndipatri.solarmonitor.fragments.ConfigurePanelDialogFragment;
import com.ndipatri.solarmonitor.providers.panelScan.PanelScanProvider;

import javax.inject.Inject;

public class BaseActivity extends RequirementsActivity {

    private static final String TAG = BaseActivity.class.getSimpleName();

    @Inject
    protected PanelScanProvider panelScanProvider;

    //region menuSetup
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_configure) {
            launchConfigureFragment();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    //endregion

    protected void launchConfigureFragment() {
        ConfigurePanelDialogFragment dialog = new ConfigurePanelDialogFragment();
        dialog.show(getSupportFragmentManager().beginTransaction(), "configure panel dialog");
        getSupportFragmentManager().executePendingTransactions();
        dialog.getDialog().setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                handleConfigureFragmentDismiss();
            }
        });
    }

    protected void handleConfigureFragmentDismiss() {
    }
}
