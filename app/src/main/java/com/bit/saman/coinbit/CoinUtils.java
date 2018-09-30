package com.bit.saman.coinbit;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;

import com.bit.saman.coinbit.model.CoinDatabase;
import com.bit.saman.coinbit.model.PriceEntity;

public class CoinUtils {


    public static double[] calculateProfit(double bitcoin, double ether,double myBitcoin,double myEther) {
        double coins[] = new double[2];
        coins[0] = (myEther * ether / bitcoin - myBitcoin) * bitcoin;
        coins[1] = (myBitcoin * bitcoin / ether - myEther) * ether;
        return coins;
    }


}
