package com.hbtec.expencetracker;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY id DESC")
    LiveData<List<Transaction>> getAllTransactions();

    @Query("SELECT * FROM transactions ORDER BY id DESC")
    List<Transaction> getAllTransactionsSync();

    @Insert
    void insert(Transaction transaction);

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    void insertAll(List<Transaction> transactions);

    @Update
    void update(Transaction transaction);

    @Delete
    void delete(Transaction transaction);

    @Query("SELECT * FROM transactions WHERE LOWER(TRIM(category)) = LOWER(TRIM(:category)) ORDER BY id DESC LIMIT 1")
    Transaction getLastTransactionByCategory(String category);

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'Income'")
    LiveData<Double> getTotalIncome();

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'Expense'")
    LiveData<Double> getTotalExpense();

    @Query("SELECT category, SUM(amount) as totalAmount FROM transactions WHERE type = 'Expense' GROUP BY category")
    LiveData<List<CategorySummary>> getExpenseByCategory();

    // ===== Filtered queries by date range (for FY/Month filtering) =====

    @Query("SELECT * FROM transactions WHERE date >= :startDate AND date <= :endDate ORDER BY id DESC")
    LiveData<List<Transaction>> getTransactionsByDateRange(String startDate, String endDate);

    @Query("SELECT * FROM transactions WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC, id ASC")
    List<Transaction> getTransactionsByDateRangeSync(String startDate, String endDate);

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'Income' AND date >= :startDate AND date <= :endDate")
    LiveData<Double> getTotalIncomeByDateRange(String startDate, String endDate);

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'Expense' AND date >= :startDate AND date <= :endDate")
    LiveData<Double> getTotalExpenseByDateRange(String startDate, String endDate);

    @Query("SELECT category, SUM(amount) as totalAmount FROM transactions WHERE type = 'Expense' AND date >= :startDate AND date <= :endDate GROUP BY category")
    LiveData<List<CategorySummary>> getExpenseByCategoryByDateRange(String startDate, String endDate);

    class CategorySummary {
        public String category;
        public double totalAmount;
    }
}

