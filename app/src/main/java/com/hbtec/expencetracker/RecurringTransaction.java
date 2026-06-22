package com.hbtec.expencetracker;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "recurring_transactions")
public class RecurringTransaction {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String type; // "Income" or "Expense"
    public String category;
    public double amount;
    public String description;
    public String wallet; // "Cash", "Bank", "Card"
    public int dayOfMonth; // e.g. 1 to 31
    public String lastAddedMonth; // "yyyy-MM", e.g. "2026-06" to prevent double-inserting in the same month
    
    public String frequency; // "Daily", "Weekly", "Monthly"
    public int dayOfWeek; // 1 = Sun, 2 = Mon ... 7 = Sat (for Weekly)
    public String lastAddedDate; // "yyyy-MM-dd"

    // Required empty constructor
    public RecurringTransaction() {
    }

    @androidx.room.Ignore
    public RecurringTransaction(String type, String category, double amount, String description, String wallet, int dayOfMonth) {
        this(type, category, amount, description, wallet, "Monthly", dayOfMonth, 1);
    }

    @androidx.room.Ignore
    public RecurringTransaction(String type, String category, double amount, String description, String wallet, String frequency, int dayOfMonth, int dayOfWeek) {
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.description = description;
        this.wallet = wallet != null ? wallet : "Cash";
        this.frequency = frequency != null ? frequency : "Monthly";
        this.dayOfMonth = dayOfMonth;
        this.dayOfWeek = dayOfWeek;
        this.lastAddedMonth = "";
        this.lastAddedDate = "";
    }
}

