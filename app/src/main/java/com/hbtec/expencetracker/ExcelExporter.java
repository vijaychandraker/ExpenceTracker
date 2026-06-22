package com.hbtec.expencetracker;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExcelExporter {

    public static boolean export(List<Transaction> transactions, String fy, String monthName, OutputStream outputStream) {
        Workbook workbook = new XSSFWorkbook();
        try {
            Sheet sheet = workbook.createSheet("Transactions Report");

            // Enable gridlines display explicitly
            sheet.setDisplayGridlines(true);

            // Set Column Widths (Units are 1/256 of character width)
            sheet.setColumnWidth(0, 1000);   // Col A: Padding
            sheet.setColumnWidth(1, 2000);   // Col B: S.No.
            sheet.setColumnWidth(2, 4500);   // Col C: Date
            sheet.setColumnWidth(3, 3000);   // Col D: Type
            sheet.setColumnWidth(4, 7500);   // Col E: Category
            sheet.setColumnWidth(5, 4500);   // Col F: Wallet
            sheet.setColumnWidth(6, 5500);   // Col G: Amount (INR)
            sheet.setColumnWidth(7, 12000);  // Col H: Description

            // Predefined IndexedColors matching the app theme
            short colorPurple = IndexedColors.INDIGO.getIndex();            // Indigo/Purple for Header
            short colorTeal = IndexedColors.TEAL.getIndex();                // Teal for Income/Savings
            short colorCoral = IndexedColors.CORAL.getIndex();              // Coral/Rose for Expense/Negative
            short colorLightGray = IndexedColors.GREY_25_PERCENT.getIndex(); // Light grey for Cards
            short colorZebra = IndexedColors.LIGHT_TURQUOISE.getIndex();    // Soft cyan/blue for alternate rows

            // Generate Fonts
            Font fontTitle = workbook.createFont();
            fontTitle.setFontName("Segoe UI");
            fontTitle.setFontHeightInPoints((short) 16);
            fontTitle.setBold(true);
            fontTitle.setColor(IndexedColors.WHITE.getIndex());

            Font fontSubtitle = workbook.createFont();
            fontSubtitle.setFontName("Segoe UI");
            fontSubtitle.setFontHeightInPoints((short) 10);
            fontSubtitle.setItalic(true);
            fontSubtitle.setColor(IndexedColors.GREY_80_PERCENT.getIndex());

            Font fontCardHeader = workbook.createFont();
            fontCardHeader.setFontName("Segoe UI");
            fontCardHeader.setFontHeightInPoints((short) 9);
            fontCardHeader.setBold(true);
            fontCardHeader.setColor(IndexedColors.GREY_50_PERCENT.getIndex());

            Font fontCardValueTeal = workbook.createFont();
            fontCardValueTeal.setFontName("Segoe UI");
            fontCardValueTeal.setFontHeightInPoints((short) 13);
            fontCardValueTeal.setBold(true);
            fontCardValueTeal.setColor(colorTeal);

            Font fontCardValueCoral = workbook.createFont();
            fontCardValueCoral.setFontName("Segoe UI");
            fontCardValueCoral.setFontHeightInPoints((short) 13);
            fontCardValueCoral.setBold(true);
            fontCardValueCoral.setColor(colorCoral);

            Font fontTableHeader = workbook.createFont();
            fontTableHeader.setFontName("Segoe UI");
            fontTableHeader.setFontHeightInPoints((short) 11);
            fontTableHeader.setBold(true);
            fontTableHeader.setColor(IndexedColors.WHITE.getIndex());

            Font fontBold = workbook.createFont();
            fontBold.setFontName("Segoe UI");
            fontBold.setFontHeightInPoints((short) 11);
            fontBold.setBold(true);

            Font fontRegular = workbook.createFont();
            fontRegular.setFontName("Segoe UI");
            fontRegular.setFontHeightInPoints((short) 11);

            // ================== STYLE DEFINITIONS ==================

            // 1. Title Block Style
            CellStyle styleTitle = workbook.createCellStyle();
            styleTitle.setFont(fontTitle);
            styleTitle.setAlignment(HorizontalAlignment.CENTER);
            styleTitle.setVerticalAlignment(VerticalAlignment.CENTER);
            styleTitle.setFillForegroundColor(colorPurple);
            styleTitle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // 2. Subtitle Style
            CellStyle styleSubtitle = workbook.createCellStyle();
            styleSubtitle.setFont(fontSubtitle);
            styleSubtitle.setAlignment(HorizontalAlignment.CENTER);
            styleSubtitle.setVerticalAlignment(VerticalAlignment.CENTER);

            // 3. Card Header Style
            CellStyle styleCardHeader = workbook.createCellStyle();
            styleCardHeader.setFont(fontCardHeader);
            styleCardHeader.setAlignment(HorizontalAlignment.CENTER);
            styleCardHeader.setVerticalAlignment(VerticalAlignment.CENTER);
            styleCardHeader.setFillForegroundColor(colorLightGray);
            styleCardHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorders(styleCardHeader, BorderStyle.THIN, IndexedColors.GREY_40_PERCENT);

            // 4. Card Value Styles
            CellStyle styleCardValueTeal = workbook.createCellStyle();
            styleCardValueTeal.setFont(fontCardValueTeal);
            styleCardValueTeal.setAlignment(HorizontalAlignment.CENTER);
            styleCardValueTeal.setVerticalAlignment(VerticalAlignment.CENTER);
            styleCardValueTeal.setFillForegroundColor(colorLightGray);
            styleCardValueTeal.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorders(styleCardValueTeal, BorderStyle.THIN, IndexedColors.GREY_40_PERCENT);

            CellStyle styleCardValueCoral = workbook.createCellStyle();
            styleCardValueCoral.setFont(fontCardValueCoral);
            styleCardValueCoral.setAlignment(HorizontalAlignment.CENTER);
            styleCardValueCoral.setVerticalAlignment(VerticalAlignment.CENTER);
            styleCardValueCoral.setFillForegroundColor(colorLightGray);
            styleCardValueCoral.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorders(styleCardValueCoral, BorderStyle.THIN, IndexedColors.GREY_40_PERCENT);

            // 5. Table Header Style
            CellStyle styleTableHeader = workbook.createCellStyle();
            styleTableHeader.setFont(fontTableHeader);
            styleTableHeader.setAlignment(HorizontalAlignment.CENTER);
            styleTableHeader.setVerticalAlignment(VerticalAlignment.CENTER);
            styleTableHeader.setFillForegroundColor(colorPurple);
            styleTableHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorders(styleTableHeader, BorderStyle.THIN, IndexedColors.BLACK);

            // Data Styles (Regular & Alternate Zebra rows)
            CellStyle styleCellRegularLeft = workbook.createCellStyle();
            styleCellRegularLeft.setFont(fontRegular);
            styleCellRegularLeft.setAlignment(HorizontalAlignment.LEFT);
            styleCellRegularLeft.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(styleCellRegularLeft, BorderStyle.THIN, IndexedColors.GREY_25_PERCENT);

            CellStyle styleCellZebraLeft = workbook.createCellStyle();
            styleCellZebraLeft.setFont(fontRegular);
            styleCellZebraLeft.setAlignment(HorizontalAlignment.LEFT);
            styleCellZebraLeft.setVerticalAlignment(VerticalAlignment.CENTER);
            styleCellZebraLeft.setFillForegroundColor(colorZebra);
            styleCellZebraLeft.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorders(styleCellZebraLeft, BorderStyle.THIN, IndexedColors.GREY_25_PERCENT);

            // Numeric (Amount) Formats
            DataFormat dataFormat = workbook.createDataFormat();
            short currencyFormatId = dataFormat.getFormat("[$₹-3601]#,##0.00"); // Indian Rupee currency format

            CellStyle styleCellRegularAmount = workbook.createCellStyle();
            styleCellRegularAmount.setFont(fontRegular);
            styleCellRegularAmount.setAlignment(HorizontalAlignment.RIGHT);
            styleCellRegularAmount.setVerticalAlignment(VerticalAlignment.CENTER);
            styleCellRegularAmount.setDataFormat(currencyFormatId);
            setBorders(styleCellRegularAmount, BorderStyle.THIN, IndexedColors.GREY_25_PERCENT);

            CellStyle styleCellZebraAmount = workbook.createCellStyle();
            styleCellZebraAmount.setFont(fontRegular);
            styleCellZebraAmount.setAlignment(HorizontalAlignment.RIGHT);
            styleCellZebraAmount.setVerticalAlignment(VerticalAlignment.CENTER);
            styleCellZebraAmount.setFillForegroundColor(colorZebra);
            styleCellZebraAmount.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            styleCellZebraAmount.setDataFormat(currencyFormatId);
            setBorders(styleCellZebraAmount, BorderStyle.THIN, IndexedColors.GREY_25_PERCENT);

            // Center Align Styles (S.No, Date, Type)
            CellStyle styleCellRegularCenter = workbook.createCellStyle();
            styleCellRegularCenter.setFont(fontRegular);
            styleCellRegularCenter.setAlignment(HorizontalAlignment.CENTER);
            styleCellRegularCenter.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(styleCellRegularCenter, BorderStyle.THIN, IndexedColors.GREY_25_PERCENT);

            CellStyle styleCellZebraCenter = workbook.createCellStyle();
            styleCellZebraCenter.setFont(fontRegular);
            styleCellZebraCenter.setAlignment(HorizontalAlignment.CENTER);
            styleCellZebraCenter.setVerticalAlignment(VerticalAlignment.CENTER);
            styleCellZebraCenter.setFillForegroundColor(colorZebra);
            styleCellZebraCenter.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorders(styleCellZebraCenter, BorderStyle.THIN, IndexedColors.GREY_25_PERCENT);

            // Colors for Type tags inside data table
            Font fontTypeIncome = workbook.createFont();
            fontTypeIncome.setFontName("Segoe UI");
            fontTypeIncome.setFontHeightInPoints((short) 11);
            fontTypeIncome.setBold(true);
            fontTypeIncome.setColor(colorTeal);

            Font fontTypeExpense = workbook.createFont();
            fontTypeExpense.setFontName("Segoe UI");
            fontTypeExpense.setFontHeightInPoints((short) 11);
            fontTypeExpense.setBold(true);
            fontTypeExpense.setColor(colorCoral);

            CellStyle styleCellRegularIncome = workbook.createCellStyle();
            styleCellRegularIncome.cloneStyleFrom(styleCellRegularCenter);
            styleCellRegularIncome.setFont(fontTypeIncome);

            CellStyle styleCellZebraIncome = workbook.createCellStyle();
            styleCellZebraIncome.cloneStyleFrom(styleCellZebraCenter);
            styleCellZebraIncome.setFont(fontTypeIncome);

            CellStyle styleCellRegularExpense = workbook.createCellStyle();
            styleCellRegularExpense.cloneStyleFrom(styleCellRegularCenter);
            styleCellRegularExpense.setFont(fontTypeExpense);

            CellStyle styleCellZebraExpense = workbook.createCellStyle();
            styleCellZebraExpense.cloneStyleFrom(styleCellZebraCenter);
            styleCellZebraExpense.setFont(fontTypeExpense);

            // Table Footer (Total Row) Styles
            CellStyle styleFooterLabel = workbook.createCellStyle();
            styleFooterLabel.setFont(fontBold);
            styleFooterLabel.setAlignment(HorizontalAlignment.RIGHT);
            styleFooterLabel.setVerticalAlignment(VerticalAlignment.CENTER);
            setFooterBorders(styleFooterLabel);

            CellStyle styleFooterAmount = workbook.createCellStyle();
            styleFooterAmount.setFont(fontBold);
            styleFooterAmount.setAlignment(HorizontalAlignment.RIGHT);
            styleFooterAmount.setVerticalAlignment(VerticalAlignment.CENTER);
            styleFooterAmount.setDataFormat(currencyFormatId);
            setFooterBorders(styleFooterAmount);

            // ================== DATA PROCESSING & CALCULATIONS ==================
            double totalIncome = 0;
            double totalExpense = 0;
            for (Transaction transaction : transactions) {
                if ("Income".equalsIgnoreCase(transaction.type)) {
                    totalIncome += transaction.amount;
                } else if ("Expense".equalsIgnoreCase(transaction.type)) {
                    totalExpense += transaction.amount;
                }
            }
            double netBalance = totalIncome - totalExpense;

            // ================== LAYOUT BUILDER ==================

            // Row 1: Title Block
            Row rowTitle = sheet.createRow(1);
            rowTitle.setHeightInPoints(40);
            for (int col = 1; col <= 7; col++) {
                Cell cell = rowTitle.createCell(col);
                if (col == 1) {
                    cell.setCellValue("EXPENSE TRACKER - MONTHLY REPORT");
                }
                cell.setCellStyle(styleTitle);
            }
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 7));

            // Row 2: Subtitle Period Block
            Row rowSubtitle = sheet.createRow(2);
            rowSubtitle.setHeightInPoints(20);
            for (int col = 1; col <= 7; col++) {
                Cell cell = rowSubtitle.createCell(col);
                if (col == 1) {
                    cell.setCellValue("Financial Year: " + fy + "   |   Period: " + monthName);
                }
                cell.setCellStyle(styleSubtitle);
            }
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 1, 7));

            // Row 4: Summary Card Headers
            Row rowCardHeader = sheet.createRow(4);
            rowCardHeader.setHeightInPoints(18);
            for (int col = 1; col <= 7; col++) {
                Cell cell = rowCardHeader.createCell(col);
                cell.setCellStyle(styleCardHeader);
            }
            rowCardHeader.getCell(1).setCellValue("TOTAL INCOME");
            rowCardHeader.getCell(3).setCellValue("TOTAL EXPENSES");
            rowCardHeader.getCell(5).setCellValue("NET BALANCE");
            sheet.addMergedRegion(new CellRangeAddress(4, 4, 1, 2));
            sheet.addMergedRegion(new CellRangeAddress(4, 4, 3, 4));
            sheet.addMergedRegion(new CellRangeAddress(4, 4, 5, 7));

            // Row 5: Summary Card Values
            Row rowCardValue = sheet.createRow(5);
            rowCardValue.setHeightInPoints(28);
            for (int col = 1; col <= 7; col++) {
                Cell cell = rowCardValue.createCell(col);
                if (col <= 2) {
                    cell.setCellStyle(styleCardValueTeal);
                } else if (col <= 4) {
                    cell.setCellStyle(styleCardValueCoral);
                } else {
                    cell.setCellStyle(netBalance >= 0 ? styleCardValueTeal : styleCardValueCoral);
                }
            }
            rowCardValue.getCell(1).setCellValue("₹" + String.format(Locale.getDefault(), "%,.2f", totalIncome));
            rowCardValue.getCell(3).setCellValue("₹" + String.format(Locale.getDefault(), "%,.2f", totalExpense));
            rowCardValue.getCell(5).setCellValue("₹" + String.format(Locale.getDefault(), "%,.2f", netBalance));
            sheet.addMergedRegion(new CellRangeAddress(5, 5, 1, 2));
            sheet.addMergedRegion(new CellRangeAddress(5, 5, 3, 4));
            sheet.addMergedRegion(new CellRangeAddress(5, 5, 5, 7));

            // Row 7: Table Header
            Row rowHeader = sheet.createRow(7);
            rowHeader.setHeightInPoints(24);
            String[] headers = {"S.No.", "Date", "Type", "Category", "Wallet", "Amount", "Description"};
            for (int col = 0; col < headers.length; col++) {
                Cell cell = rowHeader.createCell(col + 1);
                cell.setCellValue(headers[col]);
                cell.setCellStyle(styleTableHeader);
            }

            // Row 8 onwards: Transaction Rows
            int rowIndex = 8;
            int serialNo = 1;
            for (Transaction t : transactions) {
                Row row = sheet.createRow(rowIndex);
                row.setHeightInPoints(20);

                boolean isZebra = (rowIndex % 2 == 1);
                CellStyle activeLeft = isZebra ? styleCellZebraLeft : styleCellRegularLeft;
                CellStyle activeCenter = isZebra ? styleCellZebraCenter : styleCellRegularCenter;
                CellStyle activeAmount = isZebra ? styleCellZebraAmount : styleCellRegularAmount;

                // 1. S.No.
                Cell cellSNo = row.createCell(1);
                cellSNo.setCellValue(serialNo++);
                cellSNo.setCellStyle(activeCenter);

                // 2. Date
                Cell cellDate = row.createCell(2);
                cellDate.setCellValue(formatReportDate(t.date));
                cellDate.setCellStyle(activeCenter);

                // 3. Type
                Cell cellType = row.createCell(3);
                cellType.setCellValue(t.type);
                if ("Income".equalsIgnoreCase(t.type)) {
                    cellType.setCellStyle(isZebra ? styleCellZebraIncome : styleCellRegularIncome);
                } else {
                    cellType.setCellStyle(isZebra ? styleCellZebraExpense : styleCellRegularExpense);
                }

                // 4. Category
                Cell cellCategory = row.createCell(4);
                cellCategory.setCellValue(t.category);
                cellCategory.setCellStyle(activeLeft);

                // 5. Wallet
                Cell cellWallet = row.createCell(5);
                cellWallet.setCellValue(t.wallet != null ? t.wallet : "Cash");
                cellWallet.setCellStyle(activeCenter);

                // 6. Amount
                Cell cellAmount = row.createCell(6);
                cellAmount.setCellValue(t.amount);
                cellAmount.setCellStyle(activeAmount);

                // 7. Description
                Cell cellDesc = row.createCell(7);
                cellDesc.setCellValue(t.description != null ? t.description : "");
                cellDesc.setCellStyle(activeLeft);

                rowIndex++;
            }

            // Table Footer: Totals Row
            Row rowFooter = sheet.createRow(rowIndex);
            rowFooter.setHeightInPoints(22);
            for (int col = 1; col <= 7; col++) {
                Cell cell = rowFooter.createCell(col);
                cell.setCellStyle(styleFooterLabel);
            }
            rowFooter.getCell(1).setCellValue("Total (Difference/Net Balance):");
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 1, 5));

            Cell cellFooterAmt = rowFooter.getCell(6);
            cellFooterAmt.setCellValue(netBalance);
            cellFooterAmt.setCellStyle(styleFooterAmount);

            // Write workbook to stream
            workbook.write(outputStream);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void setBorders(CellStyle style, BorderStyle borderStyle, IndexedColors color) {
        style.setBorderTop(borderStyle);
        style.setBorderBottom(borderStyle);
        style.setBorderLeft(borderStyle);
        style.setBorderRight(borderStyle);
        style.setTopBorderColor(color.getIndex());
        style.setBottomBorderColor(color.getIndex());
        style.setLeftBorderColor(color.getIndex());
        style.setRightBorderColor(color.getIndex());
    }

    private static void setFooterBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.DOUBLE); // Double border bottom for accounting total style
        style.setTopBorderColor(IndexedColors.GREY_80_PERCENT.getIndex());
        style.setBottomBorderColor(IndexedColors.GREY_80_PERCENT.getIndex());
    }

    private static String formatReportDate(String dateStr) {
        try {
            SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdfInput.parse(dateStr);
            SimpleDateFormat sdfOutput = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            return sdfOutput.format(date);
        } catch (ParseException e) {
            return dateStr;
        }
    }
}

