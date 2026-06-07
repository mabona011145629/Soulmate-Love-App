package com.mabona.mobilesystems.soulmate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Gravity;
import android.view.LayoutInflater;

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

public class RequestsActivity extends AppCompatActivity {

    // UI Components
    private RecyclerView requestsRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyStateText;
    private ImageView backButton;
    private AdView bannerAdView;
    private ImageView heartImage1, heartImage2, heartImage3;

    // Data
    private RequestsAdapter requestsAdapter;
    private List<RequestItem> requestList = new ArrayList<>();
    private int userId;
    private String authToken;
    private String userEmail;
    private String userName;

    // Constants
    private static final String BASE_URL = "https://mabona.firstsuninvestment.com/soulmate/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_requests);

        // Get user data from Intent (passed from DashboardActivity)
        userId = getIntent().getIntExtra("USER_ID", -1);
        authToken = getIntent().getStringExtra("AUTH_TOKEN");
        userEmail = getIntent().getStringExtra("USER_EMAIL");
        userName = getIntent().getStringExtra("USER_NAME");

        if (userId == -1 || authToken == null || authToken.isEmpty()) {
            showPinkToast("Session expired");
            finish();
            return;
        }

        initializeViews();
        setupAnimations();
        setupRecyclerView();
        loadMediumAd();
        loadRequests();
    }

    private void initializeViews() {
        requestsRecyclerView = findViewById(R.id.requestsRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyStateText = findViewById(R.id.emptyStateText);
        backButton = findViewById(R.id.backButton);
        bannerAdView = findViewById(R.id.bannerAdView);
        heartImage1 = findViewById(R.id.heartImage1);
        heartImage2 = findViewById(R.id.heartImage2);
        heartImage3 = findViewById(R.id.heartImage3);

        backButton.setOnClickListener(v -> {
            Animation bounce = AnimationUtils.loadAnimation(RequestsActivity.this, R.anim.bounce);
            backButton.startAnimation(bounce);
            finish();
            overridePendingTransition(R.anim.slide_out_left, R.anim.slide_in_right);
        });
    }

    private void setupAnimations() {
        android.animation.ObjectAnimator floatAnimation1 = android.animation.ObjectAnimator.ofFloat(heartImage1, "translationY", -20f, 20f);
        floatAnimation1.setDuration(2000);
        floatAnimation1.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        floatAnimation1.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        floatAnimation1.start();

        android.animation.ObjectAnimator floatAnimation2 = android.animation.ObjectAnimator.ofFloat(heartImage2, "translationY", 20f, -20f);
        floatAnimation2.setDuration(2200);
        floatAnimation2.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        floatAnimation2.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        floatAnimation2.start();

        android.animation.ObjectAnimator floatAnimation3 = android.animation.ObjectAnimator.ofFloat(heartImage3, "translationX", -15f, 15f);
        floatAnimation3.setDuration(1800);
        floatAnimation3.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        floatAnimation3.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        floatAnimation3.start();
    }

    // In setupRecyclerView() method, add this line:
    private void setupRecyclerView() {
        requestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        requestsAdapter = new RequestsAdapter(requestList, request -> {
            Intent intent = new Intent(RequestsActivity.this, RequestLoveActivity.class);
            intent.putExtra("MY_USER_ID", userId);
            intent.putExtra("MY_AUTH_TOKEN", authToken);
            intent.putExtra("MY_USER_EMAIL", userEmail);
            intent.putExtra("MY_USER_NAME", userName);
            intent.putExtra("TARGET_USER_ID", request.getUserId());
            intent.putExtra("TARGET_USER_NAME", request.getFullName());
            intent.putExtra("TARGET_USER_IMAGE_PATH", request.getProfileImage());  // Pass the PATH
            intent.putExtra("TARGET_USER_GENDER", request.getGender());
            intent.putExtra("TARGET_USER_AGE", request.getAge());
            intent.putExtra("TARGET_USER_BIO", request.getBio());
            intent.putExtra("TARGET_USER_LOCATION", request.getLocation());
            intent.putExtra("TARGET_USER_ONLINE", request.isOnline());
            intent.putExtra("PROFILE_TYPE", request.getProfileType());
            intent.putExtra("REQUEST_ID", request.getRequestId());
            intent.putExtra("REQUEST_TYPE", request.getRequestType());
            intent.putExtra("MODE", "incoming");
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
        requestsAdapter.setAuthToken(authToken);  // ADD THIS LINE
        requestsRecyclerView.setAdapter(requestsAdapter);
    }

    private void loadMediumAd() {
        MobileAds.initialize(this, initializationStatus -> {
            AdRequest adRequest = new AdRequest.Builder().build();
            bannerAdView.loadAd(adRequest);
            bannerAdView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() { bannerAdView.setVisibility(View.VISIBLE); }
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError adError) { bannerAdView.setVisibility(View.GONE); }
            });
        });
    }

    private void loadRequests() {
        showProgress(true);

        OkHttpClient client = new OkHttpClient();
        String url = BASE_URL + "request_view.php?user_id=" + userId + "&token=" + authToken;

        Request request = new Request.Builder().url(url).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    showEmptyState(true, "⚠️ Network error. Please try again.");
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseBody = response.body().string();

                runOnUiThread(() -> {
                    showProgress(false);
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        boolean success = jsonResponse.getBoolean("success");

                        if (success) {
                            JSONArray requestsArray = jsonResponse.getJSONArray("requests");
                            requestList.clear();

                            for (int i = 0; i < requestsArray.length(); i++) {
                                JSONObject req = requestsArray.getJSONObject(i);
                                RequestItem requestItem = new RequestItem(
                                        req.getInt("request_id"),
                                        req.getString("request_type"),
                                        req.getInt("user_id"),
                                        req.getString("full_name"),
                                        req.getString("first_name"),
                                        req.getString("last_name"),
                                        req.getString("gender"),
                                        req.getInt("age"),
                                        req.getString("bio"),
                                        req.getString("location"),
                                        req.getString("profile_image"),
                                        req.getBoolean("is_online"),
                                        req.getString("profile_type"),
                                        req.getString("requested_at")
                                );
                                requestList.add(requestItem);
                            }

                            requestsAdapter.notifyDataSetChanged();

                            if (requestList.isEmpty()) {
                                showEmptyState(true, "💭 No pending requests\n\nWhen someone sends you a love or profile request, it will appear here.");
                            } else {
                                showEmptyState(false, "");
                            }
                        }
                    } catch (JSONException e) {
                        showEmptyState(true, "⚠️ Error loading requests");
                    }
                });
            }
        });
    }

    private void showPinkToast(String message) {
        try {
            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.custom_toast_pink, findViewById(R.id.custom_toast_container));
            TextView text = layout.findViewById(R.id.toast_text);
            text.setText(message);

            Toast toast = new Toast(getApplicationContext());
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setView(layout);
            toast.setGravity(Gravity.BOTTOM, 0, 100);
            toast.show();
        } catch (Exception e) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showEmptyState(boolean show, String message) {
        if (show) {
            emptyStateText.setText(message);
            emptyStateText.setVisibility(View.VISIBLE);
            requestsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            requestsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bannerAdView != null) bannerAdView.resume();
        loadRequests();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bannerAdView != null) bannerAdView.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bannerAdView != null) bannerAdView.destroy();
    }
}