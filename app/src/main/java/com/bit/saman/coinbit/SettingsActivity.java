package com.bit.saman.coinbit;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SettingsActivity extends AppCompatActivity {

    public static final String KEY_NOTIFICATION = "notification";
    public static final String KEY_MYBITCOINS="myBitcoins";
    public static final String KEY_MYRIALS="myRials";
    public static final String KEY_PERCENT_PROFIT="percentToSell";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}
