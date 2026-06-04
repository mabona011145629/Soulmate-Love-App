package com.mabona.mobilesystems.soulmate;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

    /* COMMENTED OUT - SharedPreferences for saving login data
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "SoulmatePrefs";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_TOKEN = "auth_token";
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* COMMENTED OUT - Initialize SharedPreferences and auto-login check
        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Check if user is already logged in
        if (isUserLoggedIn()) {
            // Auto-login - go directly to Dashboard
            String savedEmail = sharedPreferences.getString(KEY_EMAIL, "");
            if (!savedEmail.isEmpty()) {
                goToDashboard(savedEmail);
                return; // Exit onCreate, no need to initialize login UI
            }
        }
        */

        // Initialize views
        initializeViews();

        /* COMMENTED OUT - Auto-fill saved email
        // Set saved email if exists
        String savedEmail = sharedPreferences.getString(KEY_EMAIL, "");
        if (!savedEmail.isEmpty()) {
            emailEditText.setText(savedEmail);
            // Move cursor to password field for convenience
            passwordEditText.requestFocus();
        }
        */

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
                // Add shake animation
                Animation shake = AnimationUtils.loadAnimation(MainActivity.this, R.anim.shake);
                loginButton.startAnimation(shake);

                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Show verifying animation
                showVerifyingAnimation();

                // Send login request
                performLogin(email, password);
            }
        });

        // Create account button click
        createAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animation shake = AnimationUtils.loadAnimation(MainActivity.this, R.anim.shake);
                createAccountButton.startAnimation(shake);

                //Navigate to registration activity
                Intent intent = new Intent(MainActivity.this, RegistrationActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });
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
    }

    /* COMMENTED OUT - Auto-login helper methods
    private boolean isUserLoggedIn() {
        // Check if user is marked as logged in
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    private void saveLoginData(String email, String token) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_TOKEN, token);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    private void clearLoginData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_EMAIL);
        editor.remove(KEY_TOKEN);
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.apply();
    }

    private void updateEmailOnly(String email) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_EMAIL, email);
        editor.apply();
    }
    */

    private void setupAnimations() {
        // Floating animation for hearts
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

        // Rotating heart animation
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
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideVerifyingAnimation();
                        Toast.makeText(MainActivity.this, "Network error. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseBody = response.body().string();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideVerifyingAnimation();
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            boolean success = jsonResponse.getBoolean("success");

                            if (success) {
                                /* COMMENTED OUT - Save login data
                                // Save login data
                                String token = jsonResponse.optString("token", "");
                                saveLoginData(email, token);
                                */

                                Toast.makeText(MainActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();

                                // Parse user data from response and pass to Dashboard
                                int userId = jsonResponse.optInt("dater_id", -1);
                                String firstName = jsonResponse.optString("first_name", "");
                                String token = jsonResponse.optString("token", "");

                                goToDashboard(email, userId, firstName, token);
                            } else {
                                /* COMMENTED OUT - Clear login data on failure
                                // If login fails, clear any saved data (password change scenario)
                                clearLoginData();
                                */
                                String message = jsonResponse.getString("message");
                                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            /* COMMENTED OUT
                            clearLoginData();
                            */
                            Toast.makeText(MainActivity.this, "Invalid response from server", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    private void goToDashboard(String email, int userId, String firstName, String token) {
        Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
        intent.putExtra("USER_EMAIL", email);
        intent.putExtra("USER_ID", userId);
        intent.putExtra("USER_NAME", firstName);
        intent.putExtra("AUTH_TOKEN", token);
        startActivity(intent);
        finish(); // Close MainActivity so user can't go back
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    /* COMMENTED OUT - Force re-login method
    // Public method to force re-login (called from other activities when password changes)
    public static void forceReLogin(Context context, String newEmail) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_EMAIL, newEmail);
        editor.putBoolean(KEY_IS_LOGGED_IN, false); // Force login again
        editor.apply();

        // Start login activity
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
    */

    private void loadAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
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