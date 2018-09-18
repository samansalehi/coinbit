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
    private long bitcoin;
    private long dollar;

    @ColumnInfo(name = "DateTime")
    @TypeConverters(TimestampConverter.class)
    public Date date;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getBitcoin() {
        return bitcoin;
    }

    public void setBitcoin(long bitcoin) {
        this.bitcoin = bitcoin;
    }

    public long getDollar() {
        return dollar;
    }

    public void setDollar(long dollar) {
        this.dollar = dollar;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
