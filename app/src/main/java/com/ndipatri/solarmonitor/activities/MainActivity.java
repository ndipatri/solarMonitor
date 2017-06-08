package com.ndipatri.solarmonitor.activities;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ndipatri.solarmonitor.R;
import com.ndipatri.solarmonitor.SolarMonitorApp;
import com.ndipatri.solarmonitor.services.SolarOutputService;

import javax.inject.Inject;

import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;

public class MainActivity extends AppCompatActivity {

    @Inject SolarOutputService solarOutputService;

    private ProgressBar refreshProgressBar;
    private TextView dialogTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((SolarMonitorApp) getApplication()).getObjectGraph().inject(this);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        refreshProgressBar = (ProgressBar) findViewById(R.id.refreshProgressBar);
        dialogTextView = (TextView) findViewById(R.id.dialogTextView);

        refreshProgressBar.setVisibility(View.INVISIBLE);
        dialogTextView.setText("Click to load Solar Output ...");

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Retrieving latest solar output ...", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                refreshProgressBar.setVisibility(View.VISIBLE);
                dialogTextView.setVisibility(View.INVISIBLE);

                solarOutputService.getSolarOutput("123").subscribe(new SingleObserver<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        // we're not going to worry about this for now.
                    }

                    @Override
                    public void onSuccess(String solarOutput) {
                        refreshProgressBar.setVisibility(View.INVISIBLE);

                        dialogTextView.setVisibility(View.VISIBLE);
                        dialogTextView.setText(solarOutput);
                    }

                    @Override
                    public void onError(Throwable e) {
                        // we're not going to worry about this for now.
                    }
                });
            }
        });
    }

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
        }

        return super.onOptionsItemSelected(item);
    }
}
