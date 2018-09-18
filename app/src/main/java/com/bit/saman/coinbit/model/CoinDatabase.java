package com.bit.saman.coinbit.model;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(entities = {PriceEntity.class}, version = 1)
public abstract class CoinDatabase extends RoomDatabase {
    public abstract PricesDao pricesDao();
}
