package com.hbtec.expencetracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecurringTransactionAdapter extends RecyclerView.Adapter<RecurringTransactionAdapter.ViewHolder> {

    public interface OnDeleteClickListener {
        void onDeleteClick(RecurringTransaction item);
    }

    private List<RecurringTransaction> items = new ArrayList<>();
    private final OnDeleteClickListener deleteClickListener;

    public RecurringTransactionAdapter(OnDeleteClickListener deleteClickListener) {
        this.deleteClickListener = deleteClickListener;
    }

    public void setItems(List<RecurringTransaction> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recurring_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecurringTransaction item = items.get(position);
        holder.tvRecCategory.setText(item.category);
        
        String freq = item.frequency != null ? item.frequency : "Monthly";
        if ("Daily".equalsIgnoreCase(freq)) {
            holder.tvRecDayBadge.setText("Daily");
        } else if ("Weekly".equalsIgnoreCase(freq)) {
            holder.tvRecDayBadge.setText("Weekly: " + getDayOfWeekName(item.dayOfWeek));
        } else {
            holder.tvRecDayBadge.setText("Monthly: Day " + item.dayOfMonth);
        }

        String details = item.type + " • " + (item.wallet != null ? item.wallet : "Cash");
        if (item.description != null && !item.description.trim().isEmpty()) {
            details = item.description + " • " + details;
        }
        holder.tvRecDetails.setText(details);

        String amountText = String.format(Locale.getDefault(), "%,.0f", item.amount);
        if ("Income".equals(item.type)) {
            holder.tvRecAmount.setText("+₹" + amountText);
            holder.tvRecAmount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.income_green));
        } else {
            holder.tvRecAmount.setText("-₹" + amountText);
            holder.tvRecAmount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.expense_red));
        }

        holder.btnDeleteRec.setOnClickListener(v -> {
            if (deleteClickListener != null) {
                deleteClickListener.onDeleteClick(item);
            }
        });
    }

    private String getDayOfWeekName(int dayOfWeek) {
        String[] days = {"", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        if (dayOfWeek >= 1 && dayOfWeek <= 7) {
            return days[dayOfWeek];
        }
        return "Sunday";
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRecCategory, tvRecDayBadge, tvRecDetails, tvRecAmount;
        ImageButton btnDeleteRec;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRecCategory = itemView.findViewById(R.id.tvRecCategory);
            tvRecDayBadge = itemView.findViewById(R.id.tvRecDayBadge);
            tvRecDetails = itemView.findViewById(R.id.tvRecDetails);
            tvRecAmount = itemView.findViewById(R.id.tvRecAmount);
            btnDeleteRec = itemView.findViewById(R.id.btnDeleteRec);
        }
    }
}

