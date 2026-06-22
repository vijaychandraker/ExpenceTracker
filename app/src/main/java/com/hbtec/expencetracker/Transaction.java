package com.hbtec.expencetracker;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions")
public class Transaction {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String date;
    public String type; // "Income" or "Expense"
    public String category;
    public double amount;
    public String description;
    public String fy;
    public String month;
    public String wallet; // "Cash", "Bank", "Card"

    // Required by Firebase for deserialization
    @androidx.room.Ignore
    public Transaction() {
    }

    public Transaction(String date, String type, String category, double amount, String description, String wallet) {
        this.date = normalizeDate(date);
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.description = description;
        this.wallet = wallet != null ? wallet : "Cash";
        this.fy = getFYFromDate(this.date);
        this.month = getMonthNameFromDate(this.date);
    }

    public static String normalizeDate(String dateStr) {
        if (dateStr == null || !dateStr.contains("-")) return dateStr;
        try {
            String[] parts = dateStr.trim().split("-");
            if (parts.length == 3) {
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int day = Integer.parseInt(parts[2]);
                return String.format(java.util.Locale.getDefault(), "%d-%02d-%02d", year, month, day);
            }
        } catch (Exception ignored) {}
        return dateStr;
    }

    @androidx.room.Ignore
    public Transaction(String date, String type, String category, double amount, String description) {
        this(date, type, category, amount, description, "Cash");
    }

    public static String getFYFromDate(String dateStr) {
        if (dateStr == null || !dateStr.contains("-")) return "";
        try {
            String[] parts = dateStr.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]); // 1-based
            int startYear = month < 4 ? year - 1 : year;
            int endYear = startYear + 1;
            return startYear + "-" + String.format(java.util.Locale.getDefault(), "%02d", endYear % 100);
        } catch (Exception e) {
            return "";
        }
    }

    public static String getMonthNameFromDate(String dateStr) {
        if (dateStr == null || !dateStr.contains("-")) return "";
        try {
            String[] parts = dateStr.split("-");
            int month = Integer.parseInt(parts[1]); // 1-based
            String[] months = {
                "", "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            };
            if (month >= 1 && month <= 12) {
                return months[month];
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }
}

