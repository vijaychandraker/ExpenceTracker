package com.hbtec.expencetracker;

import static androidx.activity.result.ActivityResultCallerKt.registerForActivityResult;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import android.text.Editable;
import android.text.TextWatcher;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.net.Uri;
import java.io.OutputStream;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import android.content.Intent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private TransactionDao transactionDao;
    private TransactionAdapter adapter;
    private TransactionAdapter adapterHome;
    private TextView tvTotalIncome, tvTotalExpense, tvBalance;
    private PieChart pieChart;
    private PieChart homePieChart;
    private com.github.mikephil.charting.charts.LineChart lineChart;
    private TextView tvDashboardMonth;
    private HorizontalScrollView hsvBudgetRings;
    private LinearLayout llBudgetRings;
    private Spinner spinnerFY, spinnerMonth;
    private String selectedSort = "Date: Newest First";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private GoogleSignInClient mGoogleSignInClient;

    // Tab Layouts
    private View layoutHome, layoutTransactions, layoutReports, layoutProfile;
    private View llTabHome, llTabTransactions, llTabReports, llTabProfile;
    private ImageView ivTabHome, ivTabTransactions, ivTabReports, ivTabProfile;
    private TextView tvTabHome, tvTabTransactions, tvTabReports, tvTabProfile;
    private TextView tvToolbarTitle;
    private View llFilters, rlHeader;

    // Profile Stats
    private TextView tvProfileSavings, tvProfileTotalCount;

    // Google Sign-In UI controls
    private Button btnGoogleSignIn;
    private Button btnGoogleSignOut;
    private Button btnLoginOverlay;
    private Button btnSkipLogin;

    // PIN Passcode Lock UI
    private View layoutLock;
    private TextView tvPinDisplay;
    private String pinInput = "";

    // Monthly Budget UI
    private View cardBudget;
    private TextView tvBudgetStatus, tvBudgetMessage;
    private ProgressBar pbBudget;

    // Wallets Balance UI
    private TextView tvWalletCash, tvWalletBank, tvWalletCard;

    // Search and Filters UI
    private EditText etSearch;
    private Button btnChipAll, btnChipIncome, btnChipExpense;
    private Button btnChipCash, btnChipBank, btnChipCard;
    private String searchQuery = "";
    private String filterType = "All";
    private String filterWallet = "All";
    private List<Transaction> allTransactionsList = new ArrayList<>();

    // MoM Bar Chart
    private com.github.mikephil.charting.charts.BarChart barChart;

    private final String[] incomeCategories = {
            "Salary (job)", "Wages (daily/hourly)", "Freelancing / consulting",
            "Business active work income", "Rental income (house/shop)",
            "Royalties (books, music, courses)", "YouTube / blogging revenue",
            "Affiliate income", "Interest (FD, savings account)", "Dividends (stocks)",
            "Capital gains (shares, mutual funds)", "Shop profit",
            "Online business (e-commerce)", "Services business", "Bonus (job)",
            "Gifts / inheritance", "Lottery winnings", "Tax refunds"
    };

    private final String[] expenseCategories = {
            "Home Rent", "Property tax", "Electricity", "LPG",
            "Maintenance / society charges", "Repairs & upkeep", "Groceries",
            "Cooking essentials", "Eating out / food delivery", "Fuel",
            "Public transport (bus/train)", "Cab services (Ola/Uber)",
            "Vehicle maintenance & insurance", "Mobile recharge", "Internet/WiFi",
            "DTH / OTT subscriptions", "Electricity/gas bills", "Doctor visits",
            "Medicines", "Health insurance", "Emergency treatment",
            "School/college fees", "Books & stationery", "Coaching / online courses",
            "Clothing", "Grooming (salon, cosmetics)", "Fitness / gym",
            "Movies / outings", "Travel / vacations", "Hobbies / games",
            "Life insurance", "Investments (SIP, FD)", "Emergency savings",
            "Gifts / donations", "Festivals", "Unexpected costs", "EMI"
    };

    // FY/Month filter state
    private String selectedFY = "";     // e.g. "2025-26"
    private int selectedMonthIndex = 0; // 0 = "All", 1 = April, 2 = May, ..., 12 = March

    // Month names in FY order (April to March)
    private final String[] fyMonthNames = {
            "All Months", "April", "May", "June", "July", "August",
            "September", "October", "November", "December", "January", "February", "March"
    };

    // Current observers — need to remove before re-observing
    private LiveData<List<Transaction>> currentTransactionsLiveData;
    private LiveData<Double> currentIncomeLiveData;
    private LiveData<Double> currentExpenseLiveData;
    private LiveData<List<TransactionDao.CategorySummary>> currentCategoryLiveData;

    private double currentIncome = 0;
    private double currentExpense = 0;

    private final ActivityResultLauncher<String> exportExcelLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            uri -> {
                if (uri != null) {
                    performExport(uri);
                }
            }
    );

    private final ActivityResultLauncher<String> exportPdfLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/pdf"),
            uri -> {
                if (uri != null) {
                    performPdfExport(uri);
                }
            }
    );

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                boolean isAuthenticating = false;
                Intent data = result.getData();
                if (data != null) {
                    com.google.android.gms.tasks.Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null && account.getIdToken() != null) {
                            firebaseAuthWithGoogle(account.getIdToken());
                            isAuthenticating = true;
                        }
                    } catch (ApiException e) {
                        int statusCode = e.getStatusCode();
                        if (statusCode == com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                            Toast.makeText(this, "Sign-In cancelled", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Google Sign-In failed: " + e.getMessage() + " (Status Code: " + statusCode + ")", Toast.LENGTH_LONG).show();
                        }
                    }
                } else {
                    Toast.makeText(this, "Sign-In cancelled", Toast.LENGTH_SHORT).show();
                }
                if (!isAuthenticating) {
                    showSignInProgress(false);
                }
            }
    );

    // Pie chart colors — harmonious, softer palette
    private final int[] PIE_COLORS = {
            Color.parseColor("#6C5CE7"),  // Purple
            Color.parseColor("#74B9FF"),  // Sky blue
            Color.parseColor("#FF6B6B"),  // Coral
            Color.parseColor("#FDCB6E"),  // Gold
            Color.parseColor("#55EFC4"),  // Mint
            Color.parseColor("#E17055"),  // Terracotta
            Color.parseColor("#81ECEC"),  // Aqua
            Color.parseColor("#A29BFE"),  // Lavender
            Color.parseColor("#FAB1A0"),  // Peach
            Color.parseColor("#00B894"),  // Emerald
            Color.parseColor("#FD79A8"),  // Pink
            Color.parseColor("#0984E3"),  // Ocean blue
            Color.parseColor("#E84393"),  // Magenta
            Color.parseColor("#00CEC9"),  // Teal
            Color.parseColor("#DFE6E9"),  // Light gray
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load Selected Theme from SharedPreferences
        android.content.SharedPreferences prefs = getSharedPreferences("ExpenseTrackerPrefs", MODE_PRIVATE);
        String selectedTheme = prefs.getString("selected_theme", "royal_violet");
        int themeResId = R.style.Theme_ExpenceTracker_RoyalViolet;
        if ("ocean_breeze".equals(selectedTheme)) {
            themeResId = R.style.Theme_ExpenceTracker_OceanBreeze;
        } else if ("midnight_charcoal".equals(selectedTheme)) {
            themeResId = R.style.Theme_ExpenceTracker_MidnightCharcoal;
        } else if ("sunset_gold".equals(selectedTheme)) {
            themeResId = R.style.Theme_ExpenceTracker_SunsetGold;
        }
        setTheme(themeResId);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        tvBalance = findViewById(R.id.tvBalance);
        pieChart = findViewById(R.id.pieChart);
        homePieChart = findViewById(R.id.homePieChart);
        tvDashboardMonth = findViewById(R.id.tvDashboardMonth);
        hsvBudgetRings = findViewById(R.id.hsvBudgetRings);
        llBudgetRings = findViewById(R.id.llBudgetRings);
        spinnerFY = findViewById(R.id.spinnerFY);
        spinnerMonth = findViewById(R.id.spinnerMonth);

        // Bind layout views
        layoutHome = findViewById(R.id.layoutHome);
        layoutTransactions = findViewById(R.id.layoutTransactions);
        layoutReports = findViewById(R.id.layoutReports);
        layoutProfile = findViewById(R.id.layoutProfile);
        llTabHome = findViewById(R.id.llTabHome);
        llTabTransactions = findViewById(R.id.llTabTransactions);
        llTabReports = findViewById(R.id.llTabReports);
        llTabProfile = findViewById(R.id.llTabProfile);
        ivTabHome = findViewById(R.id.ivTabHome);
        ivTabTransactions = findViewById(R.id.ivTabTransactions);
        ivTabReports = findViewById(R.id.ivTabReports);
        ivTabProfile = findViewById(R.id.ivTabProfile);
        tvTabHome = findViewById(R.id.tvTabHome);
        tvTabTransactions = findViewById(R.id.tvTabTransactions);
        tvTabReports = findViewById(R.id.tvTabReports);
        tvTabProfile = findViewById(R.id.tvTabProfile);
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        llFilters = findViewById(R.id.llFilters);
        rlHeader = findViewById(R.id.rlHeader);
        tvProfileSavings = findViewById(R.id.tvProfileSavings);
        tvProfileTotalCount = findViewById(R.id.tvProfileTotalCount);

        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        btnGoogleSignOut = findViewById(R.id.btnGoogleSignOut);
        btnLoginOverlay = findViewById(R.id.btnLoginOverlay);
        btnSkipLogin = findViewById(R.id.btnSkipLogin);

        // New Premium UI Initializations
        layoutLock = findViewById(R.id.layoutLock);
        tvPinDisplay = findViewById(R.id.tvPinDisplay);
        cardBudget = findViewById(R.id.cardBudget);
        tvBudgetStatus = findViewById(R.id.tvBudgetStatus);
        tvBudgetMessage = findViewById(R.id.tvBudgetMessage);
        pbBudget = findViewById(R.id.pbBudget);
        tvWalletCash = findViewById(R.id.tvWalletCash);
        tvWalletBank = findViewById(R.id.tvWalletBank);
        tvWalletCard = findViewById(R.id.tvWalletCard);
        etSearch = findViewById(R.id.etSearch);
        btnChipAll = findViewById(R.id.btnChipAll);
        btnChipIncome = findViewById(R.id.btnChipIncome);
        btnChipExpense = findViewById(R.id.btnChipExpense);
        btnChipCash = findViewById(R.id.btnChipCash);
        btnChipBank = findViewById(R.id.btnChipBank);
        btnChipCard = findViewById(R.id.btnChipCard);
        barChart = findViewById(R.id.barChart);
        lineChart = findViewById(R.id.lineChart);

        View ibDashboardSearch = findViewById(R.id.ibDashboardSearch);
        if (ibDashboardSearch != null) {
            ibDashboardSearch.setOnClickListener(v -> {
                switchTab(1); // Switch to Transactions
                if (etSearch != null) {
                    etSearch.requestFocus();
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(etSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                    }
                }
            });
        }

        View ibDashboardNotification = findViewById(R.id.ibDashboardNotification);
        if (ibDashboardNotification != null) {
            ibDashboardNotification.setOnClickListener(v ->
                    Toast.makeText(MainActivity.this, "No new notifications", Toast.LENGTH_SHORT).show()
            );
        }

        setupPieChart();
        setupHomePieChart();
        setupLineChart();
        updateDashboardMonth();

        android.widget.ImageButton ibSort = findViewById(R.id.ibSort);
        if (ibSort != null) {
            ibSort.setOnClickListener(v -> {
                androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(MainActivity.this, ibSort);
                popup.getMenu().add("Date: Newest First");
                popup.getMenu().add("Date: Oldest First");
                popup.getMenu().add("Amount: High to Low");
                popup.getMenu().add("Amount: Low to High");
                popup.setOnMenuItemClickListener(item -> {
                    selectedSort = item.getTitle().toString();
                    applySearchAndFilters();
                    Toast.makeText(MainActivity.this, "Sorted by " + selectedSort, Toast.LENGTH_SHORT).show();
                    return true;
                });
                popup.show();
            });
        }

        // Bind Transactions recycler
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(this::handleGroupClick, true);
        recyclerView.setAdapter(adapter);

        // Bind Home recycler
        RecyclerView recyclerViewHome = findViewById(R.id.recyclerViewHome);
        recyclerViewHome.setLayoutManager(new LinearLayoutManager(this));
        adapterHome = new TransactionAdapter(this::handleGroupClick, false);
        recyclerViewHome.setAdapter(adapterHome);

        AppDatabase db = AppDatabase.getInstance(this);
        transactionDao = db.transactionDao();

        prefs = getSharedPreferences("ExpenseTrackerPrefs", MODE_PRIVATE);
        selectedFY = prefs.getString("selected_fy", getCurrentFY());
        selectedMonthIndex = prefs.getInt("selected_month_index", getCurrentMonthInFY());

        setupFYSpinner();
        setupMonthSpinner();

        // Setup bottom nav listeners
        llTabHome.setOnClickListener(v -> switchTab(0));
        llTabTransactions.setOnClickListener(v -> switchTab(1));
        llTabReports.setOnClickListener(v -> switchTab(2));
        llTabProfile.setOnClickListener(v -> switchTab(3));

        // Notification Click
        View ibNotification = findViewById(R.id.ibNotification);
        if (ibNotification != null) {
            ibNotification.setOnClickListener(v ->
                    Toast.makeText(MainActivity.this, "No new notifications", Toast.LENGTH_SHORT).show()
            );
        }



        // Set default selected tab
        switchTab(0);

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> showTransactionDialog(null));

        View btnExport = findViewById(R.id.btnExport);
        if (btnExport != null) {
            btnExport.setOnClickListener(v -> {
                android.widget.PopupMenu popup = new android.widget.PopupMenu(this, btnExport);
                popup.getMenu().add("Export as Excel Spreadsheet (.xlsx)");
                popup.getMenu().add("Export as PDF Statement (.pdf)");
                popup.setOnMenuItemClickListener(item -> {
                    String title = item.getTitle().toString();
                    String monthName = fyMonthNames[selectedMonthIndex].replace(" ", "_");
                    if (title.contains("Excel")) {
                        String defaultFilename = "Expense_Report_" + selectedFY + "_" + monthName + ".xlsx";
                        exportExcelLauncher.launch(defaultFilename);
                    } else if (title.contains("PDF")) {
                        String defaultFilename = "Expense_Report_" + selectedFY + "_" + monthName + ".pdf";
                        exportPdfLauncher.launch(defaultFilename);
                    }
                    return true;
                });
                popup.show();
            });
        }

        View tvViewAll = findViewById(R.id.tvViewAll);
        if (tvViewAll != null) {
            tvViewAll.setOnClickListener(v -> switchTab(1));
        }

        applyFilters();
        setupGoogleSignIn();
        setupFirebaseSync();
        setupLoginOverlay();

        setupPasscodeLock();
        setupSearchAndFilters();
        setupProfilePreferences();
        setupBarChart();
        checkAndRunRecurringTransactions();
        normalizeDatabaseDates();
    }

    private void normalizeDatabaseDates() {
        executorService.execute(() -> {
            try {
                List<Transaction> allTxs = transactionDao.getAllTransactionsSync();
                if (allTxs != null) {
                    boolean updatedAny = false;
                    for (Transaction t : allTxs) {
                        if (t.date != null) {
                            String norm = Transaction.normalizeDate(t.date);
                            if (!t.date.equals(norm)) {
                                t.date = norm;
                                t.fy = Transaction.getFYFromDate(norm);
                                t.month = Transaction.getMonthNameFromDate(norm);
                                transactionDao.update(t);
                                updatedAny = true;
                            }
                        }
                    }
                    if (updatedAny) {
                        runOnUiThread(() -> {
                            applyFilters();
                            updateWalletBalances();
                            updateBudgetProgress();
                            updateBarChart();
                        });
                    }
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Error normalizing dates on startup", e);
            }
        });
    }

    private void switchTab(int tabIndex) {
        int inactiveColor = ContextCompat.getColor(this, R.color.nav_inactive);
        int activeColor = ContextCompat.getColor(this, R.color.nav_active);

        ivTabHome.setImageTintList(android.content.res.ColorStateList.valueOf(inactiveColor));
        tvTabHome.setTextColor(inactiveColor);
        tvTabHome.setTypeface(null, android.graphics.Typeface.NORMAL);

        ivTabTransactions.setImageTintList(android.content.res.ColorStateList.valueOf(inactiveColor));
        tvTabTransactions.setTextColor(inactiveColor);
        tvTabTransactions.setTypeface(null, android.graphics.Typeface.NORMAL);

        ivTabReports.setImageTintList(android.content.res.ColorStateList.valueOf(inactiveColor));
        tvTabReports.setTextColor(inactiveColor);
        tvTabReports.setTypeface(null, android.graphics.Typeface.NORMAL);

        ivTabProfile.setImageTintList(android.content.res.ColorStateList.valueOf(inactiveColor));
        tvTabProfile.setTextColor(inactiveColor);
        tvTabProfile.setTypeface(null, android.graphics.Typeface.NORMAL);

        // Hide layouts
        layoutHome.setVisibility(View.GONE);
        layoutTransactions.setVisibility(View.GONE);
        layoutReports.setVisibility(View.GONE);
        layoutProfile.setVisibility(View.GONE);
        llFilters.setVisibility(View.GONE);

        // Show active tab
        if (tabIndex == 0) {
            if (rlHeader != null) rlHeader.setVisibility(View.GONE);
            ivTabHome.setImageTintList(android.content.res.ColorStateList.valueOf(activeColor));
            tvTabHome.setTextColor(activeColor);
            tvTabHome.setTypeface(null, android.graphics.Typeface.BOLD);
            layoutHome.setVisibility(View.VISIBLE);
            tvToolbarTitle.setText("HisabDo");
        } else if (tabIndex == 1) {
            if (rlHeader != null) rlHeader.setVisibility(View.VISIBLE);
            ivTabTransactions.setImageTintList(android.content.res.ColorStateList.valueOf(activeColor));
            tvTabTransactions.setTextColor(activeColor);
            tvTabTransactions.setTypeface(null, android.graphics.Typeface.BOLD);
            layoutTransactions.setVisibility(View.VISIBLE);
            llFilters.setVisibility(View.VISIBLE);
            tvToolbarTitle.setText("Transactions");
        } else if (tabIndex == 2) {
            if (rlHeader != null) rlHeader.setVisibility(View.VISIBLE);
            ivTabReports.setImageTintList(android.content.res.ColorStateList.valueOf(activeColor));
            tvTabReports.setTextColor(activeColor);
            tvTabReports.setTypeface(null, android.graphics.Typeface.BOLD);
            layoutReports.setVisibility(View.VISIBLE);
            llFilters.setVisibility(View.VISIBLE);
            findViewById(R.id.btnExport).setVisibility(View.GONE);
            tvToolbarTitle.setText("Reports");
        } else if (tabIndex == 3) {
            if (rlHeader != null) rlHeader.setVisibility(View.VISIBLE);
            ivTabProfile.setImageTintList(android.content.res.ColorStateList.valueOf(activeColor));
            tvTabProfile.setTextColor(activeColor);
            tvTabProfile.setTypeface(null, android.graphics.Typeface.BOLD);
            layoutProfile.setVisibility(View.VISIBLE);
            tvToolbarTitle.setText("Profile");
            updateProfileStats();
        }

        if (tabIndex == 1) {
            findViewById(R.id.btnExport).setVisibility(View.VISIBLE);
        }
    }

    private void updateProfileStats() {
        double savings = currentIncome - currentExpense;
        tvProfileSavings.setText("₹" + String.format(Locale.getDefault(), "%,.0f", savings));
        if (savings >= 0) {
            tvProfileSavings.setTextColor(ContextCompat.getColor(this, R.color.income_green));
        } else {
            tvProfileSavings.setTextColor(ContextCompat.getColor(this, R.color.expense_red));
        }
        int count = adapter != null ? adapter.getItemCount() : 0;
        tvProfileTotalCount.setText(String.valueOf(count));
    }

    // ================ FY & MONTH SPINNERS ================

    private void setupFYSpinner() {
        List<String> fyList = generateFYList();
        ArrayAdapter<String> fyAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, fyList);
        fyAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerFY.setAdapter(fyAdapter);

        int defaultIndex = fyList.indexOf(selectedFY);
        if (defaultIndex >= 0) {
            spinnerFY.setSelection(defaultIndex);
        }

        spinnerFY.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedFY = fyList.get(position);
                getSharedPreferences("ExpenseTrackerPrefs", MODE_PRIVATE)
                        .edit()
                        .putString("selected_fy", selectedFY)
                        .apply();
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupMonthSpinner() {
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, fyMonthNames);
        monthAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerMonth.setAdapter(monthAdapter);

        spinnerMonth.setSelection(selectedMonthIndex);

        spinnerMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMonthIndex = position;
                getSharedPreferences("ExpenseTrackerPrefs", MODE_PRIVATE)
                        .edit()
                        .putInt("selected_month_index", selectedMonthIndex)
                        .apply();
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /**
     * Generate FY list from 2020-21 to current FY + 1
     */
    private List<String> generateFYList() {
        List<String> list = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        int currentMonth = cal.get(Calendar.MONTH); // 0-based

        // If before April, current FY started last year
        int currentFYStartYear = currentMonth < 3 ? currentYear - 1 : currentYear;

        for (int startYear = currentFYStartYear; startYear >= 2020; startYear--) {
            int endYear = startYear + 1;
            String fy = startYear + "-" + String.format(Locale.getDefault(), "%02d", endYear % 100);
            list.add(fy);
        }
        return list;
    }

    /**
     * Get current FY string e.g. "2025-26"
     */
    private String getCurrentFY() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH); // 0-based, Jan=0

        int startYear = month < 3 ? year - 1 : year;
        int endYear = startYear + 1;
        return startYear + "-" + String.format(Locale.getDefault(), "%02d", endYear % 100);
    }

    /**
     * Get current month's position in FY spinner (1-based: April=1, ..., March=12)
     * Returns 0 for "All Months"
     */
    private int getCurrentMonthInFY() {
        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH); // 0-based, Jan=0
        // Convert: April(3)=1, May(4)=2, ..., Dec(11)=9, Jan(0)=10, Feb(1)=11, Mar(2)=12
        if (month >= 3) {
            return month - 2; // April=1, May=2, ..., Dec=9
        } else {
            return month + 10; // Jan=10, Feb=11, Mar=12
        }
    }

    /**
     * Parse FY string "2025-26" and selected month index into start/end date strings.
     * Returns String[2]: {startDate, endDate} in "yyyy-MM-dd" format.
     */
    private String[] getDateRange() {
        // Parse FY start year
        int fyStartYear;
        try {
            fyStartYear = Integer.parseInt(selectedFY.split("-")[0]);
        } catch (Exception e) {
            // Fallback
            fyStartYear = Calendar.getInstance().get(Calendar.YEAR);
        }

        if (selectedMonthIndex == 0) {
            // All months in FY: April of startYear to March of startYear+1
            String startDate = fyStartYear + "-04-01";
            String endDate = (fyStartYear + 1) + "-03-31";
            return new String[]{startDate, endDate};
        } else {
            // Specific month
            // selectedMonthIndex: 1=April, 2=May, ..., 9=December, 10=January, 11=February, 12=March
            int calendarMonth; // 1-based for formatting
            int year;

            if (selectedMonthIndex <= 9) {
                // April(1) to December(9) → month 4 to 12 of startYear
                calendarMonth = selectedMonthIndex + 3;
                year = fyStartYear;
            } else {
                // January(10) to March(12) → month 1 to 3 of startYear+1
                calendarMonth = selectedMonthIndex - 9;
                year = fyStartYear + 1;
            }

            String startDate = String.format(Locale.getDefault(), "%d-%02d-01", year, calendarMonth);

            // Get last day of month
            Calendar cal = Calendar.getInstance();
            cal.set(year, calendarMonth - 1, 1); // Calendar.MONTH is 0-based
            int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            String endDate = String.format(Locale.getDefault(), "%d-%02d-%02d", year, calendarMonth, lastDay);

            return new String[]{startDate, endDate};
        }
    }

    // ================ APPLY FILTERS ================

    private void applyFilters() {
        if (selectedFY == null || selectedFY.isEmpty()) return;

        String[] dateRange = getDateRange();
        String startDate = dateRange[0];
        String endDate = dateRange[1];

        // Remove old observers
        removeOldObservers();

        // Observe filtered data
        currentTransactionsLiveData = transactionDao.getTransactionsByDateRange(startDate, endDate);
        currentTransactionsLiveData.observe(this, transactions -> {
            allTransactionsList = transactions != null ? transactions : new ArrayList<>();
            applySearchAndFilters();
            updateWalletBalances();
            updateBudgetProgress();
            updateBarChart();
        });

        currentIncomeLiveData = transactionDao.getTotalIncomeByDateRange(startDate, endDate);
        currentIncomeLiveData.observe(this, income -> {
            currentIncome = income != null ? income : 0.0;
            tvTotalIncome.setText("₹" + String.format(Locale.getDefault(), "%,.0f", currentIncome));
            updateBalanceDisplay();
            updateBudgetProgress();
            // Refresh pie chart when income changes (because % is based on income)
            if (currentCategoryLiveData != null && currentCategoryLiveData.getValue() != null) {
                updatePieChart(currentCategoryLiveData.getValue());
            }
        });

        currentExpenseLiveData = transactionDao.getTotalExpenseByDateRange(startDate, endDate);
        currentExpenseLiveData.observe(this, expense -> {
            currentExpense = expense != null ? expense : 0.0;
            tvTotalExpense.setText("₹" + String.format(Locale.getDefault(), "%,.0f", currentExpense));
            updateBalanceDisplay();
            updateBudgetProgress();
        });

        currentCategoryLiveData = transactionDao.getExpenseByCategoryByDateRange(startDate, endDate);
        currentCategoryLiveData.observe(this, summaries -> {
            updatePieChart(summaries);
        });
    }

    private void removeOldObservers() {
        if (currentTransactionsLiveData != null) {
            currentTransactionsLiveData.removeObservers(this);
        }
        if (currentIncomeLiveData != null) {
            currentIncomeLiveData.removeObservers(this);
        }
        if (currentExpenseLiveData != null) {
            currentExpenseLiveData.removeObservers(this);
        }
        if (currentCategoryLiveData != null) {
            currentCategoryLiveData.removeObservers(this);
        }
    }

    // ================ PIE CHART ================

    private void setupPieChart() {
        pieChart.setUsePercentValues(false); // We calculate percentages manually
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(28f, 10f, 28f, 10f); // Extra side offsets to prevent label clipping
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setHoleRadius(60f); // Modern, wider donut hole
        pieChart.setTransparentCircleRadius(64f);
        pieChart.setTransparentCircleColor(Color.parseColor("#0F000000")); // Nice clean inner shadow
        pieChart.setTransparentCircleAlpha(100);
        pieChart.setCenterText("% of\nIncome");
        pieChart.setCenterTextColor(Color.parseColor("#1C1939"));
        pieChart.setCenterTextSize(12f);
        pieChart.setEntryLabelColor(Color.parseColor("#1C1939")); // Dark text for category labels (since drawn outside)
        pieChart.setEntryLabelTextSize(10f);
        pieChart.setDrawEntryLabels(true); // Enabled: category names drawn outside slices
        pieChart.getLegend().setEnabled(false);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);
        pieChart.animateY(800);
    }

    private void setupHomePieChart() {
        if (homePieChart == null) return;
        homePieChart.setUsePercentValues(false);
        homePieChart.getDescription().setEnabled(false);
        homePieChart.setExtraOffsets(24f, 8f, 24f, 8f);
        homePieChart.setDragDecelerationFrictionCoef(0.95f);
        homePieChart.setDrawHoleEnabled(true);

        // Use theme-aware hole color
        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(R.attr.themeCardBg, typedValue, true);
        homePieChart.setHoleColor(typedValue.data);

        homePieChart.setHoleRadius(58f);
        homePieChart.setTransparentCircleRadius(62f);
        homePieChart.setTransparentCircleColor(Color.parseColor("#0F000000"));
        homePieChart.setTransparentCircleAlpha(80);
        homePieChart.setCenterText("Expenses");

        // Use theme-aware text colors
        android.util.TypedValue textValue = new android.util.TypedValue();
        getTheme().resolveAttribute(R.attr.themeTextPrimary, textValue, true);
        homePieChart.setCenterTextColor(textValue.data);
        homePieChart.setEntryLabelColor(textValue.data);

        homePieChart.setCenterTextSize(11f);
        homePieChart.setEntryLabelTextSize(9f);
        homePieChart.setDrawEntryLabels(true);
        homePieChart.getLegend().setEnabled(false);
        homePieChart.setRotationEnabled(true);
        homePieChart.setHighlightPerTapEnabled(true);
        homePieChart.animateY(800);
    }

    private void updateDashboardMonth() {
        if (tvDashboardMonth == null) return;
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvDashboardMonth.setText(sdf.format(new java.util.Date()));
    }

    /**
     * Update pie chart with expense categories as percentage of monthly income.
     * Each slice = (categoryExpense / monthlyIncome) × 100
     * A "Savings" slice shows the remaining percentage.
     */
    private void updatePieChart(List<TransactionDao.CategorySummary> summaries) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();

        if (summaries != null && !summaries.isEmpty()) {
            if (currentIncome > 0) {
                // Calculate percentages based on income
                double totalExpensePercent = 0;
                int colorIndex = 0;

                for (TransactionDao.CategorySummary summary : summaries) {
                    float percent = (float) ((summary.totalAmount / currentIncome) * 100);
                    if (percent > 0.5f) { // Only show categories > 0.5%
                        entries.add(new PieEntry(percent, summary.category));
                        colors.add(PIE_COLORS[colorIndex % PIE_COLORS.length]);
                        totalExpensePercent += percent;
                        colorIndex++;
                    }
                }

                // Add savings/remaining slice
                double savingsPercent = 100 - totalExpensePercent;
                if (savingsPercent > 0) {
                    entries.add(new PieEntry((float) savingsPercent, "Savings"));
                    colors.add(Color.parseColor("#00CEC9")); // Teal for savings
                } else if (savingsPercent < 0) {
                    // Over-spent! Show negative savings as "Over Budget"
                    entries.add(new PieEntry((float) Math.abs(savingsPercent), "Over Budget"));
                    colors.add(Color.parseColor("#FF6B6B")); // Coral red
                }
            } else {
                // No income — fallback to raw amounts
                int colorIndex = 0;
                for (TransactionDao.CategorySummary summary : summaries) {
                    entries.add(new PieEntry((float) summary.totalAmount, summary.category));
                    colors.add(PIE_COLORS[colorIndex % PIE_COLORS.length]);
                    colorIndex++;
                }
            }
        }

        if (entries.isEmpty()) {
            pieChart.clear();
            pieChart.setCenterText("No Data");
            pieChart.invalidate();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(3f); // Slightly wider slice space
        dataSet.setSelectionShift(8f);
        dataSet.setColors(colors);
        
        // Draw both category labels and values outside the slices connected with soft lines
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setValueLinePart1OffsetPercentage(80f);
        dataSet.setValueLinePart1Length(0.22f);
        dataSet.setValueLinePart2Length(0.32f);
        dataSet.setValueLineColor(Color.parseColor("#A0A5BA")); // Soft gray-blue connector line

        PieData data = new PieData(dataSet);
        data.setValueTextSize(10f);
        data.setValueTextColor(Color.parseColor("#3F2B96")); // Primary purple color for text labels outside slices
        data.setValueFormatter(new PercentFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.1f%%", value);
            }
        });

        pieChart.setData(data);
        if (currentIncome > 0) {
            pieChart.setCenterText("% of\nIncome");
        } else {
            pieChart.setCenterText("Expenses");
        }
        pieChart.animateY(600);
        pieChart.invalidate();

        // Also update the Home Pie Chart with the same data
        updateHomePieChart(entries, colors);
    }

    private void updateHomePieChart(ArrayList<PieEntry> entries, ArrayList<Integer> colors) {
        if (homePieChart == null) return;

        if (entries.isEmpty()) {
            homePieChart.clear();
            homePieChart.setCenterText("No Data");
            homePieChart.invalidate();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(6f);
        dataSet.setColors(colors);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setValueLinePart1OffsetPercentage(80f);
        dataSet.setValueLinePart1Length(0.2f);
        dataSet.setValueLinePart2Length(0.3f);

        // Use theme-aware line color
        android.util.TypedValue lineColorValue = new android.util.TypedValue();
        getTheme().resolveAttribute(R.attr.themeTextSecondary, lineColorValue, true);
        dataSet.setValueLineColor(lineColorValue.data);

        PieData data = new PieData(dataSet);
        data.setValueTextSize(9f);

        android.util.TypedValue textColorValue = new android.util.TypedValue();
        getTheme().resolveAttribute(R.attr.themeTextPrimary, textColorValue, true);
        data.setValueTextColor(textColorValue.data);

        data.setValueFormatter(new PercentFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.1f%%", value);
            }
        });

        homePieChart.setData(data);
        if (currentIncome > 0) {
            homePieChart.setCenterText("% of\nIncome");
        } else {
            homePieChart.setCenterText("Expenses");
        }
        homePieChart.animateY(600);
        homePieChart.invalidate();
    }

    // ================ BALANCE ================

    private void updateBalanceDisplay() {
        double balance = currentIncome - currentExpense;
        tvBalance.setText("₹" + String.format(Locale.getDefault(), "%,.0f", balance));
    }

    // ================ EDIT/DELETE DIALOG ================

    private void showEditDeleteDialog(Transaction transaction) {
        String[] options = {"Edit", "Delete"};
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.DarkAlertDialog)
                .setTitle("Select Action")
                .setItems(options, (d, which) -> {
                    if (which == 0) {
                        showTransactionDialog(transaction);
                    } else {
                        AlertDialog confirmDialog = new AlertDialog.Builder(this, R.style.DarkAlertDialog)
                                .setTitle("Confirm Delete")
                                .setMessage("Are you sure you want to delete this transaction?")
                                .setPositiveButton("Delete", (dd, w) -> {
                                    executorService.execute(() -> {
                                        transactionDao.delete(transaction);
                                        triggerAutoSyncIfEnabled();
                                        runOnUiThread(() -> {
                                            updateWalletBalances();
                                            updateBudgetProgress();
                                            updateBarChart();
                                        });
                                    });
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                        styleDialogButtons(confirmDialog);
                    }
                })
                .show();
        styleDialogButtons(dialog);
    }

    private void handleGroupClick(TransactionAdapter.TransactionGroup group) {
        if (group.transactions.size() == 1) {
            showEditDeleteDialog(group.transactions.get(0));
        } else {
            showGroupDetailsDialog(group);
        }
    }

    private void showGroupDetailsDialog(TransactionAdapter.TransactionGroup group) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DarkAlertDialog);
        builder.setTitle(group.category + " Details (" + group.transactions.size() + ")");

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_group_details, null);
        builder.setView(view);

        RecyclerView rvDetails = view.findViewById(R.id.rvGroupDetails);
        rvDetails.setLayoutManager(new LinearLayoutManager(this));

        // detailsAdapter is flat (isGrouped = false)
        TransactionAdapter detailsAdapter = new TransactionAdapter(null, false);
        rvDetails.setAdapter(detailsAdapter);
        detailsAdapter.setTransactions(group.transactions);

        builder.setNegativeButton("Close", null);
        AlertDialog alertDialog = builder.show();
        styleDialogButtons(alertDialog);
        detailsAdapter.setOnTransactionClickListener(subGroup -> {
            alertDialog.dismiss();
            showEditDeleteDialog(subGroup.transactions.get(0));
        });
    }

    // ================ ADD/EDIT TRANSACTION DIALOG ================

    private void showTransactionDialog(Transaction transaction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.TransactionDialog);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_transaction, null);
        builder.setView(view);

        RadioGroup rgType = view.findViewById(R.id.rgType);
        AutoCompleteTextView etCategory = view.findViewById(R.id.etCategory);
        EditText etAmount = view.findViewById(R.id.etAmount);
        EditText etDescription = view.findViewById(R.id.etDescription);
        AutoCompleteTextView etWallet = view.findViewById(R.id.etWallet);
        EditText etDate = view.findViewById(R.id.etDate);
        TextView tvTypeEmoji = view.findViewById(R.id.tvTypeEmoji);

        // Set up Wallet exposed dropdown
        ArrayAdapter<String> walletAdapter = new ArrayAdapter<>(this, R.layout.spinner_dropdown_item, new String[]{"Cash", "Bank", "Card"});
        etWallet.setAdapter(walletAdapter);

        // Use dynamic list for category exposed dropdown with custom categories loaded from preferences
        ArrayAdapter<String> expenseAdapter = new ArrayAdapter<>(this, R.layout.spinner_dropdown_item, getCategoriesList(false));
        ArrayAdapter<String> incomeAdapter = new ArrayAdapter<>(this, R.layout.spinner_dropdown_item, getCategoriesList(true));

        // Date Picker Dialog setup
        etDate.setFocusable(false);
        etDate.setClickable(true);
        etDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            String dateStr = etDate.getText().toString();
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date parsedDate = sdf.parse(dateStr);
                if (parsedDate != null) {
                    calendar.setTime(parsedDate);
                }
            } catch (Exception ignored) {}

            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(MainActivity.this,
                    (view2, selectedYear, selectedMonth, selectedDay) -> {
                        String formattedDate = String.format(Locale.getDefault(), "%d-%02d-%02d",
                                selectedYear, selectedMonth + 1, selectedDay);
                        etDate.setText(formattedDate);
                    }, year, month, day);
            datePickerDialog.show();
        });

        com.google.android.material.textfield.TextInputLayout tilCategory = view.findViewById(R.id.tilCategory);
        com.google.android.material.textfield.TextInputLayout tilWallet = view.findViewById(R.id.tilWallet);

        if (transaction != null) {
            if (tvTypeEmoji != null) tvTypeEmoji.setText("✏️");
            if ("Income".equals(transaction.type)) {
                ((RadioButton) view.findViewById(R.id.rbIncome)).setChecked(true);
                if (tvTypeEmoji != null) tvTypeEmoji.setText("💰");
                if (tilCategory != null) tilCategory.setHint("Category");
                if (tilWallet != null) tilWallet.setHint("Wallet / Account");
                etCategory.setAdapter(incomeAdapter);
            } else if ("Expense".equals(transaction.type)) {
                ((RadioButton) view.findViewById(R.id.rbExpense)).setChecked(true);
                if (tvTypeEmoji != null) tvTypeEmoji.setText("💸");
                if (tilCategory != null) tilCategory.setHint("Category");
                if (tilWallet != null) tilWallet.setHint("Wallet / Account");
                etCategory.setAdapter(expenseAdapter);
            } else if ("Transfer".equals(transaction.type)) {
                ((RadioButton) view.findViewById(R.id.rbTransfer)).setChecked(true);
                if (tvTypeEmoji != null) tvTypeEmoji.setText("🔀");
                if (tilCategory != null) tilCategory.setHint("Transfer To (Destination)");
                if (tilWallet != null) tilWallet.setHint("Transfer From (Source)");
                etCategory.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_dropdown_item, new String[]{"Cash", "Bank", "Card"}));
            }
            etCategory.setText(transaction.category, false);
            etAmount.setText(String.valueOf(transaction.amount));
            etDescription.setText(transaction.description);
            etWallet.setText(transaction.wallet != null ? transaction.wallet : "Cash", false);
            etDate.setText(transaction.date);
        } else {
            if (tvTypeEmoji != null) tvTypeEmoji.setText("💸");
            String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            etDate.setText(currentDate);
            etCategory.setAdapter(expenseAdapter);
            etWallet.setText("Cash", false);
        }

        rgType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbIncome) {
                if (tvTypeEmoji != null) tvTypeEmoji.setText("💰");
                if (tilCategory != null) tilCategory.setHint("Category");
                if (tilWallet != null) tilWallet.setHint("Wallet / Account");
                etCategory.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_dropdown_item, getCategoriesList(true)));
            } else if (checkedId == R.id.rbExpense) {
                if (tvTypeEmoji != null) tvTypeEmoji.setText("💸");
                if (tilCategory != null) tilCategory.setHint("Category");
                if (tilWallet != null) tilWallet.setHint("Wallet / Account");
                etCategory.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_dropdown_item, getCategoriesList(false)));
            } else if (checkedId == R.id.rbTransfer) {
                if (tvTypeEmoji != null) tvTypeEmoji.setText("🔀");
                if (tilCategory != null) tilCategory.setHint("Transfer To (Destination)");
                if (tilWallet != null) tilWallet.setHint("Transfer From (Source)");
                etCategory.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_dropdown_item, new String[]{"Cash", "Bank", "Card"}));
            }
            etCategory.setText("");
        });

        etCategory.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String selectedCategory = s.toString().trim();
                if (selectedCategory.equals("+ Add Custom Category")) {
                    etCategory.setText("");
                    showAddCustomCategoryDialog(rgType.getCheckedRadioButtonId() == R.id.rbIncome, etCategory);
                    return;
                }
                // Only auto-populate if the field is focused (user-initiated) and has a value
                if (!selectedCategory.isEmpty() && etCategory.hasFocus()) {
                    autoPopulateFromLastTransaction(selectedCategory, etAmount, etDescription);
                }
            }
        });

        // Show dialog first, then wire up the in-layout buttons
        AlertDialog alertDialog = builder.show();

        Button btnDialogCancel = view.findViewById(R.id.btnDialogCancel);
        Button btnDialogSave = view.findViewById(R.id.btnDialogSave);

        if (btnDialogCancel != null) {
            btnDialogCancel.setOnClickListener(v -> alertDialog.dismiss());
        }

        if (btnDialogSave != null) {
            btnDialogSave.setOnClickListener(v -> {
                RadioButton rbIncome = view.findViewById(R.id.rbIncome);
                RadioButton rbTransfer = view.findViewById(R.id.rbTransfer);
                String type = "Expense";
                if (rbIncome.isChecked()) {
                    type = "Income";
                } else if (rbTransfer != null && rbTransfer.isChecked()) {
                    type = "Transfer";
                }

                String category = etCategory.getText().toString();
                String amountStr = etAmount.getText().toString();
                String description = etDescription.getText().toString();
                String wallet = etWallet.getText().toString();
                String date = etDate.getText().toString();

                if (category.isEmpty() || amountStr.isEmpty()) {
                    Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                if ("Transfer".equalsIgnoreCase(type) && wallet.equalsIgnoreCase(category)) {
                    Toast.makeText(this, "Source and destination wallets must be different", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    double amount = Double.parseDouble(amountStr);
                    if (transaction != null) {
                        transaction.type = type;
                        transaction.category = category;
                        transaction.amount = amount;
                        transaction.description = description;
                        transaction.wallet = wallet;
                        transaction.date = Transaction.normalizeDate(date);
                        transaction.fy = Transaction.getFYFromDate(transaction.date);
                        transaction.month = Transaction.getMonthNameFromDate(transaction.date);
                        executorService.execute(() -> {
                            transactionDao.update(transaction);
                            triggerAutoSyncIfEnabled();
                            runOnUiThread(() -> {
                                updateWalletBalances();
                                updateBudgetProgress();
                                updateBarChart();
                            });
                        });
                    } else {
                        Transaction newTransaction = new Transaction(date, type, category, amount, description, wallet);
                        executorService.execute(() -> {
                            transactionDao.insert(newTransaction);
                            triggerAutoSyncIfEnabled();
                            runOnUiThread(() -> {
                                updateWalletBalances();
                                updateBudgetProgress();
                                updateBarChart();
                            });
                        });
                    }
                    alertDialog.dismiss();
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void autoPopulateFromLastTransaction(String category, EditText etAmount, EditText etDescription) {
        executorService.execute(() -> {
            Transaction lastTx = transactionDao.getLastTransactionByCategory(category);
            if (lastTx != null) {
                runOnUiThread(() -> {
                    if (lastTx.amount == (long) lastTx.amount) {
                        etAmount.setText(String.format(Locale.getDefault(), "%d", (long) lastTx.amount));
                    } else {
                        etAmount.setText(String.format(Locale.getDefault(), "%.2f", lastTx.amount));
                    }
                    etDescription.setText(lastTx.description != null ? lastTx.description : "");
                });
            }
        });
    }

    private void styleDialogButtons(AlertDialog dialog) {
        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.primary));
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }
    }

    private List<String> getCategoriesList(boolean isIncome) {
        List<String> categories = new ArrayList<>();
        String[] predefined = isIncome ? incomeCategories : expenseCategories;
        for (String c : predefined) {
            categories.add(c);
        }

        // Add custom categories stored in SharedPreferences
        android.content.SharedPreferences prefs = getSharedPreferences("ExpenseTrackerPrefs", MODE_PRIVATE);
        java.util.Set<String> custom = prefs.getStringSet(isIncome ? "custom_income" : "custom_expense", null);
        if (custom != null) {
            for (String c : custom) {
                if (!categories.contains(c)) {
                    categories.add(c);
                }
            }
        }

        // Append option to add a new category dynamically
        categories.add("+ Add Custom Category");
        return categories;
    }

    private void showAddCustomCategoryDialog(boolean isIncome, AutoCompleteTextView etCategory) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DarkAlertDialog);
        builder.setTitle("Add Custom Category");

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_custom_category, null);
        builder.setView(view);

        EditText etCustomName = view.findViewById(R.id.etCustomCategoryName);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = etCustomName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Category name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save in SharedPreferences
            android.content.SharedPreferences prefs = getSharedPreferences("ExpenseTrackerPrefs", MODE_PRIVATE);
            java.util.Set<String> custom = new java.util.HashSet<>(prefs.getStringSet(isIncome ? "custom_income" : "custom_expense", new java.util.HashSet<>()));
            custom.add(name);
            prefs.edit().putStringSet(isIncome ? "custom_income" : "custom_expense", custom).apply();

            // Refresh category dropdown list in autocomplete adapter
            List<String> newList = getCategoriesList(isIncome);
            ArrayAdapter<String> newAdapter = new ArrayAdapter<>(this, R.layout.spinner_dropdown_item, newList);
            etCategory.setAdapter(newAdapter);
            etCategory.setText(name, false);
        });

        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.show();
        styleDialogButtons(dialog);
    }

    private void performExport(Uri uri) {
        String[] dateRange = getDateRange();
        String startDate = dateRange[0];
        String endDate = dateRange[1];
        String monthName = fyMonthNames[selectedMonthIndex];

        // Capture active filter states on the main thread
        final String currentQuery = etSearch != null ? etSearch.getText().toString().trim().toLowerCase() : "";
        final String currentType = filterType;
        final String currentWallet = filterWallet;

        executorService.execute(() -> {
            List<Transaction> transactions = transactionDao.getTransactionsByDateRangeSync(startDate, endDate);
            
            // Filter transactions by UI filters
            List<Transaction> filteredTransactions = new ArrayList<>();
            if (transactions != null) {
                for (Transaction t : transactions) {
                    boolean matchesQuery = true;
                    if (!currentQuery.isEmpty()) {
                        String cat = t.category != null ? t.category.toLowerCase() : "";
                        String desc = t.description != null ? t.description.toLowerCase() : "";
                        matchesQuery = cat.contains(currentQuery) || desc.contains(currentQuery);
                    }

                    boolean matchesType = true;
                    if ("Income".equalsIgnoreCase(currentType)) {
                        matchesType = "Income".equalsIgnoreCase(t.type);
                    } else if ("Expense".equalsIgnoreCase(currentType)) {
                        matchesType = "Expense".equalsIgnoreCase(t.type);
                    }

                    boolean matchesWallet = true;
                    String w = t.wallet != null ? t.wallet : "Cash";
                    if ("Cash".equalsIgnoreCase(currentWallet)) {
                        matchesWallet = "Cash".equalsIgnoreCase(w);
                    } else if ("Bank".equalsIgnoreCase(currentWallet)) {
                        matchesWallet = "Bank".equalsIgnoreCase(w);
                    } else if ("Card".equalsIgnoreCase(currentWallet)) {
                        matchesWallet = "Card".equalsIgnoreCase(w);
                    }

                    if (matchesQuery && matchesType && matchesWallet) {
                        filteredTransactions.add(t);
                    }
                }
            }

            boolean success = false;
            try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                if (outputStream != null) {
                    success = ExcelExporter.export(filteredTransactions, selectedFY, monthName, outputStream);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            final boolean finalSuccess = success;
            runOnUiThread(() -> {
                if (finalSuccess) {
                    Toast.makeText(this, "Report exported successfully!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Failed to export report", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void performPdfExport(Uri uri) {
        String[] dateRange = getDateRange();
        String startDate = dateRange[0];
        String endDate = dateRange[1];
        String monthName = fyMonthNames[selectedMonthIndex];

        // Capture active filter states on the main thread
        final String currentQuery = etSearch != null ? etSearch.getText().toString().trim().toLowerCase() : "";
        final String currentType = filterType;
        final String currentWallet = filterWallet;

        // Capture Chart and Profile Bitmaps on UI thread
        final Bitmap pieBitmap = (pieChart != null && pieChart.getWidth() > 0 && pieChart.getHeight() > 0) 
                ? pieChart.getChartBitmap() : null;
        final Bitmap lineBitmap = (lineChart != null && lineChart.getWidth() > 0 && lineChart.getHeight() > 0) 
                ? lineChart.getChartBitmap() : null;
                
        Bitmap profileBitmapTemp = null;
        ImageView ivToolbarProfilePic = findViewById(R.id.ivToolbarProfilePic);
        if (ivToolbarProfilePic != null && ivToolbarProfilePic.getVisibility() == View.VISIBLE) {
            android.graphics.drawable.Drawable d = ivToolbarProfilePic.getDrawable();
            if (d instanceof android.graphics.drawable.BitmapDrawable) {
                profileBitmapTemp = ((android.graphics.drawable.BitmapDrawable) d).getBitmap();
            }
        }
        final Bitmap profileBitmap = profileBitmapTemp;

        String userEmailTemp = "";
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userEmailTemp = user.getEmail();
        }
        final String userEmail = userEmailTemp;

        executorService.execute(() -> {
            List<Transaction> transactions = transactionDao.getTransactionsByDateRangeSync(startDate, endDate);
            
            // Filter transactions by UI filters
            List<Transaction> filteredTransactions = new ArrayList<>();
            double totalIncome = 0;
            double totalExpense = 0;

            if (transactions != null) {
                for (Transaction t : transactions) {
                    boolean matchesQuery = true;
                    if (!currentQuery.isEmpty()) {
                        String cat = t.category != null ? t.category.toLowerCase() : "";
                        String desc = t.description != null ? t.description.toLowerCase() : "";
                        matchesQuery = cat.contains(currentQuery) || desc.contains(currentQuery);
                    }

                    boolean matchesType = true;
                    if ("Income".equalsIgnoreCase(currentType)) {
                        matchesType = "Income".equalsIgnoreCase(t.type);
                    } else if ("Expense".equalsIgnoreCase(currentType)) {
                        matchesType = "Expense".equalsIgnoreCase(t.type);
                    }

                    boolean matchesWallet = true;
                    String w = t.wallet != null ? t.wallet : "Cash";
                    if ("Cash".equalsIgnoreCase(currentWallet)) {
                        matchesWallet = "Cash".equalsIgnoreCase(w);
                    } else if ("Bank".equalsIgnoreCase(currentWallet)) {
                        matchesWallet = "Bank".equalsIgnoreCase(w);
                    } else if ("Card".equalsIgnoreCase(currentWallet)) {
                        matchesWallet = "Card".equalsIgnoreCase(w);
                    }

                    if (matchesQuery && matchesType && matchesWallet) {
                        filteredTransactions.add(t);
                        if ("Income".equalsIgnoreCase(t.type)) {
                            totalIncome += t.amount;
                        } else if ("Expense".equalsIgnoreCase(t.type)) {
                            totalExpense += t.amount;
                        }
                    }
                }
            }

            boolean success = false;
            try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                if (outputStream != null) {
                    success = PdfExporter.export(
                            this,
                            filteredTransactions,
                            selectedFY,
                            monthName,
                            totalIncome,
                            totalExpense,
                            pieBitmap,
                            lineBitmap,
                            profileBitmap,
                            userEmail,
                            outputStream
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            final boolean finalSuccess = success;
            runOnUiThread(() -> {
                if (finalSuccess) {
                    Toast.makeText(this, "PDF Report exported successfully!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Failed to export PDF Report", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.google_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    showSignInProgress(false);
                    if (task.isSuccessful()) {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        Toast.makeText(MainActivity.this, "Welcome " + (user != null ? user.getDisplayName() : "") + "!", Toast.LENGTH_SHORT).show();
                        updateProfileUI();
                        updateProfileStats();
                        handleLoginSync();
                    } else {
                        Toast.makeText(MainActivity.this, "Authentication Failed: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        if (mGoogleSignInClient != null) {
            mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
                Toast.makeText(MainActivity.this, "Signed Out Successfully", Toast.LENGTH_SHORT).show();
                updateProfileUI();
                updateProfileStats();
            });
        } else {
            Toast.makeText(MainActivity.this, "Signed Out Successfully", Toast.LENGTH_SHORT).show();
            updateProfileUI();
            updateProfileStats();
        }
    }

    private void updateProfileUI() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        TextView tvProfileName = findViewById(R.id.tvProfileName);
        TextView tvProfileEmail = findViewById(R.id.tvProfileEmail);
        ImageView ivProfilePic = findViewById(R.id.ivProfilePic);
        TextView tvProfileInitials = findViewById(R.id.tvProfileInitials);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        btnGoogleSignOut = findViewById(R.id.btnGoogleSignOut);
        
        TextView tvToolbarEmail = findViewById(R.id.tvToolbarEmail);
        View layoutLogin = findViewById(R.id.layoutLogin);

        ImageView ivToolbarProfilePic = findViewById(R.id.ivToolbarProfilePic);
        TextView tvToolbarProfileInitials = findViewById(R.id.tvToolbarProfileInitials);

        if (user != null) {
            if (tvProfileName != null) tvProfileName.setText(user.getDisplayName() != null ? user.getDisplayName() : "Firebase User");
            if (tvProfileEmail != null) tvProfileEmail.setText(user.getEmail());
            
            String initials = "U";
            String name = user.getDisplayName();
            if (name != null && !name.isEmpty()) {
                initials = name.substring(0, 1).toUpperCase();
            }

            if (tvProfileInitials != null) {
                tvProfileInitials.setText(initials);
                tvProfileInitials.setVisibility(View.VISIBLE);
            }
            if (ivProfilePic != null) ivProfilePic.setVisibility(View.GONE);
            
            if (tvToolbarProfileInitials != null) {
                tvToolbarProfileInitials.setText(initials);
                tvToolbarProfileInitials.setVisibility(View.VISIBLE);
            }
            if (ivToolbarProfilePic != null) ivToolbarProfilePic.setVisibility(View.GONE);

            // Load user photo if available from Google Account
            if (user.getPhotoUrl() != null) {
                String photoUrlStr = user.getPhotoUrl().toString();
                loadProfileImage(photoUrlStr, ivProfilePic, tvProfileInitials);
                loadProfileImage(photoUrlStr, ivToolbarProfilePic, tvToolbarProfileInitials);
            }
            
            if (btnGoogleSignIn != null) btnGoogleSignIn.setVisibility(View.GONE);
            if (btnGoogleSignOut != null) btnGoogleSignOut.setVisibility(View.VISIBLE);
            
            if (tvToolbarEmail != null) {
                tvToolbarEmail.setText(user.getEmail());
                tvToolbarEmail.setVisibility(View.VISIBLE);
            }
            if (layoutLogin != null) {
                layoutLogin.setVisibility(View.GONE);
            }
        } else {
            if (tvProfileName != null) tvProfileName.setText("Guest User");
            if (tvProfileEmail != null) tvProfileEmail.setText("Sign in to backup data");
            
            if (tvProfileInitials != null) {
                tvProfileInitials.setText("?");
                tvProfileInitials.setVisibility(View.VISIBLE);
            }
            if (ivProfilePic != null) ivProfilePic.setVisibility(View.GONE);
            
            if (tvToolbarProfileInitials != null) {
                tvToolbarProfileInitials.setText("?");
                tvToolbarProfileInitials.setVisibility(View.VISIBLE);
            }
            if (ivToolbarProfilePic != null) ivToolbarProfilePic.setVisibility(View.GONE);
            
            if (btnGoogleSignIn != null) btnGoogleSignIn.setVisibility(View.VISIBLE);
            if (btnGoogleSignOut != null) btnGoogleSignOut.setVisibility(View.GONE);
            
            if (tvToolbarEmail != null) {
                tvToolbarEmail.setVisibility(View.GONE);
            }
            if (layoutLogin != null) {
                layoutLogin.setVisibility(View.VISIBLE);
            }
        }
        
        updateSyncStatusDisplay();
    }

    private void loadProfileImage(String urlStr, ImageView imageView, TextView initialsView) {
        if (urlStr == null || urlStr.isEmpty() || imageView == null) return;
        executorService.execute(() -> {
            try {
                java.net.URL url = new java.net.URL(urlStr);
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(url.openConnection().getInputStream());
                if (bitmap != null) {
                    runOnUiThread(() -> {
                        imageView.setImageBitmap(bitmap);
                        imageView.setVisibility(View.VISIBLE);
                        if (initialsView != null) initialsView.setVisibility(View.GONE);
                    });
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Error loading profile image", e);
            }
        });
    }

    private void setupFirebaseSync() {
        FirebaseSyncManager syncManager = FirebaseSyncManager.getInstance();
        SwitchMaterial switchAutoSync = findViewById(R.id.switchAutoSync);
        Button btnBackupCloud = findViewById(R.id.btnBackupCloud);
        Button btnRestoreCloud = findViewById(R.id.btnRestoreCloud);

        if (switchAutoSync != null) {
            switchAutoSync.setChecked(syncManager.isAutoSyncEnabled(this));
            switchAutoSync.setOnCheckedChangeListener((buttonView, isChecked) -> {
                syncManager.setAutoSyncEnabled(this, isChecked);
                updateSyncStatusDisplay();
                if (isChecked) {
                    Toast.makeText(this, "Auto-Sync Enabled", Toast.LENGTH_SHORT).show();
                    triggerAutoSyncIfEnabled();
                } else {
                    Toast.makeText(this, "Auto-Sync Disabled", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnBackupCloud != null) {
            btnBackupCloud.setOnClickListener(v -> {
                btnBackupCloud.setEnabled(false);
                btnRestoreCloud.setEnabled(false);
                btnBackupCloud.setText("Backing up...");

                executorService.execute(() -> {
                    List<Transaction> transactions = transactionDao.getAllTransactionsSync();
                    syncManager.uploadTransactions(MainActivity.this, transactions, new FirebaseSyncManager.SyncCallback() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(() -> {
                                btnBackupCloud.setEnabled(true);
                                btnRestoreCloud.setEnabled(true);
                                btnBackupCloud.setText("Backup Now");
                                updateSyncStatusDisplay();
                                Toast.makeText(MainActivity.this, "Backup to cloud completed successfully!", Toast.LENGTH_LONG).show();
                            });
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            runOnUiThread(() -> {
                                btnBackupCloud.setEnabled(true);
                                btnRestoreCloud.setEnabled(true);
                                btnBackupCloud.setText("Backup Now");
                                Toast.makeText(MainActivity.this, "Backup failed: " + errorMessage, Toast.LENGTH_LONG).show();
                            });
                        }
                    });
                });
            });
        }

        if (btnRestoreCloud != null) {
            btnRestoreCloud.setOnClickListener(v -> {
                new AlertDialog.Builder(MainActivity.this, R.style.DarkAlertDialog)
                        .setTitle("Restore from Cloud")
                        .setMessage("Restoring from cloud will replace all local transactions with the cloud backup. This action cannot be undone. Are you sure you want to proceed?")
                        .setPositiveButton("Restore", (dialog, which) -> {
                            btnBackupCloud.setEnabled(false);
                            btnRestoreCloud.setEnabled(false);
                            btnRestoreCloud.setText("Restoring...");

                            syncManager.downloadTransactions(MainActivity.this, new FirebaseSyncManager.SyncCallback() {
                                @Override
                                public void onSuccess() {
                                    runOnUiThread(() -> {
                                        btnBackupCloud.setEnabled(true);
                                        btnRestoreCloud.setEnabled(true);
                                        btnRestoreCloud.setText("Restore Now");
                                        updateSyncStatusDisplay();
                                        Toast.makeText(MainActivity.this, "Database restored from cloud successfully!", Toast.LENGTH_LONG).show();
                                    });
                                }

                                @Override
                                public void onFailure(String errorMessage) {
                                    runOnUiThread(() -> {
                                        btnBackupCloud.setEnabled(true);
                                        btnRestoreCloud.setEnabled(true);
                                        btnRestoreCloud.setText("Restore Now");
                                        Toast.makeText(MainActivity.this, "Restore failed: " + errorMessage, Toast.LENGTH_LONG).show();
                                    });
                                }
                            });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        btnGoogleSignOut = findViewById(R.id.btnGoogleSignOut);

        if (btnGoogleSignIn != null) {
            btnGoogleSignIn.setOnClickListener(v -> {
                if (mGoogleSignInClient != null) {
                    showSignInProgress(true);
                    Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                    googleSignInLauncher.launch(signInIntent);
                }
            });
        }

        if (btnGoogleSignOut != null) {
            btnGoogleSignOut.setOnClickListener(v -> {
                new AlertDialog.Builder(MainActivity.this, R.style.DarkAlertDialog)
                        .setTitle("Sign Out")
                        .setMessage("Are you sure you want to sign out? Your sync path will switch back to local device.")
                        .setPositiveButton("Sign Out", (dialog, which) -> signOut())
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        updateProfileUI();
        updateSyncStatusDisplay();
    }

    private void updateSyncStatusDisplay() {
        FirebaseSyncManager syncManager = FirebaseSyncManager.getInstance();
        long lastSyncTime = syncManager.getLastSyncTime(this);
        TextView tvCloudSyncStatus = findViewById(R.id.tvCloudSyncStatus);
        ImageView ivCloudStatus = findViewById(R.id.ivCloudStatus);
        
        if (tvCloudSyncStatus == null) return;

        if (lastSyncTime == 0) {
            tvCloudSyncStatus.setText("Last backup: Never");
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
            tvCloudSyncStatus.setText("Last backup: " + sdf.format(new Date(lastSyncTime)));
        }

        if (ivCloudStatus != null) {
            boolean isAuto = syncManager.isAutoSyncEnabled(this);
            if (isAuto) {
                ivCloudStatus.setImageTintList(android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.income_green)));
            } else {
                ivCloudStatus.setImageTintList(android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.text_secondary)));
            }
        }
    }

    private void triggerAutoSyncIfEnabled() {
        FirebaseSyncManager syncManager = FirebaseSyncManager.getInstance();
        if (syncManager.isAutoSyncEnabled(this)) {
            executorService.execute(() -> {
                List<Transaction> transactions = transactionDao.getAllTransactionsSync();
                syncManager.uploadTransactions(MainActivity.this, transactions, new FirebaseSyncManager.SyncCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> updateSyncStatusDisplay());
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e("MainActivity", "Auto-sync failed: " + errorMessage);
                    }
                });
            });
        }
    }

    private void setupLoginOverlay() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        View layoutLogin = findViewById(R.id.layoutLogin);
        btnLoginOverlay = findViewById(R.id.btnLoginOverlay);
        btnSkipLogin = findViewById(R.id.btnSkipLogin);

        if (layoutLogin != null) {
            if (user != null) {
                layoutLogin.setVisibility(View.GONE);
            } else {
                layoutLogin.setVisibility(View.VISIBLE);
            }
        }

        if (btnLoginOverlay != null) {
            btnLoginOverlay.setOnClickListener(v -> {
                if (mGoogleSignInClient != null) {
                    showSignInProgress(true);
                    Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                    googleSignInLauncher.launch(signInIntent);
                }
            });
        }

        if (btnSkipLogin != null) {
            btnSkipLogin.setOnClickListener(v -> {
                if (layoutLogin != null) {
                    layoutLogin.setVisibility(View.GONE);
                }
            });
        }
    }

    private void showSignInProgress(boolean show) {
        if (show) {
            if (btnGoogleSignIn != null) {
                btnGoogleSignIn.setEnabled(false);
                btnGoogleSignIn.setText("Connecting...");
            }
            if (btnLoginOverlay != null) {
                btnLoginOverlay.setEnabled(false);
                btnLoginOverlay.setText("Connecting...");
            }
            if (btnSkipLogin != null) {
                btnSkipLogin.setEnabled(false);
            }
        } else {
            if (btnGoogleSignIn != null) {
                btnGoogleSignIn.setEnabled(true);
                btnGoogleSignIn.setText("Sign In with Google");
            }
            if (btnLoginOverlay != null) {
                btnLoginOverlay.setEnabled(true);
                btnLoginOverlay.setText("Sign In with Google");
            }
            if (btnSkipLogin != null) {
                btnSkipLogin.setEnabled(true);
            }
        }
    }

    private void handleLoginSync() {
        FirebaseSyncManager syncManager = FirebaseSyncManager.getInstance();

        if (btnSkipLogin != null) {
            btnSkipLogin.setEnabled(false);
        }

        syncManager.getTargetRef(this).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                List<Transaction> cloudTxList = new ArrayList<>();
                if (snapshot.exists()) {
                    for (com.google.firebase.database.DataSnapshot ds : snapshot.getChildren()) {
                        Transaction tx = ds.getValue(Transaction.class);
                        if (tx != null) {
                            tx.date = Transaction.normalizeDate(tx.date);
                            tx.fy = Transaction.getFYFromDate(tx.date);
                            tx.month = Transaction.getMonthNameFromDate(tx.date);
                            cloudTxList.add(tx);
                        }
                    }
                }

                executorService.execute(() -> {
                    List<Transaction> localTxList = transactionDao.getAllTransactionsSync();

                    runOnUiThread(() -> {
                        if (btnSkipLogin != null) {
                            btnSkipLogin.setEnabled(true);
                        }

                        if (cloudTxList.isEmpty()) {
                            // No data in cloud, just auto-sync local data if enabled
                            triggerAutoSyncIfEnabled();
                            return;
                        }

                        if (localTxList.isEmpty()) {
                            // Local database is empty, auto-restore
                            restoreTransactionsToLocal(cloudTxList, "Cloud backup restored automatically.");
                        } else {
                            // Prompt user to choose between overwriting local data or keeping it
                            new AlertDialog.Builder(MainActivity.this, R.style.DarkAlertDialog)
                                    .setTitle("Cloud Backup Found")
                                    .setMessage("We found a cloud backup with " + cloudTxList.size() + " transactions. Would you like to restore it and replace your current local transactions?")
                                    .setPositiveButton("Restore Backup", (dialog, which) -> {
                                        restoreTransactionsToLocal(cloudTxList, "Backup restored successfully.");
                                    })
                                    .setNegativeButton("Keep Local", (dialog, which) -> {
                                        // Keep local. Auto-sync uploads it to cloud if auto-sync is enabled.
                                        triggerAutoSyncIfEnabled();
                                    })
                                    .setCancelable(false)
                                    .show();
                        }
                    });
                });
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                runOnUiThread(() -> {
                    if (btnSkipLogin != null) {
                        btnSkipLogin.setEnabled(true);
                    }
                    Toast.makeText(MainActivity.this, "Cloud check failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    triggerAutoSyncIfEnabled();
                });
            }
        });
    }

    private void restoreTransactionsToLocal(List<Transaction> transactions, String successMessage) {
        executorService.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            try {
                db.runInTransaction(() -> {
                    db.clearAllTables();
                    transactionDao.insertAll(transactions);
                });
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, successMessage, Toast.LENGTH_LONG).show();
                    updateProfileStats();
                    updateWalletBalances();
                    updateBudgetProgress();
                    updateBarChart();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Failed to restore: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // ==================== PREMIUM FEATURES LOGIC ====================

    // --- 1. App Passcode Lock ---
    private void setupPasscodeLock() {
        android.content.SharedPreferences prefs = getSharedPreferences("ExpenseTrackerPrefs", MODE_PRIVATE);
        boolean passcodeEnabled = prefs.getBoolean("passcode_enabled", false);
        String savedPin = prefs.getString("passcode_value", "");

        if (passcodeEnabled && !savedPin.isEmpty()) {
            layoutLock.setVisibility(View.VISIBLE);
            pinInput = "";
            updatePinDisplay();
        } else {
            layoutLock.setVisibility(View.GONE);
        }

        boolean biometricEnabled = prefs.getBoolean("biometric_enabled", false);
        ImageButton ibBiometricKey = findViewById(R.id.ibBiometricKey);
        if (ibBiometricKey != null) {
            if (passcodeEnabled && biometricEnabled && !savedPin.isEmpty()) {
                BiometricManager bm = BiometricManager.from(this);
                if (bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
                    ibBiometricKey.setVisibility(View.VISIBLE);
                    ibBiometricKey.setOnClickListener(v -> checkAndLaunchBiometrics());
                } else {
                    ibBiometricKey.setVisibility(View.GONE);
                }
            } else {
                ibBiometricKey.setVisibility(View.GONE);
            }
        }

        if (passcodeEnabled && !savedPin.isEmpty() && biometricEnabled) {
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::checkAndLaunchBiometrics, 200);
        }

        View.OnClickListener numListener = v -> {
            Button btn = (Button) v;
            if (pinInput.length() < 4) {
                pinInput += btn.getText().toString();
                updatePinDisplay();
                if (pinInput.length() == 4) {
                    String actualPin = prefs.getString("passcode_value", "");
                    if (pinInput.equals(actualPin)) {
                        layoutLock.setVisibility(View.GONE);
                        Toast.makeText(this, "Access Granted", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show();
                        pinInput = "";
                        updatePinDisplay();
                    }
                }
            }
        };

        int[] numButtons = {
                R.id.btnKey0, R.id.btnKey1, R.id.btnKey2, R.id.btnKey3,
                R.id.btnKey4, R.id.btnKey5, R.id.btnKey6, R.id.btnKey7,
                R.id.btnKey8, R.id.btnKey9
        };

        for (int id : numButtons) {
            View b = findViewById(id);
            if (b != null) b.setOnClickListener(numListener);
        }

        View clearBtn = findViewById(R.id.btnKeyClear);
        if (clearBtn != null) {
            clearBtn.setOnClickListener(v -> {
                pinInput = "";
                updatePinDisplay();
            });
        }

        View delBtn = findViewById(R.id.btnKeyDel);
        if (delBtn != null) {
            delBtn.setOnClickListener(v -> {
                if (!pinInput.isEmpty()) {
                    pinInput = pinInput.substring(0, pinInput.length() - 1);
                    updatePinDisplay();
                }
            });
        }
    }

    private void updatePinDisplay() {
        if (tvPinDisplay == null) return;
        int length = pinInput.length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            if (i < length) {
                sb.append("●");
            } else {
                sb.append("○");
            }
            if (i < 3) {
                sb.append("  ");
            }
        }
        tvPinDisplay.setText(sb.toString());
    }

    // --- 2. Advanced Search & Filter Chips ---
    private void setupSearchAndFilters() {
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    applySearchAndFilters();
                }
            });
        }

        if (btnChipAll != null) {
            btnChipAll.setOnClickListener(v -> {
                filterType = "All";
                filterWallet = "All";
                updateTypeChipsUI();
                updateWalletChipsUI();
                applySearchAndFilters();
            });
        }
        if (btnChipIncome != null) {
            btnChipIncome.setOnClickListener(v -> {
                toggleTypeFilter("Income");
                applySearchAndFilters();
            });
        }
        if (btnChipExpense != null) {
            btnChipExpense.setOnClickListener(v -> {
                toggleTypeFilter("Expense");
                applySearchAndFilters();
            });
        }

        if (btnChipCash != null) {
            btnChipCash.setOnClickListener(v -> {
                toggleWalletFilter("Cash");
                applySearchAndFilters();
            });
        }
        if (btnChipBank != null) {
            btnChipBank.setOnClickListener(v -> {
                toggleWalletFilter("Bank");
                applySearchAndFilters();
            });
        }
        if (btnChipCard != null) {
            btnChipCard.setOnClickListener(v -> {
                toggleWalletFilter("Card");
                applySearchAndFilters();
            });
        }

        updateTypeChipsUI();
        updateWalletChipsUI();
    }

    private void applySearchAndFilters() {
        List<Transaction> filtered = new ArrayList<>();
        String query = etSearch != null ? etSearch.getText().toString().trim().toLowerCase() : "";

        for (Transaction t : allTransactionsList) {
            boolean matchesQuery = true;
            if (!query.isEmpty()) {
                String cat = t.category != null ? t.category.toLowerCase() : "";
                String desc = t.description != null ? t.description.toLowerCase() : "";
                matchesQuery = cat.contains(query) || desc.contains(query);
            }

            boolean matchesType = true;
            if ("Income".equalsIgnoreCase(filterType)) {
                matchesType = "Income".equalsIgnoreCase(t.type);
            } else if ("Expense".equalsIgnoreCase(filterType)) {
                matchesType = "Expense".equalsIgnoreCase(t.type);
            }

            boolean matchesWallet = true;
            String w = t.wallet != null ? t.wallet : "Cash";
            if ("Cash".equalsIgnoreCase(filterWallet)) {
                matchesWallet = "Cash".equalsIgnoreCase(w);
            } else if ("Bank".equalsIgnoreCase(filterWallet)) {
                matchesWallet = "Bank".equalsIgnoreCase(w);
            } else if ("Card".equalsIgnoreCase(filterWallet)) {
                matchesWallet = "Card".equalsIgnoreCase(w);
            }

            if (matchesQuery && matchesType && matchesWallet) {
                filtered.add(t);
            }
        }

        // Apply sorting
        if ("Date: Newest First".equals(selectedSort)) {
            java.util.Collections.sort(filtered, (t1, t2) -> {
                if (t1.date == null || t2.date == null) return 0;
                int c = t2.date.compareTo(t1.date);
                if (c != 0) return c;
                return Integer.compare(t2.id, t1.id);
            });
        } else if ("Date: Oldest First".equals(selectedSort)) {
            java.util.Collections.sort(filtered, (t1, t2) -> {
                if (t1.date == null || t2.date == null) return 0;
                int c = t1.date.compareTo(t2.date);
                if (c != 0) return c;
                return Integer.compare(t1.id, t2.id);
            });
        } else if ("Amount: High to Low".equals(selectedSort)) {
            java.util.Collections.sort(filtered, (t1, t2) -> Double.compare(t2.amount, t1.amount));
        } else if ("Amount: Low to High".equals(selectedSort)) {
            java.util.Collections.sort(filtered, (t1, t2) -> Double.compare(t1.amount, t2.amount));
        }

        if (adapter != null) adapter.setTransactions(filtered);
        if (adapterHome != null) adapterHome.setTransactions(filtered);
        updateProfileStats();
    }

    private void toggleWalletFilter(String wallet) {
        if (filterWallet.equalsIgnoreCase(wallet)) {
            filterWallet = "All";
        } else {
            filterWallet = wallet;
            filterType = "All";
        }
        updateWalletChipsUI();
        updateTypeChipsUI();
    }

    private void toggleTypeFilter(String type) {
        if (filterType.equalsIgnoreCase(type)) {
            filterType = "All";
        } else {
            filterType = type;
            filterWallet = "All";
        }
        updateTypeChipsUI();
        updateWalletChipsUI();
    }

    private void updateTypeChipsUI() {
        int activeColor = ContextCompat.getColor(this, R.color.primary);
        int inactiveColor = ContextCompat.getColor(this, R.color.spinner_bg);
        int activeText = Color.WHITE;
        int inactiveText = ContextCompat.getColor(this, R.color.text_secondary);

        if (btnChipAll != null) {
            btnChipAll.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    "All".equalsIgnoreCase(filterType) ? activeColor : inactiveColor));
            btnChipAll.setTextColor("All".equalsIgnoreCase(filterType) ? activeText : inactiveText);
        }
        if (btnChipIncome != null) {
            btnChipIncome.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    "Income".equalsIgnoreCase(filterType) ? activeColor : inactiveColor));
            btnChipIncome.setTextColor("Income".equalsIgnoreCase(filterType) ? activeText : inactiveText);
        }
        if (btnChipExpense != null) {
            btnChipExpense.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    "Expense".equalsIgnoreCase(filterType) ? activeColor : inactiveColor));
            btnChipExpense.setTextColor("Expense".equalsIgnoreCase(filterType) ? activeText : inactiveText);
        }
    }

    private void updateWalletChipsUI() {
        int activeColor = ContextCompat.getColor(this, R.color.primary);
        int inactiveColor = ContextCompat.getColor(this, R.color.spinner_bg);
        int activeText = Color.WHITE;
        int inactiveText = ContextCompat.getColor(this, R.color.text_secondary);

        if (btnChipCash != null) {
            btnChipCash.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    "Cash".equalsIgnoreCase(filterWallet) ? activeColor : inactiveColor));
            btnChipCash.setTextColor("Cash".equalsIgnoreCase(filterWallet) ? activeText : inactiveText);
        }
        if (btnChipBank != null) {
            btnChipBank.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    "Bank".equalsIgnoreCase(filterWallet) ? activeColor : inactiveColor));
            btnChipBank.setTextColor("Bank".equalsIgnoreCase(filterWallet) ? activeText : inactiveText);
        }
        if (btnChipCard != null) {
            btnChipCard.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    "Card".equalsIgnoreCase(filterWallet) ? activeColor : inactiveColor));
            btnChipCard.setTextColor("Card".equalsIgnoreCase(filterWallet) ? activeText : inactiveText);
        }
    }

    // --- 3. Wallet Balances Support ---
    private void updateWalletBalances() {
        if (tvWalletCash == null || tvWalletBank == null || tvWalletCard == null) return;
        executorService.execute(() -> {
            List<Transaction> all = transactionDao.getAllTransactionsSync();
            double cash = 0, bank = 0, card = 0;
            for (Transaction t : all) {
                String w = t.wallet != null ? t.wallet : "Cash";
                double amt = t.amount;
                if ("Income".equals(t.type)) {
                    if ("Cash".equalsIgnoreCase(w)) cash += amt;
                    else if ("Bank".equalsIgnoreCase(w)) bank += amt;
                    else if ("Card".equalsIgnoreCase(w)) card += amt;
                } else if ("Expense".equals(t.type)) {
                    if ("Cash".equalsIgnoreCase(w)) cash -= amt;
                    else if ("Bank".equalsIgnoreCase(w)) bank -= amt;
                    else if ("Card".equalsIgnoreCase(w)) card -= amt;
                } else if ("Transfer".equals(t.type)) {
                    // Subtract from source wallet
                    if ("Cash".equalsIgnoreCase(w)) cash -= amt;
                    else if ("Bank".equalsIgnoreCase(w)) bank -= amt;
                    else if ("Card".equalsIgnoreCase(w)) card -= amt;
                    
                    // Add to destination wallet (stored in category field)
                    String dest = t.category != null ? t.category : "Cash";
                    if ("Cash".equalsIgnoreCase(dest)) cash += amt;
                    else if ("Bank".equalsIgnoreCase(dest)) bank += amt;
                    else if ("Card".equalsIgnoreCase(dest)) card += amt;
                }
            }
            final double fCash = cash;
            final double fBank = bank;
            final double fCard = card;
            runOnUiThread(() -> {
                tvWalletCash.setText("₹" + String.format(Locale.getDefault(), "%,.0f", fCash));
                tvWalletBank.setText("₹" + String.format(Locale.getDefault(), "%,.0f", fBank));
                tvWalletCard.setText("₹" + String.format(Locale.getDefault(), "%,.0f", fCard));
            });
        });
    }

    // --- 4. Budget Limits ---
    private void updateBudgetProgress() {
        if (cardBudget == null || tvBudgetStatus == null || tvBudgetMessage == null || pbBudget == null) return;
        android.content.SharedPreferences prefs = getSharedPreferences("ExpenseTrackerPrefs", MODE_PRIVATE);
        float budgetLimit = prefs.getFloat("budget_limit", 0);
        java.util.Set<String> activeCategoryBudgets = prefs.getStringSet("categories_with_budgets", new java.util.HashSet<>());

        boolean hasGlobal = budgetLimit > 0;
        boolean hasCategories = activeCategoryBudgets != null && !activeCategoryBudgets.isEmpty();

        if (!hasGlobal && !hasCategories) {
            cardBudget.setVisibility(View.GONE);
            if (hsvBudgetRings != null) hsvBudgetRings.setVisibility(View.GONE);
            TextView tvProfileBudget = findViewById(R.id.tvProfileBudgetLimit);
            if (tvProfileBudget != null) tvProfileBudget.setText("Not Set");
            return;
        }

        cardBudget.setVisibility(View.VISIBLE);
        TextView tvProfileBudget = findViewById(R.id.tvProfileBudgetLimit);
        if (tvProfileBudget != null) {
            tvProfileBudget.setText(hasGlobal ? "₹" + String.format(Locale.getDefault(), "%,.0f", budgetLimit) : "Not Set");
        }

        View pbBudgetContainer = findViewById(R.id.pbBudget);
        View tvBudgetStatusView = findViewById(R.id.tvBudgetStatus);
        View tvBudgetMessageView = findViewById(R.id.tvBudgetMessage);

        if (hasGlobal) {
            if (pbBudgetContainer != null) pbBudgetContainer.setVisibility(View.VISIBLE);
            if (tvBudgetStatusView != null) tvBudgetStatusView.setVisibility(View.VISIBLE);
            if (tvBudgetMessageView != null) tvBudgetMessageView.setVisibility(View.VISIBLE);

            double currentMonthExpense = currentExpense;
            tvBudgetStatus.setText(String.format(Locale.getDefault(), "₹%,.0f / ₹%,.0f", currentMonthExpense, budgetLimit));

            int progress = (int) ((currentMonthExpense / budgetLimit) * 100);
            pbBudget.setProgress(Math.min(progress, 100));

            int colorRes;
            String message;
            if (progress < 75) {
                colorRes = ContextCompat.getColor(this, R.color.income_green);
                message = "Within budget limit. Great job!";
            } else if (progress < 95) {
                colorRes = Color.parseColor("#FDCB6E");
                message = "Warning: Approaching your budget limit!";
            } else {
                colorRes = ContextCompat.getColor(this, R.color.expense_red);
                message = "Alert: You have exceeded/near-exceeded your budget limit!";
            }

            pbBudget.setProgressTintList(android.content.res.ColorStateList.valueOf(colorRes));
            tvBudgetMessage.setText(message);
            tvBudgetMessage.setTextColor(colorRes);
        } else {
            if (pbBudgetContainer != null) pbBudgetContainer.setVisibility(View.GONE);
            if (tvBudgetStatusView != null) tvBudgetStatusView.setVisibility(View.GONE);
            if (tvBudgetMessageView != null) tvBudgetMessageView.setVisibility(View.GONE);
        }

        LinearLayout llCategoryBudgets = findViewById(R.id.llCategoryBudgets);
        if (llCategoryBudgets != null) {
            llCategoryBudgets.removeAllViews();
            if (hasCategories) {
                for (String cat : activeCategoryBudgets) {
                    float catLimit = prefs.getFloat("budget_category_" + cat, 0);
                    if (catLimit <= 0) continue;

                    double catExpense = 0;
                    for (Transaction t : allTransactionsList) {
                        if ("Expense".equalsIgnoreCase(t.type) && cat.equalsIgnoreCase(t.category)) {
                            catExpense += t.amount;
                        }
                    }

                    View catView = LayoutInflater.from(this).inflate(R.layout.item_category_budget_home, llCategoryBudgets, false);
                    TextView tvCatName = catView.findViewById(R.id.tvCatBudgetName);
                    TextView tvCatStatus = catView.findViewById(R.id.tvCatBudgetStatus);
                    ProgressBar pbCat = catView.findViewById(R.id.pbCatBudget);

                    tvCatName.setText(cat);
                    tvCatStatus.setText(String.format(Locale.getDefault(), "₹%,.0f / ₹%,.0f", catExpense, catLimit));
                    int catProgress = (int) ((catExpense / catLimit) * 100);
                    pbCat.setProgress(Math.min(catProgress, 100));

                    int catColor;
                    if (catProgress < 75) {
                        catColor = ContextCompat.getColor(this, R.color.income_green);
                    } else if (catProgress < 100) {
                        catColor = Color.parseColor("#FDCB6E");
                    } else {
                        catColor = ContextCompat.getColor(this, R.color.expense_red);
                    }
                    pbCat.setProgressTintList(android.content.res.ColorStateList.valueOf(catColor));

                    llCategoryBudgets.addView(catView);
                }
            }
        }

        // ===== Populate Budget Rings Horizontal Scroll =====
        if (llBudgetRings != null && hsvBudgetRings != null) {
            llBudgetRings.removeAllViews();
            if (hasCategories) {
                int[] ringColors = {
                    Color.parseColor("#FF9800"), // Orange
                    Color.parseColor("#2196F3"), // Blue
                    Color.parseColor("#00BCD4"), // Teal
                    Color.parseColor("#E91E63"), // Pink
                    Color.parseColor("#9C27B0"), // Purple
                    Color.parseColor("#4CAF50"), // Green
                    Color.parseColor("#FF5722"), // Deep Orange
                    Color.parseColor("#607D8B")  // Blue Grey
                };
                int ringIdx = 0;
                for (String cat : activeCategoryBudgets) {
                    float catLimit = prefs.getFloat("budget_category_" + cat, 0);
                    if (catLimit <= 0) continue;

                    double catExpense = 0;
                    for (Transaction t : allTransactionsList) {
                        if ("Expense".equalsIgnoreCase(t.type) && cat.equalsIgnoreCase(t.category)) {
                            catExpense += t.amount;
                        }
                    }

                    int catProgress = (int) ((catExpense / catLimit) * 100);
                    int ringColor = ringColors[ringIdx % ringColors.length];
                    ringIdx++;

                    View ringView = LayoutInflater.from(this).inflate(R.layout.item_budget_ring, llBudgetRings, false);
                    ProgressBar pbRing = ringView.findViewById(R.id.pbRing);
                    TextView tvPercent = ringView.findViewById(R.id.tvRingPercent);
                    TextView tvCategory = ringView.findViewById(R.id.tvRingCategory);
                    TextView tvAmount = ringView.findViewById(R.id.tvRingAmount);

                    tvPercent.setText(Math.min(catProgress, 100) + "%");
                    tvCategory.setText(cat);
                    tvAmount.setText(String.format(Locale.getDefault(), "₹%,.0f / ₹%,.0f", catExpense, catLimit));

                    // Configure ring progress drawable programmatically
                    android.graphics.drawable.GradientDrawable trackBg = new android.graphics.drawable.GradientDrawable();
                    trackBg.setShape(android.graphics.drawable.GradientDrawable.RING);
                    trackBg.setUseLevel(false);
                    trackBg.setColor(Color.parseColor("#20808080"));
                    trackBg.setCornerRadius(999f);

                    pbRing.setMax(100);
                    pbRing.setProgress(Math.min(catProgress, 100));
                    pbRing.setProgressTintList(android.content.res.ColorStateList.valueOf(ringColor));
                    pbRing.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#20808080")));

                    // Color the percentage text
                    if (catProgress >= 100) {
                        tvPercent.setTextColor(ContextCompat.getColor(this, R.color.expense_red));
                    } else if (catProgress >= 75) {
                        tvPercent.setTextColor(Color.parseColor("#FDCB6E"));
                    } else {
                        tvPercent.setTextColor(ringColor);
                    }

                    llBudgetRings.addView(ringView);
                }
                hsvBudgetRings.setVisibility(llBudgetRings.getChildCount() > 0 ? View.VISIBLE : View.GONE);
            } else {
                hsvBudgetRings.setVisibility(View.GONE);
            }
        }
    }

    // --- 5. MPAndroidChart LineChart Savings Rate ---
    private void setupLineChart() {
        if (lineChart == null) return;
        lineChart.getDescription().setEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(false);
        lineChart.setScaleEnabled(false);
        lineChart.setPinchZoom(false);
        lineChart.setDoubleTapToZoomEnabled(false);

        com.github.mikephil.charting.components.XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.parseColor("#7E808C"));

        com.github.mikephil.charting.components.YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#F0F0F0"));
        leftAxis.setTextColor(Color.parseColor("#7E808C"));
        leftAxis.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.0f%%", value);
            }
        });

        lineChart.getAxisRight().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
    }

    // --- 5.5. MPAndroidChart BarChart MoM Trends ---
    private void setupBarChart() {
        if (barChart == null) return;
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);
        barChart.setMaxVisibleValueCount(50);
        barChart.setPinchZoom(false);
        barChart.setDoubleTapToZoomEnabled(false);
        barChart.setScaleEnabled(false);

        com.github.mikephil.charting.components.XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.parseColor("#7E808C"));
        xAxis.setCenterAxisLabels(true);

        com.github.mikephil.charting.components.YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#F0F0F0"));
        leftAxis.setTextColor(Color.parseColor("#7E808C"));
        leftAxis.setAxisMinimum(0f);

        barChart.getAxisRight().setEnabled(false);

        com.github.mikephil.charting.components.Legend legend = barChart.getLegend();
        legend.setVerticalAlignment(com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setForm(com.github.mikephil.charting.components.Legend.LegendForm.SQUARE);
        legend.setFormSize(9f);
        legend.setTextSize(11f);
        legend.setXEntrySpace(4f);
    }

    private void updateBarChart() {
        if (barChart == null) return;
        executorService.execute(() -> {
            int fyStartYear;
            try {
                fyStartYear = Integer.parseInt(selectedFY.split("-")[0]);
            } catch (Exception e) {
                fyStartYear = Calendar.getInstance().get(Calendar.YEAR);
            }

            // Calculate start date of FY: April 1st
            String fyStartDate = fyStartYear + "-04-01";
            
            // Calculate end date of selected month/range
            String fyEndDate;
            int endMonthIndex = (selectedMonthIndex == 0) ? 12 : selectedMonthIndex;
            if (selectedMonthIndex == 0) {
                fyEndDate = (fyStartYear + 1) + "-03-31";
            } else {
                int calendarMonth;
                int year;
                if (selectedMonthIndex <= 9) {
                    calendarMonth = selectedMonthIndex + 3;
                    year = fyStartYear;
                } else {
                    calendarMonth = selectedMonthIndex - 9;
                    year = fyStartYear + 1;
                }
                Calendar cal = Calendar.getInstance();
                cal.set(year, calendarMonth - 1, 1);
                int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                fyEndDate = String.format(Locale.getDefault(), "%d-%02d-%02d", year, calendarMonth, lastDay);
            }

            List<Transaction> transactions = transactionDao.getTransactionsByDateRangeSync(fyStartDate, fyEndDate);

            List<MonthAggregate> months = new ArrayList<>();
            for (int i = 1; i <= endMonthIndex; i++) {
                int calendarMonth;
                int year;
                if (i <= 9) {
                    calendarMonth = i + 3; // April(1) = month 4, Dec(9) = month 12
                    year = fyStartYear;
                } else {
                    calendarMonth = i - 9; // Jan(10) = month 1, Mar(12) = month 3
                    year = fyStartYear + 1;
                }

                MonthAggregate m = new MonthAggregate();
                Calendar tempCal = Calendar.getInstance();
                tempCal.set(year, calendarMonth - 1, 1);
                m.label = new SimpleDateFormat("MMM", Locale.getDefault()).format(tempCal.getTime());
                m.yearMonth = String.format(Locale.getDefault(), "%d-%02d", year, calendarMonth);
                months.add(m);
            }

            for (Transaction t : transactions) {
                if (t.date == null) continue;
                for (MonthAggregate m : months) {
                    if (t.date.startsWith(m.yearMonth)) {
                        if ("Income".equalsIgnoreCase(t.type)) {
                            m.income += t.amount;
                        } else {
                            m.expense += t.amount;
                        }
                        break;
                    }
                }
            }

            List<com.github.mikephil.charting.data.BarEntry> incomeEntries = new ArrayList<>();
            List<com.github.mikephil.charting.data.BarEntry> expenseEntries = new ArrayList<>();
            List<com.github.mikephil.charting.data.Entry> lineEntries = new ArrayList<>();

            for (int i = 0; i < months.size(); i++) {
                MonthAggregate m = months.get(i);
                incomeEntries.add(new com.github.mikephil.charting.data.BarEntry(i, (float) m.income));
                expenseEntries.add(new com.github.mikephil.charting.data.BarEntry(i, (float) m.expense));
                float savingsRate = 0f;
                if (m.income > 0) {
                    savingsRate = (float) (((m.income - m.expense) / m.income) * 100);
                }
                lineEntries.add(new com.github.mikephil.charting.data.Entry(i, savingsRate));
            }

            runOnUiThread(() -> {
                com.github.mikephil.charting.data.BarDataSet incomeSet = new com.github.mikephil.charting.data.BarDataSet(incomeEntries, "Income");
                incomeSet.setColor(ContextCompat.getColor(this, R.color.income_green));
                incomeSet.setValueTextSize(9f);
                incomeSet.setValueTextColor(Color.parseColor("#7E808C"));

                com.github.mikephil.charting.data.BarDataSet expenseSet = new com.github.mikephil.charting.data.BarDataSet(expenseEntries, "Expense");
                expenseSet.setColor(ContextCompat.getColor(this, R.color.expense_red));
                expenseSet.setValueTextSize(9f);
                expenseSet.setValueTextColor(Color.parseColor("#7E808C"));

                com.github.mikephil.charting.data.BarData barData = new com.github.mikephil.charting.data.BarData(incomeSet, expenseSet);
                barData.setBarWidth(0.35f);

                barChart.setData(barData);
                barChart.groupBars(0f, 0.2f, 0.05f);

                barChart.getXAxis().setAxisMinimum(0f);
                barChart.getXAxis().setAxisMaximum(months.size());
                barChart.getXAxis().setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        int index = Math.round(value);
                        if (index >= 0 && index < months.size()) {
                            return months.get(index).label;
                        }
                        return "";
                    }
                });

                barChart.animateY(800);
                barChart.invalidate();

                if (lineChart != null) {
                    com.github.mikephil.charting.data.LineDataSet lineDataSet = new com.github.mikephil.charting.data.LineDataSet(lineEntries, "Savings Rate");
                    lineDataSet.setColor(Color.parseColor("#00CEC9"));
                    lineDataSet.setLineWidth(3f);
                    lineDataSet.setCircleColor(Color.parseColor("#00CEC9"));
                    lineDataSet.setCircleRadius(5f);
                    lineDataSet.setDrawCircleHole(true);
                    lineDataSet.setCircleHoleRadius(3f);
                    lineDataSet.setCircleHoleColor(Color.WHITE);
                    lineDataSet.setValueTextSize(9f);
                    lineDataSet.setValueTextColor(Color.parseColor("#7E808C"));
                    lineDataSet.setDrawFilled(true);
                    
                    lineDataSet.setMode(com.github.mikephil.charting.data.LineDataSet.Mode.CUBIC_BEZIER);

                    if (android.os.Build.VERSION.SDK_INT >= 18) {
                        try {
                            android.graphics.drawable.Drawable drawable = ContextCompat.getDrawable(MainActivity.this, R.drawable.circle_icon_bg);
                            if (drawable != null) {
                                drawable.setAlpha(30);
                                lineDataSet.setFillDrawable(drawable);
                            }
                        } catch (Exception ignored) {}
                    } else {
                        lineDataSet.setFillColor(Color.parseColor("#00CEC9"));
                    }

                    lineDataSet.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
                        @Override
                        public String getFormattedValue(float value) {
                            return String.format(Locale.getDefault(), "%.1f%%", value);
                        }
                    });

                    com.github.mikephil.charting.data.LineData lineData = new com.github.mikephil.charting.data.LineData(lineDataSet);
                    lineChart.setData(lineData);

                    lineChart.getXAxis().setAxisMinimum(0f);
                    lineChart.getXAxis().setAxisMaximum(months.size() - 1);
                    lineChart.getXAxis().setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
                        @Override
                        public String getFormattedValue(float value) {
                            int index = Math.round(value);
                            if (index >= 0 && index < months.size()) {
                                return months.get(index).label;
                            }
                            return "";
                        }
                    });

                    lineChart.animateX(800);
                    lineChart.invalidate();
                }
            });
        });
    }

    private static class MonthAggregate {
        String label;
        String yearMonth;
        double income = 0;
        double expense = 0;
    }

    // --- 6. Auto-Recurring Engine ---
    private void checkAndRunRecurringTransactions() {
        executorService.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            RecurringTransactionDao recDao = db.recurringTransactionDao();
            List<RecurringTransaction> schedules = recDao.getAllSync();
            if (schedules == null || schedules.isEmpty()) return;

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            boolean insertedAny = false;

            for (RecurringTransaction item : schedules) {
                Calendar checkCal = Calendar.getInstance();
                if (item.lastAddedDate == null || item.lastAddedDate.isEmpty()) {
                    checkCal.add(Calendar.DAY_OF_YEAR, -1);
                } else {
                    try {
                        Date d = sdf.parse(item.lastAddedDate);
                        if (d != null) {
                            checkCal.setTime(d);
                        } else {
                            checkCal.add(Calendar.DAY_OF_YEAR, -1);
                        }
                    } catch (Exception e) {
                        checkCal.add(Calendar.DAY_OF_YEAR, -1);
                    }
                }
                
                checkCal.set(Calendar.HOUR_OF_DAY, 0);
                checkCal.set(Calendar.MINUTE, 0);
                checkCal.set(Calendar.SECOND, 0);
                checkCal.set(Calendar.MILLISECOND, 0);

                while (checkCal.before(today)) {
                    checkCal.add(Calendar.DAY_OF_YEAR, 1);
                    String checkDateStr = sdf.format(checkCal.getTime());
                    
                    boolean shouldTrigger = false;
                    String freq = item.frequency != null ? item.frequency : "Monthly";
                    
                    if ("Daily".equalsIgnoreCase(freq)) {
                        shouldTrigger = true;
                    } else if ("Weekly".equalsIgnoreCase(freq)) {
                        int checkDayOfWeek = checkCal.get(Calendar.DAY_OF_WEEK);
                        if (checkDayOfWeek == item.dayOfWeek) {
                            shouldTrigger = true;
                        }
                    } else if ("Monthly".equalsIgnoreCase(freq)) {
                        int checkDayOfMonth = checkCal.get(Calendar.DAY_OF_MONTH);
                        int maxDays = checkCal.getActualMaximum(Calendar.DAY_OF_MONTH);
                        if (checkDayOfMonth == item.dayOfMonth || (item.dayOfMonth > maxDays && checkDayOfMonth == maxDays)) {
                            shouldTrigger = true;
                        }
                    }

                    if (shouldTrigger) {
                        Transaction newTx = new Transaction(
                                checkDateStr,
                                item.type,
                                item.category,
                                item.amount,
                                item.description != null && !item.description.trim().isEmpty()
                                        ? item.description + " (Auto-Recurring)"
                                        : "Auto-Recurring Schedule",
                                item.wallet
                        );
                        transactionDao.insert(newTx);
                        item.lastAddedDate = checkDateStr;
                        recDao.update(item);
                        insertedAny = true;
                    }
                }
                
                if (item.lastAddedDate == null || item.lastAddedDate.isEmpty()) {
                    Calendar yesterday = Calendar.getInstance();
                    yesterday.add(Calendar.DAY_OF_YEAR, -1);
                    item.lastAddedDate = sdf.format(yesterday.getTime());
                    recDao.update(item);
                }
            }

            if (insertedAny) {
                triggerAutoSyncIfEnabled();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Recurring transactions added successfully!", Toast.LENGTH_SHORT).show();
                    applyFilters();
                    updateWalletBalances();
                    updateBudgetProgress();
                    updateBarChart();
                });
            }
        });
    }

    // --- 7. Preferences Setup ---
    private void setupProfilePreferences() {
        SwitchMaterial switchPasscode = findViewById(R.id.switchPasscode);
        View rlSetBudget = findViewById(R.id.rlSetBudget);
        View rlManageRecurring = findViewById(R.id.rlManageRecurring);
        SwitchMaterial switchBiometric = findViewById(R.id.switchBiometric);
        View rlThemeSelect = findViewById(R.id.rlThemeSelect);

        if (switchPasscode != null) {
            switchPasscode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                android.content.SharedPreferences prefs = getSharedPreferences("ExpenseTrackerPrefs", MODE_PRIVATE);
                boolean currentlyEnabled = prefs.getBoolean("passcode_enabled", false);
                if (isChecked) {
                    if (!currentlyEnabled) {
                        showPasscodeSetupDialog(switchPasscode);
                    }
                } else {
                    if (currentlyEnabled) {
                        prefs.edit()
                             .putBoolean("passcode_enabled", false)
                             .putBoolean("biometric_enabled", false)
                             .apply();
                        updateProfilePreferencesUI();
                        Toast.makeText(this, "Passcode lock disabled", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (switchBiometric != null) {
            switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
                android.content.SharedPreferences prefs = getSharedPreferences("ExpenseTrackerPrefs", MODE_PRIVATE);
                boolean currentlyEnabled = prefs.getBoolean("biometric_enabled", false);
                if (isChecked) {
                    if (!currentlyEnabled) {
                        BiometricManager bm = BiometricManager.from(this);
                        int canAuth = bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
                        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
                            Toast.makeText(this, "Biometrics not available or not enrolled on this device", Toast.LENGTH_LONG).show();
                            switchBiometric.setChecked(false);
                            return;
                        }

                        java.util.concurrent.Executor executor = ContextCompat.getMainExecutor(this);
                        BiometricPrompt bp = new BiometricPrompt(this, executor,
                                new BiometricPrompt.AuthenticationCallback() {
                                    @Override
                                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                                        super.onAuthenticationSucceeded(result);
                                        runOnUiThread(() -> {
                                            prefs.edit().putBoolean("biometric_enabled", true).apply();
                                            updateProfilePreferencesUI();
                                            Toast.makeText(MainActivity.this, "Biometric Unlock enabled!", Toast.LENGTH_SHORT).show();
                                        });
                                    }

                                    @Override
                                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                                        super.onAuthenticationError(errorCode, errString);
                                        runOnUiThread(() -> {
                                            switchBiometric.setChecked(false);
                                        });
                                    }
                                });

                        BiometricPrompt.PromptInfo pi = new BiometricPrompt.PromptInfo.Builder()
                                .setTitle("Verify Identity")
                                .setSubtitle("Authenticate to enable Biometric Unlock")
                                .setNegativeButtonText("Cancel")
                                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                                .build();

                        bp.authenticate(pi);
                    }
                } else {
                    if (currentlyEnabled) {
                        prefs.edit().putBoolean("biometric_enabled", false).apply();
                        updateProfilePreferencesUI();
                        Toast.makeText(this, "Biometric Unlock disabled", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (rlThemeSelect != null) {
            rlThemeSelect.setOnClickListener(v -> showThemeSelectionDialog());
        }

        if (rlSetBudget != null) {
            rlSetBudget.setOnClickListener(v -> showBudgetSetupDialog());
        }

        if (rlManageRecurring != null) {
            rlManageRecurring.setOnClickListener(v -> showManageRecurringDialog());
        }

        updateProfilePreferencesUI();
        updateBudgetProgress();
    }

    private void updateProfilePreferencesUI() {
        android.content.SharedPreferences prefs = getSharedPreferences("ExpenseTrackerPrefs", MODE_PRIVATE);
        boolean passcodeEnabled = prefs.getBoolean("passcode_enabled", false);
        SwitchMaterial switchPasscode = findViewById(R.id.switchPasscode);
        TextView tvPasscodeStatus = findViewById(R.id.tvPasscodeStatus);

        if (switchPasscode != null) {
            switchPasscode.setChecked(passcodeEnabled);
        }
        if (tvPasscodeStatus != null) {
            tvPasscodeStatus.setText(passcodeEnabled ? "Passcode PIN Lock is ACTIVE" : "Protect your data on launch");
        }

        // Biometrics UI update
        boolean biometricEnabled = prefs.getBoolean("biometric_enabled", false);
        SwitchMaterial switchBiometric = findViewById(R.id.switchBiometric);
        TextView tvBiometricStatus = findViewById(R.id.tvBiometricStatus);
        if (switchBiometric != null) {
            switchBiometric.setEnabled(passcodeEnabled);
            switchBiometric.setChecked(biometricEnabled && passcodeEnabled);
        }
        if (tvBiometricStatus != null) {
            if (!passcodeEnabled) {
                tvBiometricStatus.setText("Requires Passcode PIN Lock first");
            } else {
                tvBiometricStatus.setText(biometricEnabled ? "Biometric Unlock is ACTIVE" : "Fingerprint or face recognition");
            }
        }

        // Theme UI update
        String selectedTheme = prefs.getString("selected_theme", "royal_violet");
        TextView tvCurrentTheme = findViewById(R.id.tvCurrentTheme);
        if (tvCurrentTheme != null) {
            String themeDisplayName = "Royal Violet";
            if ("ocean_breeze".equals(selectedTheme)) themeDisplayName = "Ocean Breeze";
            else if ("midnight_charcoal".equals(selectedTheme)) themeDisplayName = "Midnight Charcoal";
            else if ("sunset_gold".equals(selectedTheme)) themeDisplayName = "Sunset Gold";
            tvCurrentTheme.setText(themeDisplayName);
        }

        float budgetLimit = prefs.getFloat("budget_limit", 0);
        TextView tvProfileBudget = findViewById(R.id.tvProfileBudgetLimit);
        if (tvProfileBudget != null) {
            tvProfileBudget.setText(budgetLimit > 0
                    ? "₹" + String.format(Locale.getDefault(), "%,.0f", budgetLimit)
                    : "Not Set");
        }
    }

    private void showThemeSelectionDialog() {
        String[] themes = {"Royal Violet", "Ocean Breeze", "Midnight Charcoal", "Sunset Gold"};
        String[] themeKeys = {"royal_violet", "ocean_breeze", "midnight_charcoal", "sunset_gold"};
        
        android.content.SharedPreferences prefs = getSharedPreferences("ExpenseTrackerPrefs", MODE_PRIVATE);
        String currentTheme = prefs.getString("selected_theme", "royal_violet");
        int checkedItem = 0;
        for (int i = 0; i < themeKeys.length; i++) {
            if (themeKeys[i].equals(currentTheme)) {
                checkedItem = i;
                break;
            }
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DarkAlertDialog);
        builder.setTitle("Select App Theme");
        builder.setSingleChoiceItems(themes, checkedItem, (dialog, which) -> {
            prefs.edit().putString("selected_theme", themeKeys[which]).apply();
            dialog.dismiss();
            recreate();
        });
        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.show();
        styleDialogButtons(dialog);
    }

    private void checkAndLaunchBiometrics() {
        android.content.SharedPreferences prefs = getSharedPreferences("ExpenseTrackerPrefs", MODE_PRIVATE);
        boolean passcodeEnabled = prefs.getBoolean("passcode_enabled", false);
        boolean biometricEnabled = prefs.getBoolean("biometric_enabled", false);
        String savedPin = prefs.getString("passcode_value", "");

        if (passcodeEnabled && !savedPin.isEmpty() && biometricEnabled) {
            BiometricManager biometricManager = BiometricManager.from(this);
            int canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
            if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
                java.util.concurrent.Executor executor = ContextCompat.getMainExecutor(this);
                BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
                        new BiometricPrompt.AuthenticationCallback() {
                            @Override
                            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                                super.onAuthenticationError(errorCode, errString);
                            }

                            @Override
                            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                                super.onAuthenticationSucceeded(result);
                                runOnUiThread(() -> {
                                    if (layoutLock != null) {
                                        layoutLock.setVisibility(View.GONE);
                                        Toast.makeText(MainActivity.this, "Access Granted via Biometrics", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            @Override
                            public void onAuthenticationFailed() {
                                super.onAuthenticationFailed();
                            }
                        });

                BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Biometric Unlock")
                        .setSubtitle("Authenticate using fingerprint or face recognition")
                        .setNegativeButtonText("Use PIN")
                        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                        .build();

                biometricPrompt.authenticate(promptInfo);
            }
        }
    }

    private void showPasscodeSetupDialog(com.google.android.material.switchmaterial.SwitchMaterial switchPasscode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DarkAlertDialog);
        builder.setTitle("Set 4-Digit PIN");
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_custom_category, null);
        builder.setView(view);

        EditText etPin = view.findViewById(R.id.etCustomCategoryName);
        etPin.setHint("Enter 4-Digit PIN");
        etPin.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String pin = etPin.getText().toString();
            if (pin.length() == 4 && pin.matches("\\d{4}")) {
                getSharedPreferences("ExpenseTrackerPrefs", MODE_PRIVATE)
                        .edit()
                        .putBoolean("passcode_enabled", true)
                        .putString("passcode_value", pin)
                        .apply();
                Toast.makeText(this, "PIN saved successfully!", Toast.LENGTH_SHORT).show();
                updateProfilePreferencesUI();
            } else {
                Toast.makeText(this, "PIN must be exactly 4 digits", Toast.LENGTH_SHORT).show();
                switchPasscode.setChecked(false);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            switchPasscode.setChecked(false);
        });

        AlertDialog d = builder.show();
        styleDialogButtons(d);
    }

    private void showBudgetSetupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DarkAlertDialog);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_setup_budget, null);
        builder.setView(view);

        EditText etGlobalBudget = view.findViewById(R.id.etGlobalBudget);
        Button btnSaveGlobalBudget = view.findViewById(R.id.btnSaveGlobalBudget);
        Spinner spinBudgetCategory = view.findViewById(R.id.spinBudgetCategory);
        EditText etCategoryBudget = view.findViewById(R.id.etCategoryBudget);
        Button btnSaveCategoryBudget = view.findViewById(R.id.btnSaveCategoryBudget);
        RecyclerView rvCategoryBudgetsList = view.findViewById(R.id.rvCategoryBudgetsList);

        android.content.SharedPreferences prefs = getSharedPreferences("ExpenseTrackerPrefs", MODE_PRIVATE);

        // 1. Global budget logic
        float globalLimit = prefs.getFloat("budget_limit", 0);
        if (globalLimit > 0) {
            etGlobalBudget.setText(String.format(Locale.getDefault(), "%.0f", globalLimit));
        }
        btnSaveGlobalBudget.setOnClickListener(v -> {
            String val = etGlobalBudget.getText().toString().trim();
            try {
                float limit = val.isEmpty() ? 0 : Float.parseFloat(val);
                prefs.edit().putFloat("budget_limit", limit).apply();
                Toast.makeText(this, "Global budget saved!", Toast.LENGTH_SHORT).show();
                updateProfilePreferencesUI();
                updateBudgetProgress();
            } catch (Exception e) {
                Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            }
        });

        // 2. Category list spinner setup
        List<String> expenseCategoriesList = new ArrayList<>();
        for (String c : expenseCategories) {
            expenseCategoriesList.add(c);
        }
        java.util.Set<String> custom = prefs.getStringSet("custom_expense", null);
        if (custom != null) {
            for (String c : custom) {
                if (!expenseCategoriesList.contains(c)) {
                    expenseCategoriesList.add(c);
                }
            }
        }
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, expenseCategoriesList);
        catAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinBudgetCategory.setAdapter(catAdapter);

        // 3. Category budget list setup
        rvCategoryBudgetsList.setLayoutManager(new LinearLayoutManager(this));
        final List<String> activeCats = new ArrayList<>(prefs.getStringSet("categories_with_budgets", new java.util.HashSet<>()));
        final CategoryBudgetSetupAdapter[] setupAdapterHolder = new CategoryBudgetSetupAdapter[1];

        CategoryBudgetSetupAdapter.OnDeleteClickListener deleteClick = new CategoryBudgetSetupAdapter.OnDeleteClickListener() {
            @Override
            public void onDelete(String category) {
                prefs.edit().remove("budget_category_" + category).apply();
                java.util.Set<String> set = new java.util.HashSet<>(prefs.getStringSet("categories_with_budgets", new java.util.HashSet<>()));
                set.remove(category);
                prefs.edit().putStringSet("categories_with_budgets", set).apply();
                activeCats.remove(category);
                if (setupAdapterHolder[0] != null) {
                    setupAdapterHolder[0].notifyDataSetChanged();
                }
                updateBudgetProgress();
            }
        };

        CategoryBudgetSetupAdapter setupAdapter = new CategoryBudgetSetupAdapter(activeCats, prefs, deleteClick);
        setupAdapterHolder[0] = setupAdapter;
        rvCategoryBudgetsList.setAdapter(setupAdapter);

        btnSaveCategoryBudget.setOnClickListener(v -> {
            String category = spinBudgetCategory.getSelectedItem().toString();
            String val = etCategoryBudget.getText().toString().trim();
            if (val.isEmpty()) {
                Toast.makeText(this, "Enter amount", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                float limit = Float.parseFloat(val);
                prefs.edit().putFloat("budget_category_" + category, limit).apply();
                java.util.Set<String> set = new java.util.HashSet<>(prefs.getStringSet("categories_with_budgets", new java.util.HashSet<>()));
                set.add(category);
                prefs.edit().putStringSet("categories_with_budgets", set).apply();
                
                if (!activeCats.contains(category)) {
                    activeCats.add(category);
                }
                setupAdapter.notifyDataSetChanged();
                etCategoryBudget.setText("");
                Toast.makeText(this, "Category budget saved!", Toast.LENGTH_SHORT).show();
                updateBudgetProgress();
            } catch (Exception e) {
                Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Close", null);
        AlertDialog d = builder.show();
        styleDialogButtons(d);
    }

    private static class CategoryBudgetSetupAdapter extends RecyclerView.Adapter<CategoryBudgetSetupAdapter.VH> {
        interface OnDeleteClickListener {
            void onDelete(String category);
        }
        private final List<String> categories;
        private final android.content.SharedPreferences prefs;
        private final OnDeleteClickListener deleteListener;

        CategoryBudgetSetupAdapter(List<String> categories, android.content.SharedPreferences prefs, OnDeleteClickListener deleteListener) {
            this.categories = categories;
            this.prefs = prefs;
            this.deleteListener = deleteListener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_budget_setup, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            String cat = categories.get(position);
            holder.tvCat.setText(cat);
            float amt = prefs.getFloat("budget_category_" + cat, 0);
            holder.tvAmt.setText("Budget: ₹" + String.format(Locale.getDefault(), "%,.0f", amt));
            holder.btnDel.setOnClickListener(v -> deleteListener.onDelete(cat));
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvCat, tvAmt;
            ImageButton btnDel;
            VH(View itemView) {
                super(itemView);
                tvCat = itemView.findViewById(R.id.tvBudgetSetupCategory);
                tvAmt = itemView.findViewById(R.id.tvBudgetSetupAmount);
                btnDel = itemView.findViewById(R.id.btnDeleteCatBudget);
            }
        }
    }

    private void showManageRecurringDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DarkAlertDialog);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_manage_recurring, null);
        builder.setView(view);

        Spinner spinRecType = view.findViewById(R.id.spinRecType);
        Spinner spinRecWallet = view.findViewById(R.id.spinRecWallet);
        AutoCompleteTextView etRecCategory = view.findViewById(R.id.etRecCategory);
        EditText etRecAmount = view.findViewById(R.id.etRecAmount);
        EditText etRecDay = view.findViewById(R.id.etRecDay);
        Spinner spinRecFrequency = view.findViewById(R.id.spinRecFrequency);
        Spinner spinRecDayOfWeek = view.findViewById(R.id.spinRecDayOfWeek);
        EditText etRecDescription = view.findViewById(R.id.etRecDescription);
        Button btnRecAdd = view.findViewById(R.id.btnRecAdd);
        RecyclerView rvRecurringList = view.findViewById(R.id.rvRecurringList);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, new String[]{"Expense", "Income"});
        typeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinRecType.setAdapter(typeAdapter);

        ArrayAdapter<String> walletAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, new String[]{"Cash", "Bank", "Card"});
        walletAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinRecWallet.setAdapter(walletAdapter);

        ArrayAdapter<String> freqAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, new String[]{"Monthly", "Weekly", "Daily"});
        freqAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinRecFrequency.setAdapter(freqAdapter);

        String[] daysOfWeekNames = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        ArrayAdapter<String> dowAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, daysOfWeekNames);
        dowAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinRecDayOfWeek.setAdapter(dowAdapter);

        spinRecFrequency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                String selectedFreq = parent.getItemAtPosition(position).toString();
                if ("Monthly".equalsIgnoreCase(selectedFreq)) {
                    etRecDay.setVisibility(View.VISIBLE);
                    spinRecDayOfWeek.setVisibility(View.GONE);
                } else if ("Weekly".equalsIgnoreCase(selectedFreq)) {
                    etRecDay.setVisibility(View.GONE);
                    spinRecDayOfWeek.setVisibility(View.VISIBLE);
                } else {
                    etRecDay.setVisibility(View.GONE);
                    spinRecDayOfWeek.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        ArrayAdapter<String> expenseAdapter = new ArrayAdapter<>(this, R.layout.spinner_dropdown_item, getCategoriesList(false));
        ArrayAdapter<String> incomeAdapter = new ArrayAdapter<>(this, R.layout.spinner_dropdown_item, getCategoriesList(true));
        etRecCategory.setAdapter(expenseAdapter);

        spinRecType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = parent.getItemAtPosition(position).toString();
                if ("Income".equalsIgnoreCase(selectedType)) {
                    etRecCategory.setAdapter(incomeAdapter);
                } else {
                    etRecCategory.setAdapter(expenseAdapter);
                }
                etRecCategory.setText("");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        rvRecurringList.setLayoutManager(new LinearLayoutManager(this));
        RecurringTransactionDao recDao = AppDatabase.getInstance(this).recurringTransactionDao();

        final RecurringTransactionAdapter[] recAdapterHolder = new RecurringTransactionAdapter[1];
        RecurringTransactionAdapter recAdapter = new RecurringTransactionAdapter(item -> {
            new AlertDialog.Builder(this, R.style.DarkAlertDialog)
                    .setTitle("Delete Schedule")
                    .setMessage("Are you sure you want to delete this schedule?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        executorService.execute(() -> {
                            recDao.delete(item);
                            List<RecurringTransaction> updatedList = recDao.getAllSync();
                            runOnUiThread(() -> {
                                if (recAdapterHolder[0] != null) {
                                    recAdapterHolder[0].setItems(updatedList);
                                }
                            });
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        recAdapterHolder[0] = recAdapter;
        rvRecurringList.setAdapter(recAdapter);

        executorService.execute(() -> {
            List<RecurringTransaction> list = recDao.getAllSync();
            runOnUiThread(() -> recAdapter.setItems(list));
        });

        AlertDialog dialog = builder.create();

        btnRecAdd.setOnClickListener(v -> {
            String type = spinRecType.getSelectedItem().toString();
            String wallet = spinRecWallet.getSelectedItem().toString();
            String category = etRecCategory.getText().toString().trim();
            String amountStr = etRecAmount.getText().toString().trim();
            String desc = etRecDescription.getText().toString().trim();
            String freq = spinRecFrequency.getSelectedItem().toString();

            if (category.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
                return;
            }

            int dayOfMonth = 1;
            int dayOfWeek = 1;

            if ("Monthly".equalsIgnoreCase(freq)) {
                String dayStr = etRecDay.getText().toString().trim();
                if (dayStr.isEmpty()) {
                    Toast.makeText(this, "Please specify day of month", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    dayOfMonth = Integer.parseInt(dayStr);
                    if (dayOfMonth < 1 || dayOfMonth > 31) {
                        Toast.makeText(this, "Day must be between 1 and 31", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid day of month", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else if ("Weekly".equalsIgnoreCase(freq)) {
                dayOfWeek = spinRecDayOfWeek.getSelectedItemPosition() + 1;
            }

            final int finalDayOfMonth = dayOfMonth;
            final int finalDayOfWeek = dayOfWeek;

            RecurringTransaction newSchedule = new RecurringTransaction(type, category, amount, desc, wallet, freq, finalDayOfMonth, finalDayOfWeek);
            
            executorService.execute(() -> {
                recDao.insert(newSchedule);
                List<RecurringTransaction> updatedList = recDao.getAllSync();
                runOnUiThread(() -> {
                    recAdapter.setItems(updatedList);
                    etRecCategory.setText("");
                    etRecAmount.setText("");
                    etRecDay.setText("");
                    etRecDescription.setText("");
                    Toast.makeText(this, "Schedule added successfully!", Toast.LENGTH_SHORT).show();
                });

                checkAndRunRecurringTransactions();
            });
        });

        dialog.show();
        styleDialogButtons(dialog);
    }
}

