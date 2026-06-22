package com.hbtec.expencetracker;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    public static class TransactionGroup {
        public String category;
        public String type; // "Income" or "Expense"
        public double totalAmount;
        public List<Transaction> transactions = new ArrayList<>();

        public TransactionGroup(String category, String type) {
            this.category = category;
            this.type = type;
        }
    }

    private List<TransactionGroup> groups = new ArrayList<>();
    private OnTransactionClickListener listener;
    private boolean isGrouped;

    public interface OnTransactionClickListener {
        void onTransactionClick(TransactionGroup group);
    }

    public TransactionAdapter(OnTransactionClickListener listener, boolean isGrouped) {
        this.listener = listener;
        this.isGrouped = isGrouped;
    }

    public void setOnTransactionClickListener(OnTransactionClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        TransactionGroup group = groups.get(position);

        if ("Transfer".equalsIgnoreCase(group.type)) {
            holder.tvCategory.setText("Transfer to " + group.category);
            if (group.transactions.size() == 1) {
                Transaction transaction = group.transactions.get(0);
                holder.tvDescription.setText("From " + transaction.wallet + (transaction.description != null && !transaction.description.isEmpty()
                        ? " • " + transaction.description : ""));
                holder.tvDate.setText(transaction.date);
            } else {
                holder.tvDescription.setText(String.format(Locale.getDefault(), "%d transfers", group.transactions.size()));
                holder.tvDate.setText("Last: " + group.transactions.get(0).date);
            }
        } else {
            holder.tvCategory.setText(group.category);
            if (group.transactions.size() == 1) {
                Transaction transaction = group.transactions.get(0);
                holder.tvDescription.setText(transaction.description != null && !transaction.description.isEmpty()
                        ? transaction.description : "—");
                holder.tvDate.setText(transaction.date);
            } else {
                holder.tvDescription.setText(String.format(Locale.getDefault(), "%d transactions", group.transactions.size()));
                holder.tvDate.setText("Last: " + group.transactions.get(0).date);
            }
        }

        String amountText = String.format(Locale.getDefault(), "₹%,.0f", group.totalAmount);

        // Map categories to specific icons using category name
        int iconResId;
        String category = group.category != null ? group.category.toLowerCase() : "";
        if ("Transfer".equalsIgnoreCase(group.type)) {
            iconResId = R.drawable.ic_transfer;
        } else if (category.contains("grocery") || category.contains("groceries") || category.contains("cooking")) {
            iconResId = R.drawable.ic_grocery;
        } else if (category.contains("food") || category.contains("eat") || category.contains("delivery") || category.contains("restaurant")) {
            iconResId = R.drawable.ic_grocery;
        } else if (category.contains("salary") || category.contains("wage") || category.contains("bonus") || category.contains("freelanc") || category.contains("consulting")) {
            iconResId = R.drawable.ic_salary;
        } else if (category.contains("rental income") || category.contains("royalt") || category.contains("youtube") || category.contains("blog") || category.contains("affiliate") || category.contains("interest") || category.contains("dividend") || category.contains("capital gain") || category.contains("shop profit") || category.contains("online business") || category.contains("e-commerce") || category.contains("services business")) {
            iconResId = R.drawable.ic_salary;
        } else if (category.contains("school") || category.contains("college") || category.contains("book") || category.contains("stationery") || category.contains("coaching") || category.contains("course") || category.contains("education") || category.contains("tuition")) {
            iconResId = R.drawable.ic_education;
        } else if (category.contains("fitness") || category.contains("gym") || category.contains("grooming") || category.contains("salon") || category.contains("cosmetic")) {
            iconResId = R.drawable.ic_fitness;
        } else if (category.contains("rent") || category.contains("property tax") || category.contains("maintenance") || category.contains("society") || category.contains("repair") || category.contains("upkeep")) {
            iconResId = R.drawable.ic_rent;
        } else if (category.contains("doctor") || category.contains("medicine") || category.contains("health") || category.contains("hospital") || category.contains("treatment")) {
            iconResId = R.drawable.ic_health;
        } else if (category.contains("bill") || category.contains("electricity") || category.contains("mobile") || category.contains("wifi") || category.contains("internet") || category.contains("recharge") || category.contains("dth") || category.contains("ott") || category.contains("lpg") || category.contains("subscription")) {
            iconResId = R.drawable.ic_electricity;
        } else if (category.contains("fuel") || category.contains("cab") || category.contains("transport") || category.contains("vehicle") || category.contains("ola") || category.contains("uber")) {
            iconResId = R.drawable.ic_fuel;
        } else if (category.contains("travel") || category.contains("vacation") || category.contains("flight") || category.contains("hotel")) {
            iconResId = R.drawable.ic_travel;
        } else if (category.contains("movie") || category.contains("outing") || category.contains("hobb") || category.contains("game") || category.contains("entertainment")) {
            iconResId = R.drawable.ic_entertainment;
        } else if (category.contains("insurance") || category.contains("emi") || category.contains("investment") || category.contains("sip") || category.contains("fd") || category.contains("saving")) {
            iconResId = R.drawable.ic_insurance;
        } else if (category.contains("gift") || category.contains("donation") || category.contains("festival") || category.contains("inheritance") || category.contains("lottery") || category.contains("tax refund")) {
            iconResId = R.drawable.ic_gift;
        } else if (category.contains("cloth") || category.contains("shopping")) {
            iconResId = R.drawable.ic_grocery;
        } else {
            iconResId = R.drawable.ic_generic_transaction;
        }
        holder.ivCategoryIcon.setImageResource(iconResId);

        int tintColor;
        int circleBgColor;

        if ("Transfer".equalsIgnoreCase(group.type)) {
            holder.tvAmount.setText(amountText);
            holder.tvAmount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary));
            holder.itemContainer.setBackgroundResource(R.drawable.list_bg_shape);

            tintColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.primary);
            circleBgColor = Color.parseColor("#EBF1FA");
        } else if ("Income".equals(group.type)) {
            holder.tvAmount.setText("+ " + amountText);
            holder.tvAmount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.income_green));
            holder.itemContainer.setBackgroundResource(R.drawable.item_card_income);

            tintColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.income_green);
            circleBgColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.income_green_bg);
        } else {
            holder.tvAmount.setText("- " + amountText);
            holder.tvAmount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary));
            holder.itemContainer.setBackgroundResource(R.drawable.item_card_expense);

            tintColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.expense_red);
            circleBgColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.expense_red_bg);
        }

        // Apply circle background
        GradientDrawable circleBg = new GradientDrawable();
        circleBg.setShape(GradientDrawable.OVAL);
        circleBg.setColor(circleBgColor);
        holder.categoryIndicator.setBackground(circleBg);

        // Apply icon tint
        holder.ivCategoryIcon.setImageTintList(ColorStateList.valueOf(tintColor));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTransactionClick(group);
            }
        });
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    public void setTransactions(List<Transaction> list) {
        groups.clear();
        if (list == null || list.isEmpty()) {
            notifyDataSetChanged();
            return;
        }

        if (isGrouped) {
            // Group by category and type
            Map<String, TransactionGroup> map = new LinkedHashMap<>();
            for (Transaction t : list) {
                String key = (t.category != null ? t.category.toLowerCase().trim() : "") + "_" + t.type;
                TransactionGroup group = map.get(key);
                if (group == null) {
                    group = new TransactionGroup(t.category, t.type);
                    map.put(key, group);
                }
                group.transactions.add(t);
                group.totalAmount += t.amount;
            }
            groups.addAll(map.values());
        } else {
            // Flat list: each transaction is its own group of size 1
            for (Transaction t : list) {
                TransactionGroup group = new TransactionGroup(t.category, t.type);
                group.transactions.add(t);
                group.totalAmount = t.amount;
                groups.add(group);
            }
        }
        notifyDataSetChanged();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvDescription, tvDate, tvAmount;
        ImageView ivCategoryIcon;
        LinearLayout itemContainer;
        View categoryIndicator;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            itemContainer = itemView.findViewById(R.id.itemContainer);
            categoryIndicator = itemView.findViewById(R.id.categoryIndicator);
        }
    }
}

