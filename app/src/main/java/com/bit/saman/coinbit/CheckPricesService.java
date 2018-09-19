package com.bit.saman.coinbit;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
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

                doc = Jsoup.connect(urls[1].toString()).ignoreContentType(true).execute().body();
                JSONObject dollarObj = new JSONObject(doc);

                PriceEntity priceEntity = new PriceEntity();
                priceEntity.setBitcoin(Double.parseDouble(new JSONObject(new JSONObject(bitcoinObj.getString("bpi")).getString("USD")).getString("rate_float")));
                priceEntity.setDollar(Double.parseDouble(new JSONObject(dollarObj.getString("sana_buy_usd")).getString("price").replace(",", "")));
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
            urls[1] = new URL("http://www.tgju.org/?act=sanarateservice&client=tgju&noview&type=json");
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
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), "12")
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .setContentTitle("New Prices Change")
                        .setContentText("Bitcoin " + priceEntities[0].getBitcoin() + " $" + ", Dollar" + priceEntities[0].getDollar())
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

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

            if ((newBitPrice >= oldBitPrice * 0.02 + oldBitPrice)
                    || (newBitPrice <= oldBitPrice - oldBitPrice * 0.02)
                    || (newDollarPrice >= oldDollarPrice * 0.02 + oldDollarPrice)
                    || (newDollarPrice <= oldDollarPrice - oldDollarPrice * 0.02)) {
                return true;
            } else {
                return false;
            }
        }

    }
}
