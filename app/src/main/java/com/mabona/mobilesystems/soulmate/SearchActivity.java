package com.mabona.mobilesystems.soulmate;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
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
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

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

public class SearchActivity extends AppCompatActivity {

    // UI Components
    private EditText searchEditText;
    private RecyclerView resultsRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyStateText;
    private ImageView backButton;
    private AdView mediumAdView;
    private ImageView heartImage1, heartImage2, heartImage3;

    // Data
    private SearchAdapter searchAdapter;
    private List<SearchItem> userList = new ArrayList<>();
    private int userId;
    private String authToken;
    private String userEmail;
    private String userName;

    // Search debounce
    private Handler searchHandler = new Handler();
    private Runnable searchRunnable;
    private static final int SEARCH_DELAY = 500;

    // Constants
    private static final String BASE_URL = "https://mabona.firstsuninvestment.com/soulmate/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Get user data from Intent (passed from DashboardActivity)
        userId = getIntent().getIntExtra("USER_ID", -1);
        authToken = getIntent().getStringExtra("AUTH_TOKEN");
        userEmail = getIntent().getStringExtra("USER_EMAIL");
        userName = getIntent().getStringExtra("USER_NAME");

        // If no user data, go back
        if (userId == -1 || authToken == null || authToken.isEmpty()) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            goBack();
            return;
        }

        initializeViews();
        setupAnimations();
        setupRecyclerView();
        loadMediumAd();

        // Load initial suggestions
        performSearch("");
    }

    private void initializeViews() {
        searchEditText = findViewById(R.id.searchEditText);
        resultsRecyclerView = findViewById(R.id.resultsRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyStateText = findViewById(R.id.emptyStateText);
        backButton = findViewById(R.id.backButton);
        mediumAdView = findViewById(R.id.mediumAdView);

        heartImage1 = findViewById(R.id.heartImage1);
        heartImage2 = findViewById(R.id.heartImage2);
        heartImage3 = findViewById(R.id.heartImage3);

        // Search with debounce
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                searchRunnable = new Runnable() {
                    @Override
                    public void run() {
                        performSearch(s.toString());
                    }
                };
                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Back button
        backButton.setOnClickListener(v -> {
            Animation bounce = AnimationUtils.loadAnimation(SearchActivity.this, R.anim.bounce);
            backButton.startAnimation(bounce);
            goBack();
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

    private void setupRecyclerView() {
        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchAdapter = new SearchAdapter(userList, user -> {
            // Navigate to RequestLoveActivity with the selected user
            Intent intent = new Intent(SearchActivity.this, RequestLoveActivity.class);

            intent.putExtra("TARGET_USER_ID", user.getUserId());
            intent.putExtra("TARGET_USER_NAME", user.getFullName());
            intent.putExtra("TARGET_USER_IMAGE_PATH", user.getProfileImagePath());
            intent.putExtra("TARGET_USER_GENDER", user.getGender());
            intent.putExtra("TARGET_USER_AGE", user.getAge());
            intent.putExtra("TARGET_USER_BIO", user.getBio());
            intent.putExtra("TARGET_USER_LOCATION", user.getLocation());
            intent.putExtra("TARGET_USER_ONLINE", user.isOnline());
            intent.putExtra("PROFILE_TYPE", user.getProfileType());
            intent.putExtra("MY_USER_ID", userId);
            intent.putExtra("MY_AUTH_TOKEN", authToken);

            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
        resultsRecyclerView.setAdapter(searchAdapter);
    }

    private void loadMediumAd() {
        MobileAds.initialize(this, initializationStatus -> {
            AdRequest adRequest = new AdRequest.Builder().build();
            mediumAdView.loadAd(adRequest);

            mediumAdView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    mediumAdView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                    mediumAdView.setVisibility(View.GONE);
                }
            });
        });
    }

    private void performSearch(String query) {
        showProgress(true);

        OkHttpClient client = new OkHttpClient();

        String url = BASE_URL + "search.php?user_id=" + userId +
                "&token=" + authToken +
                "&q=" + java.net.URLEncoder.encode(query);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    showEmptyState(true, "⚠️ Network error. Please try again.");
                    Toast.makeText(SearchActivity.this,
                            "Failed to load results", Toast.LENGTH_SHORT).show();
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
                            JSONArray usersArray = jsonResponse.getJSONArray("users");
                            userList.clear();

                            for (int i = 0; i < usersArray.length(); i++) {
                                JSONObject user = usersArray.getJSONObject(i);

                                SearchItem searchItem = new SearchItem(
                                        user.getInt("user_id"),
                                        user.getString("full_name"),
                                        user.getString("first_name"),
                                        user.getString("last_name"),
                                        user.getString("gender"),
                                        user.getInt("age"),
                                        user.getString("bio"),
                                        user.getString("location"),
                                        user.getString("profile_image"),
                                        user.getString("profile_image_path"),
                                        user.getBoolean("is_online"),
                                        user.getString("profile_type"),
                                        user.getBoolean("has_pending_request")
                                );

                                userList.add(searchItem);
                            }

                            searchAdapter.notifyDataSetChanged();

                            if (userList.isEmpty()) {
                                String message = query.isEmpty() ?
                                        "💭 No suggestions available\n\nTry adjusting your preferences" :
                                        "🔍 No results found for \"" + query + "\"\n\nTry different keywords";
                                showEmptyState(true, message);
                            } else {
                                showEmptyState(false, "");
                            }

                        } else {
                            String message = jsonResponse.getString("message");
                            showEmptyState(true, "⚠️ " + message);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        showEmptyState(true, "⚠️ Error loading results. Please try again.");
                    }
                });
            }
        });
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showEmptyState(boolean show, String message) {
        if (show) {
            emptyStateText.setText(message);
            emptyStateText.setVisibility(View.VISIBLE);
            resultsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            resultsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void goBack() {
        finish();
        overridePendingTransition(R.anim.slide_out_left, R.anim.slide_in_right);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediumAdView != null) {
            mediumAdView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediumAdView != null) {
            mediumAdView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediumAdView != null) {
            mediumAdView.destroy();
        }
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }
}
























/*package com.mabona.mobilesystems.soulmate;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
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
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

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

public class SearchActivity extends AppCompatActivity {

    // UI Components
    private EditText searchEditText;
    private RecyclerView resultsRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyStateText;
    private ImageView backButton;
    private AdView mediumAdView;
    private ImageView heartImage1, heartImage2, heartImage3;

    // Data
    private SearchAdapter searchAdapter;
    private List<SearchItem> userList = new ArrayList<>();
    private int userId;
    private String authToken;
    private String userEmail;
    private String userName;

    // Search debounce
    private Handler searchHandler = new Handler();
    private Runnable searchRunnable;
    private static final int SEARCH_DELAY = 500;

    // Constants
    private static final String BASE_URL = "https://mabona.firstsuninvestment.com/soulmate/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Get user data from Intent (passed from DashboardActivity)
        userId = getIntent().getIntExtra("USER_ID", -1);
        authToken = getIntent().getStringExtra("AUTH_TOKEN");
        userEmail = getIntent().getStringExtra("USER_EMAIL");
        userName = getIntent().getStringExtra("USER_NAME");

        // If no user data, go back
        if (userId == -1 || authToken == null || authToken.isEmpty()) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            goBack();
            return;
        }

        initializeViews();
        setupAnimations();
        setupRecyclerView();
        loadMediumAd();

        // Load initial suggestions
        performSearch("");
    }

    private void initializeViews() {
        searchEditText = findViewById(R.id.searchEditText);
        resultsRecyclerView = findViewById(R.id.resultsRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyStateText = findViewById(R.id.emptyStateText);
        backButton = findViewById(R.id.backButton);
        mediumAdView = findViewById(R.id.mediumAdView);

        heartImage1 = findViewById(R.id.heartImage1);
        heartImage2 = findViewById(R.id.heartImage2);
        heartImage3 = findViewById(R.id.heartImage3);

        // Search with debounce
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                searchRunnable = new Runnable() {
                    @Override
                    public void run() {
                        performSearch(s.toString());
                    }
                };
                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Back button
        backButton.setOnClickListener(v -> {
            Animation bounce = AnimationUtils.loadAnimation(SearchActivity.this, R.anim.bounce);
            backButton.startAnimation(bounce);
            goBack();
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

    private void setupRecyclerView() {
        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchAdapter = new SearchAdapter(userList, user -> {
            // Navigate to RequestLoveActivity with the selected user
            Intent intent = new Intent(SearchActivity.this, RequestLoveActivity.class);

            intent.putExtra("TARGET_USER_ID", user.getUserId());
            intent.putExtra("TARGET_USER_NAME", user.getFullName());
            // Pass the IMAGE PATH (not Base64) for RequestLoveActivity to load
            intent.putExtra("TARGET_USER_IMAGE_PATH", user.getProfileImagePath());
            intent.putExtra("TARGET_USER_GENDER", user.getGender());
            intent.putExtra("TARGET_USER_AGE", user.getAge());
            intent.putExtra("TARGET_USER_BIO", user.getBio());
            intent.putExtra("TARGET_USER_LOCATION", user.getLocation());
            intent.putExtra("TARGET_USER_ONLINE", user.isOnline());
            intent.putExtra("PROFILE_TYPE", user.getProfileType());
            intent.putExtra("MY_USER_ID", userId);
            intent.putExtra("MY_AUTH_TOKEN", authToken);

            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
        resultsRecyclerView.setAdapter(searchAdapter);
    }

    private void loadMediumAd() {
        MobileAds.initialize(this, initializationStatus -> {
            AdRequest adRequest = new AdRequest.Builder().build();
            mediumAdView.loadAd(adRequest);

            mediumAdView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    mediumAdView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                    mediumAdView.setVisibility(View.GONE);
                }
            });
        });
    }

    private void performSearch(String query) {
        showProgress(true);

        OkHttpClient client = new OkHttpClient();

        String url = BASE_URL + "search.php?user_id=" + userId +
                "&token=" + authToken +
                "&q=" + java.net.URLEncoder.encode(query);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    showEmptyState(true, "⚠️ Network error. Please try again.");
                    Toast.makeText(SearchActivity.this,
                            "Failed to load results", Toast.LENGTH_SHORT).show();
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
                            JSONArray usersArray = jsonResponse.getJSONArray("users");
                            userList.clear();

                            for (int i = 0; i < usersArray.length(); i++) {
                                JSONObject user = usersArray.getJSONObject(i);

                                SearchItem searchItem = new SearchItem(
                                        user.getInt("user_id"),
                                        user.getString("full_name"),
                                        user.getString("first_name"),
                                        user.getString("last_name"),
                                        user.getString("gender"),
                                        user.getInt("age"),
                                        user.getString("bio"),
                                        user.getString("location"),
                                        user.getString("profile_image"),
                                        user.getBoolean("is_online"),
                                        user.getString("profile_type"),
                                        user.getBoolean("has_pending_request")
                                );

                                userList.add(searchItem);
                            }

                            searchAdapter.notifyDataSetChanged();

                            if (userList.isEmpty()) {
                                String message = query.isEmpty() ?
                                        "💭 No suggestions available\n\nTry adjusting your preferences" :
                                        "🔍 No results found for \"" + query + "\"\n\nTry different keywords";
                                showEmptyState(true, message);
                            } else {
                                showEmptyState(false, "");
                            }

                        } else {
                            String message = jsonResponse.getString("message");
                            showEmptyState(true, "⚠️ " + message);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        showEmptyState(true, "⚠️ Error loading results. Please try again.");
                    }
                });
            }
        });
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showEmptyState(boolean show, String message) {
        if (show) {
            emptyStateText.setText(message);
            emptyStateText.setVisibility(View.VISIBLE);
            resultsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            resultsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void goBack() {
        finish();
        overridePendingTransition(R.anim.slide_out_left, R.anim.slide_in_right);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediumAdView != null) {
            mediumAdView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediumAdView != null) {
            mediumAdView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediumAdView != null) {
            mediumAdView.destroy();
        }
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }
}*/