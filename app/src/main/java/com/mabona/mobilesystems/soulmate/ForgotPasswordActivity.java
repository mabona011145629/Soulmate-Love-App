package com.mabona.mobilesystems.soulmate;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
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
import android.view.Gravity;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

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

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText emailEditText;
    private Button resetButton;
    private ProgressBar progressBar;
    private TextView backToLoginText;
    private ImageView heartImage1, heartImage2, heartImage3;
    private AdView adView;

    private static final String BASE_URL = "https://mabona.firstsuninvestment.com/soulmate/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        initializeViews();
        setupAnimations();
        loadAd();

        resetButton.setOnClickListener(v -> {
            Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
            resetButton.startAnimation(shake);

            String email = emailEditText.getText().toString().trim();
            if (email.isEmpty()) {
                showPinkToast("Please enter your email");
                return;
            }

            performPasswordReset(email);
        });

        backToLoginText.setOnClickListener(v -> finish());
    }

    private void initializeViews() {
        emailEditText = findViewById(R.id.emailEditText);
        resetButton = findViewById(R.id.resetButton);
        progressBar = findViewById(R.id.progressBar);
        backToLoginText = findViewById(R.id.backToLoginText);
        heartImage1 = findViewById(R.id.heartImage1);
        heartImage2 = findViewById(R.id.heartImage2);
        heartImage3 = findViewById(R.id.heartImage3);
        adView = findViewById(R.id.bannerAdView);
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

    private void performPasswordReset(String email) {
        showProgress(true);
        OkHttpClient client = new OkHttpClient();

        RequestBody formBody = new FormBody.Builder()
                .add("email", email)
                .add("action", "forgot_password")
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + "login.php")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    showPinkToast("Network error");
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseBody = response.body().string();
                runOnUiThread(() -> {
                    showProgress(false);
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        boolean success = jsonResponse.getBoolean("success");
                        String message = jsonResponse.getString("message");
                        showPinkToast(message);
                        if (success) {
                            finish(); // Go back to login
                        }
                    } catch (JSONException e) {
                        showPinkToast("Error parsing response");
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
        resetButton.setEnabled(!show);
    }

    private void loadAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adView != null) adView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (adView != null) adView.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adView != null) adView.destroy();
    }
}
