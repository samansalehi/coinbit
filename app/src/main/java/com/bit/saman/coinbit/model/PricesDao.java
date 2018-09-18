package com.bit.saman.coinbit.model;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface PricesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insertPrice(PriceEntity entityList);

    @Query("select * from PriceEntity limit 1")
    public PriceEntity getLastPrice();

}
