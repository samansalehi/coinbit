package com.bit.saman.coinbit;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.arch.persistence.room.Room;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import com.bit.saman.coinbit.model.CoinDatabase;
import com.bit.saman.coinbit.model.PriceEntity;

import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        scheduleJob();
        updateContent();
    }

    private void updateContent() {
        final Button bitcoin=findViewById(R.id.bitcoin);
        final Button dollar=findViewById(R.id.dollar);
        final Button rial=findViewById(R.id.rial);

        new AsyncTask<Void, Void, PriceEntity>() {
            @Override
            protected PriceEntity doInBackground(Void... voids) {
                CoinDatabase db = Room.databaseBuilder(getApplicationContext(), CoinDatabase.class, "CoinDB").build();
                return db.pricesDao().getLastPrice();
            }

            @Override
            protected void onPostExecute(PriceEntity priceEntity) {
                if (priceEntity!=null) {
                    super.onPostExecute(priceEntity);
                    DecimalFormat formatter = new DecimalFormat("#,###.00");
                    bitcoin.setText(formatter.format(priceEntity.getBitcoin()));
                    dollar.setText(formatter.format(priceEntity.getDollar()));
                    rial.setText(formatter.format(Math.round(priceEntity.getDollar() * priceEntity.getBitcoin())));
                }
            }
        }.execute();
    }

    private void scheduleJob() {
        JobScheduler jobScheduler= (JobScheduler) getApplicationContext().getSystemService(Context.JOB_SCHEDULER_SERVICE);
        ComponentName componentName =new ComponentName(this,CheckPricesService.class);
        JobInfo jobInfoObj = new JobInfo.Builder(1, componentName)
                .setPeriodic(15000*60).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).build();
        jobScheduler.schedule(jobInfoObj);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(this);
        Boolean notification = sharedPref.getBoolean
                (SettingsActivity.KEY_NOTIFICATION, false);
        Toast.makeText(this, notification.toString(), Toast.LENGTH_SHORT).show();

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
