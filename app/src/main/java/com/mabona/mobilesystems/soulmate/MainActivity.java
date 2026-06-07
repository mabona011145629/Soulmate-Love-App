package com.mabona.mobilesystems.soulmate;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Gravity;
import android.view.LayoutInflater;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;
import android.content.pm.PackageManager;
import android.Manifest;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton, createAccountButton;
    private ProgressBar progressBar;
    private TextView verifyingText;
    private ImageView heartImage1, heartImage2, heartImage3;
    private AdView adView;
    private CheckBox keepLoggedInCheckbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request notification permissions for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // Check if user is already logged in with "Keep me logged in"
        if (LoginManager.isLoggedIn(this)) {
            String savedEmail = LoginManager.getSavedEmail(this);
            int savedUserId = LoginManager.getSavedUserId(this);
            String savedUserName = LoginManager.getSavedUserName(this);
            String savedToken = LoginManager.getSavedToken(this);

            if (savedUserId != -1 && !savedToken.isEmpty()) {
                // Start notification service for auto-login
                startNotificationService(savedUserId, savedToken);

                goToDashboard(savedEmail, savedUserId, savedUserName, savedToken);
                return;
            }
        }

        // Initialize views
        initializeViews();

        // Set saved email if exists
        String savedEmail = LoginManager.getSavedEmail(this);
        if (!savedEmail.isEmpty()) {
            emailEditText.setText(savedEmail);
            passwordEditText.requestFocus();
        }

        // Initialize Admob
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
                loadAd();
            }
        });

        // Setup animations
        setupAnimations();

        // Login button click
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animation shake = AnimationUtils.loadAnimation(MainActivity.this, R.anim.shake);
                loginButton.startAnimation(shake);

                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    showPinkToast("Please fill all fields");
                    return;
                }

                showVerifyingAnimation();
                performLogin(email, password);
            }
        });

        // Create account button click
        createAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animation shake = AnimationUtils.loadAnimation(MainActivity.this, R.anim.shake);
                createAccountButton.startAnimation(shake);

                Intent intent = new Intent(MainActivity.this, RegistrationActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

        // Forgot password click
        TextView forgotPasswordText = findViewById(R.id.forgotPasswordText);
        if (forgotPasswordText != null) {
            forgotPasswordText.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }
    }

    private void initializeViews() {
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        createAccountButton = findViewById(R.id.createAccountButton);
        progressBar = findViewById(R.id.progressBar);
        verifyingText = findViewById(R.id.verifyingText);
        heartImage1 = findViewById(R.id.heartImage1);
        heartImage2 = findViewById(R.id.heartImage2);
        heartImage3 = findViewById(R.id.heartImage3);
        adView = findViewById(R.id.adView);
        keepLoggedInCheckbox = findViewById(R.id.keepLoggedInCheckbox);
    }

    private void setupAnimations() {
        ObjectAnimator floatAnimation1 = ObjectAnimator.ofFloat(heartImage1, "translationY", -50f, 50f);
        floatAnimation1.setDuration(2000);
        floatAnimation1.setRepeatCount(ValueAnimator.INFINITE);
        floatAnimation1.setRepeatMode(ValueAnimator.REVERSE);
        floatAnimation1.start();

        ObjectAnimator floatAnimation2 = ObjectAnimator.ofFloat(heartImage2, "translationY", 50f, -50f);
        floatAnimation2.setDuration(2200);
        floatAnimation2.setRepeatCount(ValueAnimator.INFINITE);
        floatAnimation2.setRepeatMode(ValueAnimator.REVERSE);
        floatAnimation2.start();

        ObjectAnimator floatAnimation3 = ObjectAnimator.ofFloat(heartImage3, "translationX", -30f, 30f);
        floatAnimation3.setDuration(1800);
        floatAnimation3.setRepeatCount(ValueAnimator.INFINITE);
        floatAnimation3.setRepeatMode(ValueAnimator.REVERSE);
        floatAnimation3.start();
    }

    private void showVerifyingAnimation() {
        progressBar.setVisibility(View.VISIBLE);
        verifyingText.setVisibility(View.VISIBLE);
        loginButton.setEnabled(false);

        ObjectAnimator rotateHeart = ObjectAnimator.ofFloat(heartImage1, "rotation", 0f, 360f);
        rotateHeart.setDuration(1000);
        rotateHeart.setRepeatCount(ValueAnimator.INFINITE);
        rotateHeart.start();
    }

    private void hideVerifyingAnimation() {
        progressBar.setVisibility(View.GONE);
        verifyingText.setVisibility(View.GONE);
        loginButton.setEnabled(true);
    }

    private void performLogin(String email, String password) {
        OkHttpClient client = new OkHttpClient();

        RequestBody formBody = new FormBody.Builder()
                .add("email", email)
                .add("password", password)
                .add("action", "login")
                .build();

        Request request = new Request.Builder()
                .url("https://mabona.firstsuninvestment.com/soulmate/login.php")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    hideVerifyingAnimation();
                    showPinkToast("Network error. Please try again.");
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseBody = response.body().string();

                runOnUiThread(() -> {
                    hideVerifyingAnimation();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        boolean success = jsonResponse.getBoolean("success");

                        if (success) {
                            int userId = jsonResponse.optInt("dater_id", -1);
                            String firstName = jsonResponse.optString("first_name", "");
                            String token = jsonResponse.optString("token", "");

                            // Save login data using LoginManager
                            if (keepLoggedInCheckbox != null && keepLoggedInCheckbox.isChecked()) {
                                LoginManager.saveLogin(MainActivity.this, email, userId, firstName, token);
                            } else {
                                // Clear any existing saved data
                                LoginManager.forceLogout(MainActivity.this);
                            }

                            showPinkToast("Login successful!");

                            // Start notification service
                            startNotificationService(userId, token);

                            goToDashboard(email, userId, firstName, token);
                        } else {
                            String message = jsonResponse.getString("message");
                            showPinkToast(message);
                        }
                    } catch (JSONException e) {
                        showPinkToast("Invalid response from server");
                    }
                });
            }
        });
    }

    private void startNotificationService(int userId, String token) {
        try {
            Intent serviceIntent = new Intent(this, NotificationService.class);
            serviceIntent.putExtra("USER_ID", userId);
            serviceIntent.putExtra("AUTH_TOKEN", token);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void goToDashboard(String email, int userId, String firstName, String token) {
        Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
        intent.putExtra("USER_EMAIL", email);
        intent.putExtra("USER_ID", userId);
        intent.putExtra("USER_NAME", firstName);
        intent.putExtra("AUTH_TOKEN", token);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void loadAd() {
        if (adView != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        }
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

    @Override
    protected void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (adView != null) {
            adView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adView != null) {
            adView.destroy();
        }
    }
}