package com.hbtec.expencetracker;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface RecurringTransactionDao {
    @Query("SELECT * FROM recurring_transactions ORDER BY dayOfMonth ASC")
    List<RecurringTransaction> getAllSync();

    @Insert
    void insert(RecurringTransaction transaction);

    @Update
    void update(RecurringTransaction transaction);

    @Delete
    void delete(RecurringTransaction transaction);
}

