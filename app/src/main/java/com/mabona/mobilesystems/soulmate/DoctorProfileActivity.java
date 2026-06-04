package com.mabona.mobilesystems.soulmate;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DoctorProfileActivity extends AppCompatActivity {

    private static final String TAG = "DoctorProfile";

    // UI Components
    private ImageView backButton;
    private ImageView profileImageView;
    private ImageView onlineIndicator;
    private TextView nameTextView;
    private TextView qualificationBadge;
    private TextView ratingText;
    private TextView reviewCountText;
    private TextView sessionsText;
    private TextView bioTextView;
    private TextView qualificationsText;
    private TextView sessionPriceText;
    private TextView hourlyRateText;
    private CardView chatButton;
    private ProgressBar progressBar;
    private ImageView heartImage1, heartImage2, heartImage3;

    // Data
    private int userId;
    private String authToken;
    private String userEmail;
    private String userName;
    private int doctorId;
    private String doctorName;

    private OkHttpClient client;
    private static final String BASE_URL = "https://mabona.firstsuninvestment.com/soulmate/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_profile);

        client = new OkHttpClient();

        // Get intent data
        userId = getIntent().getIntExtra("USER_ID", -1);
        authToken = getIntent().getStringExtra("AUTH_TOKEN");
        userEmail = getIntent().getStringExtra("USER_EMAIL");
        userName = getIntent().getStringExtra("USER_NAME");
        doctorId = getIntent().getIntExtra("DOCTOR_ID", -1);
        doctorName = getIntent().getStringExtra("DOCTOR_NAME");

        Log.d(TAG, "onCreate: userId=" + userId + ", doctorId=" + doctorId + ", authToken=" + authToken);

        if (userId == -1 || authToken == null || doctorId == -1) {
            Toast.makeText(this, "Invalid profile data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupAnimations();
        setupListeners();
        loadDoctorProfile();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        profileImageView = findViewById(R.id.profileImageView);
        onlineIndicator = findViewById(R.id.onlineIndicator);
        nameTextView = findViewById(R.id.nameTextView);
        qualificationBadge = findViewById(R.id.qualificationBadge);
        ratingText = findViewById(R.id.ratingText);
        reviewCountText = findViewById(R.id.reviewCountText);
        sessionsText = findViewById(R.id.sessionsText);
        bioTextView = findViewById(R.id.bioTextView);
        qualificationsText = findViewById(R.id.qualificationsText);
        sessionPriceText = findViewById(R.id.sessionPriceText);
        hourlyRateText = findViewById(R.id.hourlyRateText);
        chatButton = findViewById(R.id.chatButton);
        progressBar = findViewById(R.id.progressBar);
        heartImage1 = findViewById(R.id.heartImage1);
        heartImage2 = findViewById(R.id.heartImage2);
        heartImage3 = findViewById(R.id.heartImage3);

        backButton.setOnClickListener(v -> finish());
    }

    private void setupAnimations() {
        ObjectAnimator floatAnim1 = ObjectAnimator.ofFloat(heartImage1, "translationY", -20f, 20f);
        floatAnim1.setDuration(2000);
        floatAnim1.setRepeatCount(ObjectAnimator.INFINITE);
        floatAnim1.setRepeatMode(ObjectAnimator.REVERSE);
        floatAnim1.start();

        ObjectAnimator floatAnim2 = ObjectAnimator.ofFloat(heartImage2, "translationY", 20f, -20f);
        floatAnim2.setDuration(2200);
        floatAnim2.setRepeatCount(ObjectAnimator.INFINITE);
        floatAnim2.setRepeatMode(ObjectAnimator.REVERSE);
        floatAnim2.start();

        ObjectAnimator floatAnim3 = ObjectAnimator.ofFloat(heartImage3, "translationX", -15f, 15f);
        floatAnim3.setDuration(1800);
        floatAnim3.setRepeatCount(ObjectAnimator.INFINITE);
        floatAnim3.setRepeatMode(ObjectAnimator.REVERSE);
        floatAnim3.start();
    }

    private void setupListeners() {
        chatButton.setOnClickListener(v -> {
            Animation bounce = AnimationUtils.loadAnimation(DoctorProfileActivity.this, R.anim.bounce);
            chatButton.startAnimation(bounce);
            checkAndStartTherapy();
        });
    }

    private void loadDoctorProfile() {
        showProgress(true);

        String url = BASE_URL + "dr.php?action=get_doctor&token=" + authToken + "&doctor_id=" + doctorId;
        Log.d(TAG, "Loading doctor profile from: " + url);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    Log.e(TAG, "Network error: " + e.getMessage());
                    Toast.makeText(DoctorProfileActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, "Server response: " + responseBody);

                runOnUiThread(() -> {
                    showProgress(false);
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        boolean success = json.optBoolean("success", false);

                        if (success) {
                            JSONObject doctor = json.optJSONObject("doctor");
                            if (doctor != null) {
                                displayDoctorProfile(doctor);
                            } else {
                                Toast.makeText(DoctorProfileActivity.this, "Doctor data not found", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            String message = json.optString("message", "Failed to load profile");
                            Log.e(TAG, "API error: " + message);
                            Toast.makeText(DoctorProfileActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                        Log.e(TAG, "Response was: " + responseBody);
                        Toast.makeText(DoctorProfileActivity.this, "Error parsing data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void displayDoctorProfile(JSONObject doctor) {
        try {
            String fullName = doctor.optString("full_name", "Love Doctor");
            String bio = doctor.optString("bio", "Love Doctor ready to help you find healing and happiness 💕");
            String qualificationType = doctor.optString("qualification_type", "Counsellor");
            String qualifications = doctor.optString("qualifications", "Professional love and relationship counsellor");
            double rating = doctor.optDouble("love_doctor_rating", 0);
            int reviewCount = doctor.optInt("review_count", 0);
            int sessionsCount = doctor.optInt("love_doctor_sessions", 0);
            double sessionPrice = doctor.optDouble("session_price", 150);
            double hourlyRate = doctor.optDouble("hourly_rate", 80);
            boolean isOnline = doctor.optBoolean("is_online", false);
            String profileImage = doctor.optString("profile_image", null);

            Log.d(TAG, "Displaying doctor: " + fullName + ", rating=" + rating);

            nameTextView.setText(fullName);

            // Capitalize qualification type
            String qualDisplay = qualificationType.substring(0, 1).toUpperCase() + qualificationType.substring(1);
            qualificationBadge.setText(qualDisplay);

            bioTextView.setText(bio);
            qualificationsText.setText(qualifications);

            if (rating > 0) {
                ratingText.setText("⭐ " + rating);
            } else {
                ratingText.setText("⭐ Not rated yet");
            }

            reviewCountText.setText("(" + reviewCount + " reviews)");
            sessionsText.setText(sessionsCount + "+ successful sessions");
            sessionPriceText.setText("E" + String.format("%.2f", sessionPrice) + " per session");
            hourlyRateText.setText("E" + String.format("%.2f", hourlyRate) + " per hour");
            onlineIndicator.setVisibility(isOnline ? View.VISIBLE : View.GONE);

            // Load profile image
            if (profileImage != null && !profileImage.isEmpty() && !profileImage.equals("null")) {
                String imageUrl = BASE_URL + "get_image.php?path=" + profileImage + "&token=" + authToken;
                Glide.with(this)
                        .load(imageUrl)
                        .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .into(profileImageView);
            } else {
                profileImageView.setImageResource(R.drawable.default_profile);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error displaying profile: " + e.getMessage());
            Toast.makeText(this, "Error displaying profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void checkAndStartTherapy() {
        showProgress(true);

        String url = BASE_URL + "dr.php?action=check_pin_status&user_id=" + userId + "&token=" + authToken;
        Log.d(TAG, "Checking PIN status: " + url);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    Log.e(TAG, "PIN check network error: " + e.getMessage());
                    Toast.makeText(DoctorProfileActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, "PIN status response: " + responseBody);

                runOnUiThread(() -> {
                    showProgress(false);
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        boolean success = json.optBoolean("success", false);

                        if (success) {
                            boolean hasPin = json.optBoolean("has_pin", false);
                            if (hasPin) {
                                showPinVerificationDialog();
                            } else {
                                showFirstTimeTherapyDialog();
                            }
                        } else {
                            // If API fails, default to first-time dialog
                            showFirstTimeTherapyDialog();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "PIN status parse error: " + e.getMessage());
                        showFirstTimeTherapyDialog();
                    }
                });
            }
        });
    }

    private void showFirstTimeTherapyDialog() {
        android.app.Dialog dialog = new android.app.Dialog(DoctorProfileActivity.this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_love_doctor_entry);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView messageText = dialog.findViewById(R.id.messageText);
        EditText pinInput = dialog.findViewById(R.id.pinInput);
        LinearLayout pinLayout = dialog.findViewById(R.id.pinLayout);
        CardView enterButton = dialog.findViewById(R.id.enterButton);
        TextView enterButtonText = dialog.findViewById(R.id.enterButtonText);
        CardView cancelButton = dialog.findViewById(R.id.cancelButton);

        messageText.setText("💕 It is time to let your fears out, talk about what makes you upset about your partner and get advice from professional love doctors.\n\n💝 Your chats are PIN protected and secure!");
        pinLayout.setVisibility(View.VISIBLE);

        if (enterButtonText != null) {
            enterButtonText.setText("Create PIN & Start Therapy");
        }

        enterButton.setOnClickListener(v -> {
            String pin = pinInput.getText().toString().trim();
            if (pin.length() < 4) {
                Toast.makeText(DoctorProfileActivity.this, "Please enter a PIN with at least 4 digits", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            savePinAndStartSession(pin);
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showPinVerificationDialog() {
        android.app.Dialog dialog = new android.app.Dialog(DoctorProfileActivity.this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_pin_verification);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        EditText pinInput = dialog.findViewById(R.id.pinInput);
        CardView verifyButton = dialog.findViewById(R.id.verifyButton);
        CardView cancelButton = dialog.findViewById(R.id.cancelButton);

        verifyButton.setOnClickListener(v -> {
            String pin = pinInput.getText().toString().trim();
            if (pin.isEmpty()) {
                Toast.makeText(DoctorProfileActivity.this, "Please enter your PIN", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            verifyPinAndStartSession(pin);
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void savePinAndStartSession(String pin) {
        showProgress(true);

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("action", "save_pin")
                .addFormDataPart("token", authToken)
                .addFormDataPart("user_id", String.valueOf(userId))
                .addFormDataPart("pin", pin);

        okhttp3.RequestBody body = builder.build();

        Request request = new Request.Builder()
                .url(BASE_URL + "dr.php")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(DoctorProfileActivity.this, "Failed to save PIN", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> {
                    showProgress(false);
                    startTherapySession(pin);
                });
            }
        });
    }

    private void verifyPinAndStartSession(String pin) {
        showProgress(true);

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("action", "verify_pin")
                .addFormDataPart("token", authToken)
                .addFormDataPart("user_id", String.valueOf(userId))
                .addFormDataPart("pin", pin);

        okhttp3.RequestBody body = builder.build();

        Request request = new Request.Builder()
                .url(BASE_URL + "dr.php")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(DoctorProfileActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body().string();
                runOnUiThread(() -> {
                    showProgress(false);
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        if (json.optBoolean("success", false)) {
                            startTherapySession(pin);
                        } else {
                            Toast.makeText(DoctorProfileActivity.this, "Invalid PIN. Please try again.", Toast.LENGTH_SHORT).show();
                            showPinVerificationDialog();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(DoctorProfileActivity.this, "Error verifying PIN", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void startTherapySession(String pin) {
        showProgress(true);

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("action", "start_session")
                .addFormDataPart("token", authToken)
                .addFormDataPart("doctor_id", String.valueOf(doctorId))
                .addFormDataPart("session_pin", pin);

        okhttp3.RequestBody body = builder.build();

        Request request = new Request.Builder()
                .url(BASE_URL + "dr.php")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(DoctorProfileActivity.this, "Failed to start session", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body().string();
                runOnUiThread(() -> {
                    showProgress(false);
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        if (json.optBoolean("success", false)) {
                            int sessionId = json.optInt("session_id", 0);

                            Intent intent = new Intent(DoctorProfileActivity.this, LoveDoctorChatActivity.class);
                            intent.putExtra("USER_ID", userId);
                            intent.putExtra("AUTH_TOKEN", authToken);
                            intent.putExtra("USER_EMAIL", userEmail);
                            intent.putExtra("USER_NAME", userName);
                            intent.putExtra("DOCTOR_ID", doctorId);
                            intent.putExtra("DOCTOR_NAME", doctorName);
                            intent.putExtra("SESSION_ID", sessionId);
                            intent.putExtra("SESSION_PIN", pin);
                            startActivity(intent);
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        } else {
                            String message = json.optString("message", "Failed to start session");
                            Toast.makeText(DoctorProfileActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(DoctorProfileActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        chatButton.setEnabled(!show);
    }
}