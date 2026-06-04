package com.mabona.mobilesystems.soulmate;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DashboardActivity extends AppCompatActivity {

    // UI Components
    private TextView messageCountText, requestCountText, replyCountText, postCountText;
    private ImageView settingsIcon;
    private RecyclerView chatsRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ImageView createPostIcon, loveDoctorIcon, searchIcon, requestLoveIcon, postIcon;
    private AdView bannerAdView;
    private TextView emptyStateText;

    // Data
    private ChatAdapter chatAdapter;
    private List<ChatItem> chatList = new ArrayList<>();

    // User data from Intent
    private String userEmail;
    private int userId;
    private String userName;
    private String authToken;

    // Auto-refresh
    private Handler refreshHandler = new Handler();
    private Runnable refreshRunnable;
    private static final int REFRESH_INTERVAL = 30000;

    // Constants
    private static final String BASE_URL = "https://mabona.firstsuninvestment.com/soulmate/";
    private static final String PREFS_NAME = "SoulmatePrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Get user data from Intent (passed from MainActivity after login)
        userEmail = getIntent().getStringExtra("USER_EMAIL");
        userId = getIntent().getIntExtra("USER_ID", -1);
        userName = getIntent().getStringExtra("USER_NAME");
        authToken = getIntent().getStringExtra("AUTH_TOKEN");
        Toast.makeText(getApplicationContext(), "Welcome " + userName + "! 👋", Toast.LENGTH_LONG).show();

        // If no user data, go back to login
        if (userId == -1 || authToken == null || authToken.isEmpty()) {
            goToLogin();
            return;
        }

        initializeViews();
        setupTopIconClickListeners();
        setupBottomNavigation();
        loadAds();
        loadChatData(true);
        loadSummaryCounts();
        setupSwipeRefresh();
        startAutoRefresh();

        // Start notification service
        startNotificationService();
    }

    private void initializeViews() {
        messageCountText = findViewById(R.id.messageCountText);
        requestCountText = findViewById(R.id.requestCountText);
        replyCountText = findViewById(R.id.replyCountText);
        postCountText = findViewById(R.id.postCountText);
        settingsIcon = findViewById(R.id.settingsIcon);

        chatsRecyclerView = findViewById(R.id.chatsRecyclerView);
        chatsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatAdapter = new ChatAdapter(chatList, new ChatAdapter.OnChatClickListener() {
            @Override
            public void onChatClick(ChatItem chat) {
                Intent intent = new Intent(DashboardActivity.this, ChatActivity.class);
                intent.putExtra("OTHER_USER_ID", chat.getUserId());
                intent.putExtra("OTHER_USER_NAME", chat.getName());
                intent.putExtra("OTHER_USER_PROFILE_IMAGE", chat.getProfileImage());
                intent.putExtra("USER_ID", userId);
                intent.putExtra("AUTH_TOKEN", authToken);
                intent.putExtra("USER_EMAIL", userEmail);
                intent.putExtra("USER_NAME", userName);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });
        chatsRecyclerView.setAdapter(chatAdapter);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        createPostIcon = findViewById(R.id.createPostIcon);
        loveDoctorIcon = findViewById(R.id.loveDoctorIcon);
        searchIcon = findViewById(R.id.searchIcon);
        requestLoveIcon = findViewById(R.id.requestLoveIcon);
        postIcon = findViewById(R.id.postIcon);

        bannerAdView = findViewById(R.id.bannerAdView);

        settingsIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animation bounce = AnimationUtils.loadAnimation(DashboardActivity.this, R.anim.bounce);
                settingsIcon.startAnimation(bounce);
                Toast.makeText(DashboardActivity.this, "⚙️ Settings coming soon!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupTopIconClickListeners() {
        // Message Icon (top left)
        ImageView messageIcon = findViewById(R.id.messageIcon);
        if (messageIcon != null) {
            messageIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Animation bounce = AnimationUtils.loadAnimation(DashboardActivity.this, R.anim.bounce);
                    messageIcon.startAnimation(bounce);
                    Toast.makeText(DashboardActivity.this, "💬 Messages coming soon!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Request Icon - Opens RequestsActivity
        ImageView requestIcon = findViewById(R.id.requestsIcon);
        if (requestIcon != null) {
            requestIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Animation bounce = AnimationUtils.loadAnimation(DashboardActivity.this, R.anim.bounce);
                    requestIcon.startAnimation(bounce);

                    Intent intent = new Intent(DashboardActivity.this, RequestsActivity.class);
                    intent.putExtra("USER_ID", userId);
                    intent.putExtra("AUTH_TOKEN", authToken);
                    intent.putExtra("USER_EMAIL", userEmail);
                    intent.putExtra("USER_NAME", userName);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                }
            });
        }

        // Reply Icon - Opens RepliesActivity
        ImageView replyIcon = findViewById(R.id.replyIcon);
        if (replyIcon != null) {
            replyIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Animation bounce = AnimationUtils.loadAnimation(DashboardActivity.this, R.anim.bounce);
                    replyIcon.startAnimation(bounce);

                    Intent intent = new Intent(DashboardActivity.this, RepliesActivity.class);
                    intent.putExtra("USER_ID", userId);
                    intent.putExtra("AUTH_TOKEN", authToken);
                    intent.putExtra("USER_EMAIL", userEmail);
                    intent.putExtra("USER_NAME", userName);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                }
            });
        }

        // Post Icon - Opens PostsActivity
        ImageView postIcon = findViewById(R.id.postIcon);
        if (postIcon != null) {
            postIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Animation bounce = AnimationUtils.loadAnimation(DashboardActivity.this, R.anim.bounce);
                    postIcon.startAnimation(bounce);

                    Intent intent = new Intent(DashboardActivity.this, PostsActivity.class);
                    intent.putExtra("USER_ID", userId);
                    intent.putExtra("AUTH_TOKEN", authToken);
                    intent.putExtra("USER_EMAIL", userEmail);
                    intent.putExtra("USER_NAME", userName);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                }
            });
        }
    }

    private void startNotificationService() {
        try {
            Intent serviceIntent = new Intent(this, NotificationService.class);
            serviceIntent.putExtra("USER_ID", userId);
            serviceIntent.putExtra("AUTH_TOKEN", authToken);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

            Log.d("Dashboard", "NotificationService started");
        } catch (Exception e) {
            Log.e("Dashboard", "Error starting service: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupBottomNavigation() {
        createPostIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animation bounce = AnimationUtils.loadAnimation(DashboardActivity.this, R.anim.bounce);
                createPostIcon.startAnimation(bounce);

                Intent intent = new Intent(DashboardActivity.this, CreatePostActivity.class);
                intent.putExtra("USER_ID", userId);
                intent.putExtra("AUTH_TOKEN", authToken);
                intent.putExtra("USER_EMAIL", userEmail);
                intent.putExtra("USER_NAME", userName);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

        loveDoctorIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animation bounce = AnimationUtils.loadAnimation(DashboardActivity.this, R.anim.bounce);
                loveDoctorIcon.startAnimation(bounce);

                // Check if user has already set a PIN in the database
                checkUserLoveDoctorStatus();
            }
        });

        searchIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animation bounce = AnimationUtils.loadAnimation(DashboardActivity.this, R.anim.bounce);
                searchIcon.startAnimation(bounce);
                Intent intent = new Intent(DashboardActivity.this, SearchActivity.class);
                intent.putExtra("USER_ID", userId);
                intent.putExtra("AUTH_TOKEN", authToken);
                intent.putExtra("USER_EMAIL", userEmail);
                intent.putExtra("USER_NAME", userName);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

        requestLoveIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animation bounce = AnimationUtils.loadAnimation(DashboardActivity.this, R.anim.bounce);
                requestLoveIcon.startAnimation(bounce);
                Intent intent = new Intent(DashboardActivity.this, SearchActivity.class);
                intent.putExtra("USER_ID", userId);
                intent.putExtra("AUTH_TOKEN", authToken);
                intent.putExtra("USER_EMAIL", userEmail);
                intent.putExtra("USER_NAME", userName);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

        postIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animation bounce = AnimationUtils.loadAnimation(DashboardActivity.this, R.anim.bounce);
                postIcon.startAnimation(bounce);
                Intent intent = new Intent(DashboardActivity.this, PostsActivity.class);
                intent.putExtra("USER_ID", userId);
                intent.putExtra("AUTH_TOKEN", authToken);
                intent.putExtra("USER_EMAIL", userEmail);
                intent.putExtra("USER_NAME", userName);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });
    }

    // New method to check if user already has a PIN set in database
    private void checkUserLoveDoctorStatus() {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(BASE_URL + "dr.php?action=check_pin_status&user_id=" + userId + "&token=" + authToken)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    // If network error, show PIN dialog anyway
                    showLoveDoctorEntryDialog();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body().string();
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        boolean hasPin = json.optBoolean("has_pin", false);

                        if (hasPin) {
                            // User already has a PIN - show PIN verification dialog
                            showPinVerificationDialog();
                        } else {
                            // First time - show warning and create PIN dialog
                            showFirstTimeLoveDoctorDialog();
                        }
                    } catch (JSONException e) {
                        // Default to showing first-time dialog
                        showFirstTimeLoveDoctorDialog();
                    }
                });
            }
        });
    }

    // Show Love Doctor entry dialog as fallback
    private void showLoveDoctorEntryDialog() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean hasSeenLoveDoctorWarning = prefs.getBoolean("has_seen_love_doctor_warning", false);

        if (!hasSeenLoveDoctorWarning) {
            showFirstTimeLoveDoctorDialog();
        } else {
            showPinVerificationDialog();
        }
    }

    // First time - Show warning message and PIN creation
    private void showFirstTimeLoveDoctorDialog() {
        Dialog dialog = new Dialog(DashboardActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_love_doctor_entry);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView messageText = dialog.findViewById(R.id.messageText);
        EditText pinInput = dialog.findViewById(R.id.pinInput);
        LinearLayout pinLayout = dialog.findViewById(R.id.pinLayout);
        CardView enterButton = dialog.findViewById(R.id.enterButton);
        TextView enterButtonText = dialog.findViewById(R.id.enterButtonText);
        CardView cancelButton = dialog.findViewById(R.id.cancelButton);

        // Show the full warning message
        messageText.setText("💕 It is time to let your fears out, talk about what makes you upset about your partner and get advice from professional love doctors who are Counsellors, Influencers, Motivators and even Pastors.\n\nIt's time you no longer use your anger towards him or her, but understand...\n\n💝 Your chats are PIN protected and secure!");
        pinLayout.setVisibility(View.VISIBLE);

        if (enterButtonText != null) {
            enterButtonText.setText("Create PIN & Continue");
        }

        enterButton.setOnClickListener(v -> {
            String pin = pinInput.getText().toString().trim();
            if (pin.length() < 4) {
                Toast.makeText(DashboardActivity.this, "Please enter a PIN with at least 4 digits", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            // Save PIN to database and proceed
            saveLoveDoctorPinToDatabase(pin);
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // Returning user - Show only PIN verification
    private void showPinVerificationDialog() {
        Dialog dialog = new Dialog(DashboardActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_pin_verification);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        EditText pinInput = dialog.findViewById(R.id.pinInput);
        CardView verifyButton = dialog.findViewById(R.id.verifyButton);
        CardView cancelButton = dialog.findViewById(R.id.cancelButton);

        verifyButton.setOnClickListener(v -> {
            String pin = pinInput.getText().toString().trim();
            if (pin.isEmpty()) {
                Toast.makeText(DashboardActivity.this, "Please enter your PIN", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            verifyLoveDoctorPin(pin);
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // Save the PIN to database (first time)
    private void saveLoveDoctorPinToDatabase(String pin) {
        OkHttpClient client = new OkHttpClient();

        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("action", "save_pin")
                .addFormDataPart("token", authToken)
                .addFormDataPart("user_id", String.valueOf(userId))
                .addFormDataPart("pin", pin)
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + "dr.php")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(DashboardActivity.this, "Failed to save PIN. Please try again.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body().string();
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        if (json.getBoolean("success")) {
                            // Save that user has seen the warning
                            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                                    .putBoolean("has_seen_love_doctor_warning", true).apply();

                            // PIN saved successfully, open LoveDoctorActivity
                            openLoveDoctorActivity();
                        } else {
                            Toast.makeText(DashboardActivity.this, json.optString("message", "Failed to save PIN"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(DashboardActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    // Verify existing PIN
    private void verifyLoveDoctorPin(String pin) {
        OkHttpClient client = new OkHttpClient();

        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("action", "verify_pin")
                .addFormDataPart("token", authToken)
                .addFormDataPart("user_id", String.valueOf(userId))
                .addFormDataPart("pin", pin)
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + "dr.php")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(DashboardActivity.this, "Network error. Please try again.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body().string();
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        if (json.getBoolean("success")) {
                            // PIN verified, open LoveDoctorActivity
                            openLoveDoctorActivity();
                        } else {
                            Toast.makeText(DashboardActivity.this, "Invalid PIN. Please try again.", Toast.LENGTH_SHORT).show();
                            showPinVerificationDialog(); // Show again
                        }
                    } catch (JSONException e) {
                        Toast.makeText(DashboardActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    // Open LoveDoctorActivity
    private void openLoveDoctorActivity() {
        Intent intent = new Intent(DashboardActivity.this, LoveDoctorActivity.class);
        intent.putExtra("USER_ID", userId);
        intent.putExtra("AUTH_TOKEN", authToken);
        intent.putExtra("USER_EMAIL", userEmail);
        intent.putExtra("USER_NAME", userName);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadChatData(true);
                loadSummaryCounts();
            }
        });
        swipeRefreshLayout.setColorSchemeResources(R.color.pink, R.color.purple, R.color.red_light);
    }

    private void loadAds() {
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
                loadBannerAd();
            }
        });
    }

    private void loadBannerAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        bannerAdView.loadAd(adRequest);
        bannerAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                bannerAdView.setVisibility(View.VISIBLE);
            }
            @Override
            public void onAdFailedToLoad(LoadAdError adError) {
                bannerAdView.setVisibility(View.GONE);
            }
        });
    }

    private void loadChatData(boolean showLoading) {
        if (showLoading) {
            swipeRefreshLayout.setRefreshing(true);
        }

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(BASE_URL + "chats.php?user_id=" + userId + "&token=" + authToken)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefreshLayout.setRefreshing(false);
                        if (showLoading) {
                            Toast.makeText(DashboardActivity.this,
                                    "⚠️ Network error. Please check your connection.",
                                    Toast.LENGTH_SHORT).show();
                        }
                        showEmptyStateMessage("🌐 No internet connection.\nPull down to try again!");
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseBody = response.body().string();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefreshLayout.setRefreshing(false);

                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            boolean success = jsonResponse.getBoolean("success");

                            if (success) {
                                JSONArray chatsArray = jsonResponse.getJSONArray("chats");
                                chatList.clear();

                                if (chatsArray.length() == 0) {
                                    showEmptyStateMessage("😍 Start by searching a partner ❤️\n\n✨ Start by searching for people\n💕 Send a love request\n💬 Or wait for someone to message you!");
                                } else {
                                    for (int i = 0; i < chatsArray.length(); i++) {
                                        JSONObject chat = chatsArray.getJSONObject(i);

                                        ChatItem chatItem = new ChatItem(
                                                chat.getInt("user_id"),
                                                chat.getString("name"),
                                                chat.getString("last_message"),
                                                chat.getString("time"),
                                                chat.getString("profile_image"),
                                                chat.getBoolean("is_online")
                                        );

                                        chatList.add(chatItem);
                                    }
                                }

                                chatAdapter.notifyDataSetChanged();

                            } else {
                                String message = jsonResponse.getString("message");
                                if (showLoading) {
                                    Toast.makeText(DashboardActivity.this, "📭 " + message, Toast.LENGTH_SHORT).show();
                                }
                                showEmptyStateMessage("💔 No chats found.\n\n🔍 Search for people to start a conversation!");
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                            if (showLoading) {
                                Toast.makeText(DashboardActivity.this,
                                        "💔Error loading chats. Please try again.",
                                        Toast.LENGTH_SHORT).show();
                            }
                            showEmptyStateMessage("😍Start by searching a partner.\nPull down to refresh!");
                        }
                    }
                });
            }
        });
    }

    private void showEmptyStateMessage(String message) {
        chatList.clear();
        ChatItem emptyChat = new ChatItem(0, "✨ No Messages Yet", message, "", "", false);
        emptyChat.setAsEmptyState(true);
        chatList.add(emptyChat);
        chatAdapter.notifyDataSetChanged();
    }

    private void loadSummaryCounts() {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(BASE_URL + "dashboard_counts.php?user_id=" + userId + "&token=" + authToken)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        messageCountText.setText("0");
                        requestCountText.setText("0");
                        replyCountText.setText("0");
                        postCountText.setText("0");
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseBody = response.body().string();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            boolean success = jsonResponse.getBoolean("success");

                            if (success) {
                                messageCountText.setText(jsonResponse.getString("messages"));
                                requestCountText.setText(jsonResponse.getString("requests"));
                                replyCountText.setText(jsonResponse.getString("replies"));
                                postCountText.setText(jsonResponse.getString("posts"));
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    private void startAutoRefresh() {
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isFinishing() && !isDestroyed()) {
                    refreshDataSilently();
                }
                refreshHandler.postDelayed(this, REFRESH_INTERVAL);
            }
        };
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
    }

    private void refreshDataSilently() {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(BASE_URL + "chats.php?user_id=" + userId + "&token=" + authToken)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Silent fail
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseBody = response.body().string();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            boolean success = jsonResponse.getBoolean("success");

                            if (success) {
                                JSONArray chatsArray = jsonResponse.getJSONArray("chats");

                                boolean dataChanged = false;

                                if (chatList.size() != chatsArray.length()) {
                                    dataChanged = true;
                                } else if (chatsArray.length() > 0 && chatList.size() > 0) {
                                    try {
                                        JSONObject firstNew = chatsArray.getJSONObject(0);
                                        ChatItem firstOld = chatList.get(0);
                                        if (!firstNew.getString("last_message").equals(firstOld.getLastMessage())) {
                                            dataChanged = true;
                                        }
                                    } catch (JSONException e) {
                                        dataChanged = true;
                                    }
                                }

                                if (dataChanged) {
                                    chatList.clear();

                                    for (int i = 0; i < chatsArray.length(); i++) {
                                        JSONObject chat = chatsArray.getJSONObject(i);

                                        ChatItem chatItem = new ChatItem(
                                                chat.getInt("user_id"),
                                                chat.getString("name"),
                                                chat.getString("last_message"),
                                                chat.getString("time"),
                                                chat.getString("profile_image"),
                                                chat.getBoolean("is_online")
                                        );

                                        chatList.add(chatItem);
                                    }

                                    chatAdapter.notifyDataSetChanged();
                                }
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        refreshCountsSilently();
    }

    private void refreshCountsSilently() {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(BASE_URL + "dashboard_counts.php?user_id=" + userId + "&token=" + authToken)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Silent fail
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseBody = response.body().string();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            boolean success = jsonResponse.getBoolean("success");

                            if (success) {
                                messageCountText.setText(jsonResponse.getString("messages"));
                                requestCountText.setText(jsonResponse.getString("requests"));
                                replyCountText.setText(jsonResponse.getString("replies"));
                                postCountText.setText(jsonResponse.getString("posts"));
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    private void stopAutoRefresh() {
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }

    private void goToLogin() {
        Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        moveTaskToBack(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bannerAdView != null) {
            bannerAdView.resume();
        }
        loadChatData(true);
        loadSummaryCounts();
        startAutoRefresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bannerAdView != null) {
            bannerAdView.pause();
        }
        stopAutoRefresh();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAutoRefresh();
        if (bannerAdView != null) {
            bannerAdView.destroy();
        }
        // Stop notification service when app closes
        Intent serviceIntent = new Intent(this, NotificationService.class);
        stopService(serviceIntent);
    }
}