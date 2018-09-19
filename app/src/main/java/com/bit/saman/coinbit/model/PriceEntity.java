package com.bit.saman.coinbit.model;
import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;

import java.util.Date;

@Entity
public class PriceEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private double bitcoin;
    private double dollar;

    @ColumnInfo(name = "DateTime")
    @TypeConverters(TimestampConverter.class)
    public Date date;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getBitcoin() {
        return bitcoin;
    }

    public void setBitcoin(double bitcoin) {
        this.bitcoin = bitcoin;
    }

    public double getDollar() {
        return dollar;
    }

    public void setDollar(double dollar) {
        this.dollar = dollar;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
