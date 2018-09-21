package com.bit.saman.coinbit;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.support.v7.preference.PreferenceManager;
import android.widget.Toast;

import com.bit.saman.coinbit.model.CoinDatabase;
import com.bit.saman.coinbit.model.PriceEntity;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

public class CheckPricesService extends JobService {
    private CoinDatabase db;
    private PriceEntity oldPrice;

    private class FetchPrices extends AsyncTask<URL, Integer, PriceEntity> {
        @Override
        protected PriceEntity doInBackground(URL... urls) {
            String doc = null;
            try {
                doc = Jsoup.connect(urls[0].toString()).ignoreContentType(true).execute().body();
                JSONObject bitcoinObj = new JSONObject(doc);

                Elements element = Jsoup.connect(urls[1].toString()).get()
                        .getElementsByClass("profile-container container")
                        .select("span[itemprop$=price]");

                PriceEntity priceEntity = new PriceEntity();
                priceEntity.setBitcoin(Double.parseDouble(new JSONObject(new JSONObject(bitcoinObj.getString("bpi")).getString("USD")).getString("rate_float")));
                priceEntity.setDollar(Double.parseDouble(element.get(0).childNodes().get(0).toString().replace(",", "")));
                priceEntity.setDate(new Date());
                insertLastPriceToDb(priceEntity);

                return priceEntity;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(PriceEntity result) {
            showDialog("Bitcoin " + result.getBitcoin() + " $" + ", Dollar" + result.getDollar());
        }

        private void showDialog(String s) {
            Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
        }

    }


    @Override
    public boolean onStartJob(JobParameters params) {
        try {
            URL[] urls = new URL[2];
            urls[0] = new URL("https://api.coindesk.com/v1/bpi/currentprice.json");
            urls[1] = new URL("http://www.tgju.org/chart/price_dollar_rl");
            new FetchPrices().execute(urls);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void insertLastPriceToDb(PriceEntity priceEntity) {
        new DBconnection().execute(priceEntity);
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    private class DBconnection extends AsyncTask<PriceEntity, Void, Boolean> {

        @Override
        protected Boolean doInBackground(PriceEntity... priceEntities) {
            db = Room.databaseBuilder(getApplicationContext(), CoinDatabase.class, "CoinDB").build();
            oldPrice = db.pricesDao().getLastPrice();
            if (isChange(oldPrice, priceEntities[0])) {
                Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), "12")
                        .setSmallIcon(R.mipmap.bitcoin)
                        .setContentTitle("New Prices Change")
                        .setContentText("Bitcoin " + priceEntities[0].getBitcoin() + " $" + ", Dollar" + priceEntities[0].getDollar())
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT).setSound(alarmSound);

                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, intent, Intent.FLAG_ACTIVITY_NEW_TASK);
                mBuilder.setContentIntent(pi);
                NotificationManager mNotificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.notify(0, mBuilder.build());
            }

            if (oldPrice == null || oldPrice.getBitcoin() != priceEntities[0].getBitcoin() || oldPrice.getDollar() != priceEntities[0].getDollar()) {
                db.pricesDao().insertPrice(priceEntities[0]);
            }
            return true;
        }

        private boolean isChange(PriceEntity oldPrice, PriceEntity result) {
            double newBitPrice = result.getBitcoin();
            double newDollarPrice = result.getDollar();
            double oldBitPrice = oldPrice.getBitcoin();
            double oldDollarPrice = oldPrice.getDollar();

            double changePercent = getPropertyValue("changePercent");
            if ((newBitPrice >= oldBitPrice * changePercent + oldBitPrice)
                    || (newBitPrice <= oldBitPrice - oldBitPrice * changePercent)
                    || (newDollarPrice >= oldDollarPrice * changePercent + oldDollarPrice)
                    || (newDollarPrice <= oldDollarPrice - oldDollarPrice * changePercent)) {
                return true;
            } else {
                return false;
            }
        }

        private double getPropertyValue(String key) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            return Double.valueOf(preferences.getString(key, "2"))/100;
        }
    }
}
