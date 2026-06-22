package com.hbtec.expencetracker;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FirebaseSyncManager {
    private static final String TAG = "FirebaseSyncManager";
    private static final String PREFS_NAME = "ExpenseTrackerPrefs";
    private static final String KEY_AUTO_SYNC = "firebase_auto_sync";
    private static final String KEY_LAST_SYNC = "firebase_last_sync";
    
    private static final String DATABASE_URL = "https://expencetracker-bb26d-default-rtdb.firebaseio.com";
    private static FirebaseSyncManager instance;
    private final DatabaseReference databaseReference;

    public interface SyncCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    private FirebaseSyncManager() {
        try {
            // Enable offline capability so sync functions even on spotty connections
            FirebaseDatabase.getInstance(DATABASE_URL).setPersistenceEnabled(true);
        } catch (Exception e) {
            Log.d(TAG, "Firebase persistence already configured: " + e.getMessage());
        }
        databaseReference = FirebaseDatabase.getInstance(DATABASE_URL).getReference("devices");
    }

    public static synchronized FirebaseSyncManager getInstance() {
        if (instance == null) {
            instance = new FirebaseSyncManager();
        }
        return instance;
    }

    public String getDeviceId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public boolean isAutoSyncEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_AUTO_SYNC, false); // Default: false, let user toggle on
    }

    public void setAutoSyncEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_AUTO_SYNC, enabled)
                .apply();
    }

    public long getLastSyncTime(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(KEY_LAST_SYNC, 0);
    }

    public void setLastSyncTime(Context context, long time) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_SYNC, time)
                .apply();
    }

    /**
     * Get target Firebase database reference depending on user sign-in state
     */
    public DatabaseReference getTargetRef(Context context) {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            return FirebaseDatabase.getInstance(DATABASE_URL).getReference("users").child(user.getUid()).child("transactions");
        } else {
            String deviceId = getDeviceId(context);
            return FirebaseDatabase.getInstance(DATABASE_URL).getReference("devices").child(deviceId).child("transactions");
        }
    }

    /**
     * Upload all local transactions to Firebase Realtime Database
     */
    public void uploadTransactions(Context context, List<Transaction> transactions, SyncCallback callback) {
        getTargetRef(context).setValue(transactions)
                .addOnSuccessListener(aVoid -> {
                    setLastSyncTime(context, System.currentTimeMillis());
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    /**
     * Fetch all transactions from Firebase Realtime Database and replace the local database content.
     */
    public void downloadTransactions(Context context, SyncCallback callback) {
        getTargetRef(context).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Transaction> transactions = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Transaction transaction = ds.getValue(Transaction.class);
                    if (transaction != null) {
                        transaction.date = Transaction.normalizeDate(transaction.date);
                        transaction.fy = Transaction.getFYFromDate(transaction.date);
                        transaction.month = Transaction.getMonthNameFromDate(transaction.date);
                        transactions.add(transaction);
                    }
                }
                
                // Replace Room contents
                AppDatabase db = AppDatabase.getInstance(context);
                java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
                executor.execute(() -> {
                    try {
                        db.runInTransaction(() -> {
                            db.clearAllTables();
                            db.transactionDao().insertAll(transactions);
                        });
                        setLastSyncTime(context, System.currentTimeMillis());
                        if (callback != null) callback.onSuccess();
                    } catch (Exception e) {
                        if (callback != null) callback.onFailure(e.getMessage());
                    } finally {
                        executor.shutdown();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (callback != null) callback.onFailure(error.getMessage());
            }
        });
    }
}

