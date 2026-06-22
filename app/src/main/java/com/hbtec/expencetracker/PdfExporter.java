package com.hbtec.expencetracker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.util.Log;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PdfExporter {

    private static final String TAG = "PdfExporter";

    public static boolean export(
            Context context,
            List<Transaction> transactions,
            String fiscalYear,
            String monthName,
            double totalIncome,
            double totalExpense,
            Bitmap pieChartBitmap,
            Bitmap lineChartBitmap,
            Bitmap userProfileBitmap,
            String userEmail,
            OutputStream outputStream) {

        PdfDocument document = new PdfDocument();
        int pageNumber = 1;

        try {
            // ==================== PAGE 1: SUMMARY & CHARTS ====================
            PdfDocument.PageInfo pageInfo1 = new PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create();
            PdfDocument.Page page1 = document.startPage(pageInfo1);
            Canvas canvas1 = page1.getCanvas();

            Paint paint = new Paint();
            paint.setAntiAlias(true);

            // 1. Header Section
            // Draw a soft premium background block for the header
            paint.setColor(Color.parseColor("#3F2B96"));
            canvas1.drawRect(0, 0, 595, 110, paint);

            // Document Title
            paint.setColor(Color.WHITE);
            paint.setTextSize(20);
            paint.setFakeBoldText(true);
            canvas1.drawText("HisabDo Expense Statement", 30, 45, paint);

            // Subtitle
            paint.setColor(Color.parseColor("#DFE6E9"));
            paint.setTextSize(12);
            paint.setFakeBoldText(false);
            canvas1.drawText("Report Period: " + monthName + " (FY " + fiscalYear + ")", 30, 70, paint);

            // Generation Date
            String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
            canvas1.drawText("Generated: " + dateStr, 30, 90, paint);

            // User Info (Profile Email & circular picture on the right)
            if (userEmail != null && !userEmail.isEmpty()) {
                paint.setTextAlign(Paint.Align.RIGHT);
                paint.setColor(Color.WHITE);
                paint.setTextSize(11);
                canvas1.drawText(userEmail, 500, 55, paint);
                paint.setTextAlign(Paint.Align.LEFT); // Reset
            }

            if (userProfileBitmap != null) {
                // Draw circular profile image at top right
                try {
                    int size = 44;
                    int x = 520;
                    int y = 33;
                    RectF rect = new RectF(x, y, x + size, y + size);
                    
                    // Create a circle mask
                    canvas1.save();
                    android.graphics.Path path = new android.graphics.Path();
                    path.addRoundRect(rect, size / 2f, size / 2f, android.graphics.Path.Direction.CW);
                    canvas1.clipPath(path);
                    canvas1.drawBitmap(userProfileBitmap, null, rect, paint);
                    canvas1.restore();
                } catch (Exception e) {
                    Log.e(TAG, "Error drawing profile image onto PDF", e);
                }
            } else {
                // Draw default circular initials placeholder icon
                paint.setColor(Color.parseColor("#26FFFFFF"));
                canvas1.drawCircle(542, 55, 22, paint);
                paint.setColor(Color.WHITE);
                paint.setTextSize(16);
                paint.setFakeBoldText(true);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas1.drawText("U", 542, 61, paint);
                paint.setTextAlign(Paint.Align.LEFT); // Reset
                paint.setFakeBoldText(false);
            }

            // 2. Summary Cards Section
            int cardY = 135;
            int cardHeight = 65;

            // Income Card (Soft Green)
            paint.setColor(Color.parseColor("#EBF8F2"));
            canvas1.drawRoundRect(new RectF(30, cardY, 195, cardY + cardHeight), 12, 12, paint);
            paint.setColor(Color.parseColor("#2E7D32"));
            paint.setTextSize(11);
            paint.setFakeBoldText(true);
            canvas1.drawText("Total Income", 45, cardY + 24, paint);
            paint.setTextSize(15);
            canvas1.drawText(String.format("₹%,.2f", totalIncome), 45, cardY + 48, paint);

            // Expense Card (Soft Red)
            paint.setColor(Color.parseColor("#FDF2F4"));
            canvas1.drawRoundRect(new RectF(215, cardY, 380, cardY + cardHeight), 12, 12, paint);
            paint.setColor(Color.parseColor("#C62828"));
            paint.setTextSize(11);
            paint.setFakeBoldText(true);
            canvas1.drawText("Total Expenses", 230, cardY + 24, paint);
            paint.setTextSize(15);
            canvas1.drawText(String.format("₹%,.2f", totalExpense), 230, cardY + 48, paint);

            // Balance Card (Soft Blue)
            double balance = totalIncome - totalExpense;
            String balanceColor = balance >= 0 ? "#EBF3FA" : "#FDF5E6";
            String balanceTextColor = balance >= 0 ? "#1565C0" : "#D84315";
            paint.setColor(Color.parseColor(balanceColor));
            canvas1.drawRoundRect(new RectF(400, cardY, 565, cardY + cardHeight), 12, 12, paint);
            paint.setColor(Color.parseColor(balanceTextColor));
            paint.setTextSize(11);
            paint.setFakeBoldText(true);
            canvas1.drawText("Net Balance", 415, cardY + 24, paint);
            paint.setTextSize(15);
            canvas1.drawText(String.format("₹%,.2f", balance), 415, cardY + 48, paint);

            // 3. Embedded Visual Charts Section
            paint.setColor(Color.parseColor("#1C1939"));
            paint.setTextSize(13);
            paint.setFakeBoldText(true);
            canvas1.drawText("Visual Analytics Breakdown", 30, 230, paint);

            // Draw line divider under section title
            paint.setColor(Color.parseColor("#EEEEEE"));
            canvas1.drawLine(30, 240, 565, 240, paint);

            // Pie Chart (Categorized Expenses)
            if (pieChartBitmap != null) {
                Rect dstPie = new Rect(30, 260, 280, 510);
                canvas1.drawBitmap(pieChartBitmap, null, dstPie, paint);
                paint.setColor(Color.parseColor("#757575"));
                paint.setTextSize(10);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas1.drawText("Expense Category Distribution", 155, 530, paint);
                paint.setTextAlign(Paint.Align.LEFT); // Reset
            } else {
                paint.setColor(Color.parseColor("#F5F6FA"));
                canvas1.drawRoundRect(new RectF(30, 260, 280, 510), 10, 10, paint);
                paint.setColor(Color.parseColor("#BDBDBD"));
                paint.setTextSize(11);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas1.drawText("No Expense Data to Plot", 155, 385, paint);
                paint.setTextAlign(Paint.Align.LEFT); // Reset
            }

            // Savings Rate Line Chart
            if (lineChartBitmap != null) {
                Rect dstLine = new Rect(315, 260, 565, 510);
                canvas1.drawBitmap(lineChartBitmap, null, dstLine, paint);
                paint.setColor(Color.parseColor("#757575"));
                paint.setTextSize(10);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas1.drawText("Monthly Savings Rate Trend (%)", 440, 530, paint);
                paint.setTextAlign(Paint.Align.LEFT); // Reset
            } else {
                paint.setColor(Color.parseColor("#F5F6FA"));
                canvas1.drawRoundRect(new RectF(315, 260, 565, 510), 10, 10, paint);
                paint.setColor(Color.parseColor("#BDBDBD"));
                paint.setTextSize(11);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas1.drawText("No Trend History Available", 440, 385, paint);
                paint.setTextAlign(Paint.Align.LEFT); // Reset
            }

            // Information block at bottom of Page 1
            paint.setColor(Color.parseColor("#EBF3FA"));
            canvas1.drawRoundRect(new RectF(30, 570, 565, 770), 10, 10, paint);
            
            paint.setColor(Color.parseColor("#1565C0"));
            paint.setTextSize(12);
            paint.setFakeBoldText(true);
            canvas1.drawText("Report Insight & Summary", 45, 595, paint);
            
            paint.setColor(Color.parseColor("#1C1939"));
            paint.setFakeBoldText(false);
            paint.setTextSize(10);
            
            String summaryText1 = "This financial statement lists income, expense, and transfer records compiled from";
            String summaryText2 = "your local database sync for " + monthName + " (" + fiscalYear + "). The charts above display";
            String summaryText3 = "category breakdowns and saving rates. Page 2 outlines the full transaction ledger.";
            String summaryText4 = "Total transactions logged in this period: " + (transactions != null ? transactions.size() : 0);
            
            canvas1.drawText(summaryText1, 45, 625, paint);
            canvas1.drawText(summaryText2, 45, 645, paint);
            canvas1.drawText(summaryText3, 45, 665, paint);
            
            paint.setFakeBoldText(true);
            canvas1.drawText(summaryText4, 45, 700, paint);
            paint.setFakeBoldText(false);

            // Draw Footer Page 1
            paint.setColor(Color.parseColor("#BDBDBD"));
            paint.setTextSize(9);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas1.drawText("Page 1 of 2+", 297, 810, paint);
            paint.setTextAlign(Paint.Align.LEFT); // Reset

            document.finishPage(page1);

            // ==================== PAGE 2+: DETAILED TRANSACTIONS LEDGER ====================
            PdfDocument.PageInfo pageInfo2 = new PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create();
            PdfDocument.Page page2 = document.startPage(pageInfo2);
            Canvas canvas2 = page2.getCanvas();

            // Setup Header on Page 2
            drawLedgerHeader(canvas2, paint, monthName, fiscalYear);

            int startY = 130;
            int currentY = startY;
            int rowHeight = 24;

            if (transactions == null || transactions.isEmpty()) {
                paint.setColor(Color.parseColor("#757575"));
                paint.setTextSize(11);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas2.drawText("No transactions logged in this period.", 297, 200, paint);
                paint.setTextAlign(Paint.Align.LEFT); // Reset
                
                // Finish Page 2
                drawLedgerFooter(canvas2, paint, pageNumber - 1, pageNumber - 1);
                document.finishPage(page2);
            } else {
                PdfDocument.Page currentPage = page2;
                Canvas currentCanvas = canvas2;
                int totalTransactionsCount = transactions.size();

                for (int i = 0; i < totalTransactionsCount; i++) {
                    Transaction t = transactions.get(i);

                    // Row Zebra striping background tint
                    if (i % 2 == 1) {
                        paint.setColor(Color.parseColor("#F9F9FC"));
                        currentCanvas.drawRect(30, currentY - 15, 565, currentY + 9, paint);
                    }

                    // Format Date (yyyy-MM-dd -> MM-dd or keep short)
                    String date = t.date != null ? t.date : "";
                    if (date.length() > 5) {
                        // Keep only MM-dd or short string if it has format yyyy-MM-dd
                        try {
                            String[] parts = date.split("-");
                            if (parts.length >= 3) {
                                date = parts[1] + "-" + parts[2];
                            }
                        } catch (Exception ignored) {}
                    }

                    // Draw Table fields
                    paint.setColor(Color.parseColor("#1C1939"));
                    paint.setTextSize(9);
                    paint.setFakeBoldText(false);

                    // Date
                    currentCanvas.drawText(date, 35, currentY, paint);

                    // Category
                    String category = t.category != null ? t.category : "Generic";
                    if (category.length() > 14) category = category.substring(0, 12) + "..";
                    currentCanvas.drawText(category, 95, currentY, paint);

                    // Wallet
                    String wallet = t.wallet != null ? t.wallet : "Cash";
                    if (wallet.length() > 10) wallet = wallet.substring(0, 8) + "..";
                    currentCanvas.drawText(wallet, 185, currentY, paint);

                    // Description (Crop/Truncate if it spans long)
                    String description = t.description != null ? t.description : "";
                    if (description.length() > 28) description = description.substring(0, 25) + "...";
                    currentCanvas.drawText(description, 260, currentY, paint);

                    // Type Badge
                    String type = t.type != null ? t.type : "Expense";
                    currentCanvas.drawText(type, 435, currentY, paint);

                    // Amount with color
                    String amountPrefix = "-";
                    int amountColor = Color.parseColor("#C62828"); // Red
                    if ("Income".equalsIgnoreCase(type)) {
                        amountPrefix = "+";
                        amountColor = Color.parseColor("#2E7D32"); // Green
                    } else if ("Transfer".equalsIgnoreCase(type)) {
                        amountPrefix = "➜";
                        amountColor = Color.parseColor("#1565C0"); // Blue
                    }
                    
                    paint.setColor(amountColor);
                    paint.setFakeBoldText(true);
                    paint.setTextAlign(Paint.Align.RIGHT);
                    currentCanvas.drawText(String.format("%s ₹%,.2f", amountPrefix, t.amount), 555, currentY, paint);
                    paint.setTextAlign(Paint.Align.LEFT); // Reset

                    // Draw soft separator line under row
                    paint.setColor(Color.parseColor("#EEEEEE"));
                    paint.setStrokeWidth(0.5f);
                    currentCanvas.drawLine(30, currentY + 9, 565, currentY + 9, paint);
                    paint.setStrokeWidth(1.0f); // Reset

                    // Increment row coordinate
                    currentY += rowHeight;

                    // Dynamic Multi-Page logic: Check if we are near bottom of page
                    if (currentY > 770 && i < totalTransactionsCount - 1) {
                        // Finish current page
                        drawLedgerFooter(currentCanvas, paint, pageNumber - 1, 0); // 0 means temporary page number count
                        document.finishPage(currentPage);

                        // Start a new page
                        PdfDocument.PageInfo nextPageInfo = new PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create();
                        currentPage = document.startPage(nextPageInfo);
                        currentCanvas = currentPage.getCanvas();

                        // Draw ledger headers on new page
                        drawLedgerHeader(currentCanvas, paint, monthName, fiscalYear);
                        currentY = startY;
                    }
                }

                // Draw bottom border under final table row
                paint.setColor(Color.parseColor("#3F2B96"));
                paint.setStrokeWidth(1.5f);
                currentCanvas.drawLine(30, currentY - 14, 565, currentY - 14, paint);
                paint.setStrokeWidth(1.0f); // Reset

                // Finish final page
                drawLedgerFooter(currentCanvas, paint, pageNumber - 1, pageNumber - 1);
                document.finishPage(currentPage);
            }

            // Write output
            document.writeTo(outputStream);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error writing PDF Document", e);
            return false;
        } finally {
            document.close();
        }
    }

    private static void drawLedgerHeader(Canvas canvas, Paint paint, String monthName, String fiscalYear) {
        // App brand header line
        paint.setColor(Color.parseColor("#3F2B96"));
        canvas.drawRect(0, 0, 595, 45, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(14);
        paint.setFakeBoldText(true);
        canvas.drawText("HisabDo Transaction Statement", 30, 28, paint);

        paint.setTextSize(10);
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(monthName + " (FY " + fiscalYear + ")", 565, 28, paint);
        paint.setTextAlign(Paint.Align.LEFT); // Reset

        // Table Header Titles
        int headerY = 85;
        paint.setColor(Color.parseColor("#F5F6FA"));
        canvas.drawRoundRect(new RectF(30, headerY - 18, 565, headerY + 10), 4, 4, paint);

        paint.setColor(Color.parseColor("#1D193E"));
        paint.setTextSize(9);
        paint.setFakeBoldText(true);

        canvas.drawText("Date", 35, headerY, paint);
        canvas.drawText("Category", 95, headerY, paint);
        canvas.drawText("Wallet", 185, headerY, paint);
        canvas.drawText("Description", 260, headerY, paint);
        canvas.drawText("Type", 435, headerY, paint);
        
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("Amount", 555, headerY, paint);
        paint.setTextAlign(Paint.Align.LEFT); // Reset
        paint.setFakeBoldText(false);
    }

    private static void drawLedgerFooter(Canvas canvas, Paint paint, int currentPage, int totalPages) {
        paint.setColor(Color.parseColor("#BDBDBD"));
        paint.setTextSize(9);
        paint.setTextAlign(Paint.Align.CENTER);
        
        String pageText = "Page " + currentPage;
        if (totalPages > 0) {
            pageText += " of " + totalPages;
        }
        canvas.drawText(pageText, 297, 810, paint);
        paint.setTextAlign(Paint.Align.LEFT); // Reset
    }
}

