package com.hbtec.expencetracker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Transaction.class, RecurringTransaction.class}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    public abstract TransactionDao transactionDao();
    public abstract RecurringTransactionDao recurringTransactionDao();

    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE recurring_transactions ADD COLUMN frequency TEXT DEFAULT 'Monthly'");
            database.execSQL("ALTER TABLE recurring_transactions ADD COLUMN dayOfWeek INTEGER DEFAULT 1 NOT NULL");
            database.execSQL("ALTER TABLE recurring_transactions ADD COLUMN lastAddedDate TEXT DEFAULT ''");
            database.execSQL("UPDATE recurring_transactions SET lastAddedDate = lastAddedMonth || '-01' WHERE lastAddedMonth IS NOT NULL AND lastAddedMonth != ''");
        }
    };

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "expense_tracker_db")
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}

