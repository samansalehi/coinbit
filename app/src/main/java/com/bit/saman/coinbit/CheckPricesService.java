package com.bit.saman.coinbit;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.arch.persistence.room.Room;
import android.os.AsyncTask;
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

public class CheckPricesService extends JobService {
    private CoinDatabase db;

    private class FetchPrices extends AsyncTask<URL, Integer, Double> {
        @Override
        protected Double doInBackground(URL... urls) {
            String doc = null;
            try {
                doc = Jsoup.connect(urls[0].toString()).ignoreContentType(true).execute().body();
                JSONObject jsonObject=new JSONObject(doc);
                 return Double.valueOf(new JSONObject(new JSONObject(jsonObject.getString("bpi")).getString("USD")).getString("rate_float"));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return Double.valueOf(0);
        }

        protected void onPostExecute(Double result) {
            PriceEntity priceEntity = new PriceEntity();
            priceEntity.setBitcoin(12l);
            priceEntity.setDollar(14l);
            insertLastPriceToDb(priceEntity);
            showDialog("Bitcoin Price " + result + " $");
        }

        private void showDialog(String s) {
            Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
        }

    }


    @Override
    public boolean onStartJob(JobParameters params) {
        try {
            new FetchPrices().execute(new URL("https://api.coindesk.com/v1/bpi/currentprice.json"));
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
            PriceEntity oldPrice = db.pricesDao().getLastPrice();

            if (oldPrice==null || oldPrice.getBitcoin() != priceEntities[0].getBitcoin() || oldPrice.getDollar() != priceEntities[0].getDollar()) {
                db.pricesDao().insertPrice(priceEntities[0]);
            }
            return true;
        }


    }
}
