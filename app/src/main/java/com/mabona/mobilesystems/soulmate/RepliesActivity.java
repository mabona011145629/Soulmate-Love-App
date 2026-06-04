package com.mabona.mobilesystems.soulmate;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RepliesActivity extends AppCompatActivity {

    // UI Components
    private RecyclerView repliesRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyStateText;
    private ImageView backButton;
    private AdView bannerAdView;
    private ImageView heartImage1, heartImage2, heartImage3;

    // Data
    private RepliesAdapter repliesAdapter;
    private List<ReplyItem> replyList = new ArrayList<>();
    private int userId;
    private String authToken;
    private String userEmail;
    private String userName;

    // Constants
    private static final String BASE_URL = "https://mabona.firstsuninvestment.com/soulmate/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_replies);

        // Get user data from intent
        userId = getIntent().getIntExtra("USER_ID", -1);
        authToken = getIntent().getStringExtra("AUTH_TOKEN");
        userEmail = getIntent().getStringExtra("USER_EMAIL");
        userName = getIntent().getStringExtra("USER_NAME");

        // Validate session
        if (userId == -1 || authToken == null || authToken.isEmpty()) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupAnimations();
        setupRecyclerView();
        loadAd();
        loadReplies();
    }

    private void initializeViews() {
        repliesRecyclerView = findViewById(R.id.repliesRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyStateText = findViewById(R.id.emptyStateText);
        backButton = findViewById(R.id.backButton);
        bannerAdView = findViewById(R.id.bannerAdView);
        heartImage1 = findViewById(R.id.heartImage1);
        heartImage2 = findViewById(R.id.heartImage2);
        heartImage3 = findViewById(R.id.heartImage3);

        backButton.setOnClickListener(v -> {
            Animation bounce = AnimationUtils.loadAnimation(RepliesActivity.this, R.anim.bounce);
            backButton.startAnimation(bounce);
            finish();
            overridePendingTransition(R.anim.slide_out_left, R.anim.slide_in_right);
        });
    }

    private void setupAnimations() {
        // Floating animation for heart 1
        android.animation.ObjectAnimator floatAnimation1 = android.animation.ObjectAnimator.ofFloat(
                heartImage1, "translationY", -20f, 20f);
        floatAnimation1.setDuration(2000);
        floatAnimation1.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        floatAnimation1.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        floatAnimation1.start();

        // Floating animation for heart 2
        android.animation.ObjectAnimator floatAnimation2 = android.animation.ObjectAnimator.ofFloat(
                heartImage2, "translationY", 20f, -20f);
        floatAnimation2.setDuration(2200);
        floatAnimation2.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        floatAnimation2.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        floatAnimation2.start();

        // Floating animation for heart 3
        android.animation.ObjectAnimator floatAnimation3 = android.animation.ObjectAnimator.ofFloat(
                heartImage3, "translationX", -15f, 15f);
        floatAnimation3.setDuration(1800);
        floatAnimation3.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        floatAnimation3.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        floatAnimation3.start();
    }

    private void setupRecyclerView() {
        repliesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        repliesAdapter = new RepliesAdapter(
                replyList,
                new RepliesAdapter.OnReplyClickListener() {
                    @Override
                    public void onChatClick(ReplyItem reply) {
                        openChat(reply);
                    }

                    @Override
                    public void onProfileViewClick(ReplyItem reply) {
                        viewProfile(reply);
                    }
                },
                authToken,
                userId,
                userName,
                userEmail
        );
        repliesRecyclerView.setAdapter(repliesAdapter);
    }

    private void openChat(ReplyItem reply) {
        Intent intent = new Intent(RepliesActivity.this, ChatActivity.class);
        intent.putExtra("USER_ID", userId);
        intent.putExtra("AUTH_TOKEN", authToken);
        intent.putExtra("USER_EMAIL", userEmail);
        intent.putExtra("USER_NAME", userName);
        intent.putExtra("OTHER_USER_ID", reply.getUserId());
        intent.putExtra("OTHER_USER_NAME", reply.getFullName());
        intent.putExtra("OTHER_USER_PROFILE_IMAGE", reply.getProfileImage());
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void viewProfile(ReplyItem reply) {
        // Check if allowed to view profile
        if (!reply.canViewProfile() && !"approved".equals(reply.getRequestStatus())) {
            Toast.makeText(this, "You need to approve this request first to view full profile", Toast.LENGTH_SHORT).show();
            return;
        }

        // Open profile view activity
        Intent intent = new Intent(RepliesActivity.this, ProfileViewActivity.class);
        intent.putExtra("USER_ID", userId);
        intent.putExtra("AUTH_TOKEN", authToken);
        intent.putExtra("VIEW_USER_ID", reply.getUserId());
        intent.putExtra("VIEW_USER_NAME", reply.getFullName());
        intent.putExtra("CAN_VIEW_FULL_PROFILE", reply.canViewProfile() || "approved".equals(reply.getRequestStatus()));
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void loadAd() {
        MobileAds.initialize(this, initializationStatus -> {
            AdRequest adRequest = new AdRequest.Builder().build();
            bannerAdView.loadAd(adRequest);
            bannerAdView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    bannerAdView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                    bannerAdView.setVisibility(View.GONE);
                }

                @Override
                public void onAdOpened() {
                    // Ad opened
                }

                @Override
                public void onAdClosed() {
                    // Ad closed
                }
            });
        });
    }

    private void loadReplies() {
        showProgress(true);
        showEmptyState(false, null);

        OkHttpClient client = new OkHttpClient();
        String url = BASE_URL + "replies.php?user_id=" + userId + "&token=" + authToken;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    showEmptyState(true, "⚠️ Network error\n\nPlease check your internet connection and try again.");
                    Toast.makeText(RepliesActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";

                runOnUiThread(() -> {
                    showProgress(false);

                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        boolean success = jsonResponse.getBoolean("success");

                        if (success) {
                            JSONArray repliesArray = jsonResponse.getJSONArray("replies");
                            replyList.clear();

                            for (int i = 0; i < repliesArray.length(); i++) {
                                JSONObject reply = repliesArray.getJSONObject(i);

                                ReplyItem replyItem = new ReplyItem(
                                        reply.getInt("reply_id"),
                                        reply.getString("request_type"),
                                        reply.getString("request_status"),
                                        reply.getString("responded_at"),
                                        reply.getInt("user_id"),
                                        reply.getString("full_name"),
                                        reply.getString("first_name"),
                                        reply.getString("last_name"),
                                        reply.getString("gender"),
                                        reply.getInt("age"),
                                        reply.getString("bio"),
                                        reply.getString("location"),
                                        reply.optString("profile_image", null),
                                        reply.getBoolean("is_online"),
                                        reply.getString("profile_type"),
                                        reply.getBoolean("can_view_profile")
                                );
                                replyList.add(replyItem);
                            }

                            if (replyList.isEmpty()) {
                                showEmptyState(true, "💕 No replies yet\n\nWhen someone responds to your love requests or profile access requests, they'll appear here.");
                            } else {
                                showEmptyState(false, null);
                                repliesAdapter.notifyDataSetChanged();
                            }
                        } else {
                            String message = jsonResponse.optString("message", "Failed to load replies");
                            showEmptyState(true, "⚠️ " + message);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        showEmptyState(true, "⚠️ Error loading data\n\nPlease try again later.");
                        Toast.makeText(RepliesActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            emptyStateText.setVisibility(View.GONE);
            repliesRecyclerView.setVisibility(View.GONE);
        }
    }

    private void showEmptyState(boolean show, String message) {
        if (show && message != null) {
            emptyStateText.setText(message);
            emptyStateText.setVisibility(View.VISIBLE);
            repliesRecyclerView.setVisibility(View.GONE);
        } else if (!show) {
            emptyStateText.setVisibility(View.GONE);
            repliesRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh replies when returning to this activity
        if (replyList != null) {
            loadReplies();
        }
    }

    @SuppressLint({"GestureBackNavigation", "MissingSuperCall"})
    @Override
    public void onBackPressed() {
        // super.onBackPressed() is omitted to handle the finish and transition manually
        finish();
        overridePendingTransition(R.anim.slide_out_left, R.anim.slide_in_right);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up any resources if needed
        if (bannerAdView != null) {
            bannerAdView.destroy();
        }
    }
}