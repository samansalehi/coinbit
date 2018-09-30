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
import java.text.DecimalFormat;
import java.util.Date;

public class CheckPricesService extends JobService {
    private CoinDatabase db;
    private PriceEntity oldPrice;

    private class FetchPrices extends AsyncTask<URL, Integer, PriceEntity> {
        @Override
        protected PriceEntity doInBackground(URL... urls) {
            String doc = null;
            try {
                doc = Jsoup.connect(urls[0].toString() + "bitcoin/").ignoreContentType(true).execute().body();
                JSONObject bitcoinObj = new JSONObject(doc.replace("[", "").replace("]", ""));

                doc = Jsoup.connect(urls[0].toString() + "ethereum/").ignoreContentType(true).execute().body();
                JSONObject etherObj = new JSONObject(doc.replace("[", "").replace("]", ""));

                Elements element = Jsoup.connect(urls[1].toString()).get()
                        .getElementsByClass("data-table market-table market-section-right")
                        .select("td.nf");
                PriceEntity priceEntity = new PriceEntity();
                priceEntity.setBitcoin(Double.parseDouble(bitcoinObj.getString("price_usd")));
                priceEntity.setEther(Double.parseDouble(etherObj.getString("price_usd")));
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
            //showDialog("Bitcoin " + result.getBitcoin() + " $" + ", Dollar" + result.getDollar());
        }

        private void showDialog(String s) {
            Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
        }

    }


    @Override
    public boolean onStartJob(JobParameters params) {
        try {
            URL[] urls = new URL[2];
            urls[0] = new URL("https://api.coinmarketcap.com/v1/ticker/");
            urls[1] = new URL("http://www.tgju.org/currency");
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
            double myEther = getPropertyValue("myEther");
            double myBitcoin = getPropertyValue("myBitcoin");
            if (oldPrice != null) {
                double coinsProfit[] = CoinUtils.calculateProfit(priceEntities[0].getBitcoin(), priceEntities[0].getEther(),myBitcoin,myEther);
                if (isEtherToBitcoin(priceEntities[0])) {
                    showNotification(priceEntities[0], "Change Ethereum to Bitcoin", coinsProfit[0]);
                } else if (isBitcoinToEther(priceEntities[0])) {
                    showNotification(priceEntities[0], "Change Bitcoin to Ethereum", coinsProfit[1]);
                }
            }

            if (oldPrice == null || oldPrice.getBitcoin() != priceEntities[0].getBitcoin() || oldPrice.getDollar() != priceEntities[0].getDollar()) {
                db.pricesDao().insertPrice(priceEntities[0]);
            }
            return true;
        }

        private boolean isBitcoinToEther(PriceEntity priceEntity) {
            double changePercent = getPropertyValue("changePercent") / 100;
            double myEther = getPropertyValue("myEther");
            double myBitcoin = getPropertyValue("myBitcoin");

            if (priceEntity.getBitcoin() * myBitcoin / priceEntity.getEther() > myEther + myEther * changePercent)
                return true;
            return false;
        }

        private void showNotification(PriceEntity priceEntity, String title, double profit) {
            DecimalFormat formatter = new DecimalFormat("#,###.0000");
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), "12")
                    .setSmallIcon(R.mipmap.bitcoin)
                    .setContentTitle(title)
                    .setContentText("Bitcoin " + formatter.format(priceEntity.getBitcoin()) + " $" + ", Ether " + formatter.format(priceEntity.getEther()) + ", profit " + formatter.format(profit))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT).setSound(alarmSound);

            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, intent, Intent.FLAG_ACTIVITY_NEW_TASK);
            mBuilder.setContentIntent(pi);
            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(0, mBuilder.build());
        }

        private boolean isEtherToBitcoin(PriceEntity priceEntity) {
            double changePercent = getPropertyValue("changePercent") / 100;
            double myEther = getPropertyValue("myEther");
            double myBitcoin = getPropertyValue("myBitcoin");

            if (priceEntity.getEther() * myEther / priceEntity.getBitcoin() > myBitcoin + myBitcoin * changePercent)
                return true;
            return false;
        }

        private boolean isChange(PriceEntity oldPrice, PriceEntity result) {
            double newBitPrice = result.getBitcoin();
            double newDollarPrice = result.getDollar();
            double oldBitPrice = oldPrice.getBitcoin();
            double oldDollarPrice = oldPrice.getDollar();
            double oldEtherPrice = oldPrice.getEther();
            double newEtherPrice = result.getEther();

            double changePercent = getPropertyValue("changePercent") / 100;
            if ((newBitPrice >= oldBitPrice * changePercent + oldBitPrice)
                    || (newBitPrice <= oldBitPrice - oldBitPrice * changePercent)
                    || (newDollarPrice >= oldDollarPrice * changePercent + oldDollarPrice)
                    || (newDollarPrice <= oldDollarPrice - oldDollarPrice * changePercent)
                    || (newEtherPrice >= oldEtherPrice * changePercent + oldEtherPrice)
                    || (newEtherPrice <= oldEtherPrice - oldEtherPrice * changePercent)
                    ) {
                return true;
            } else {
                return false;
            }
        }

        private double getPropertyValue(String key) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            return Double.valueOf(preferences.getString(key, "2"));
        }
    }
}
