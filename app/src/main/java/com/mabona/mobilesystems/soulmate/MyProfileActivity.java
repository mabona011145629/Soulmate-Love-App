package com.mabona.mobilesystems.soulmate;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ArgbEvaluator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MyProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 100;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final String BASE_URL = "https://mabona.firstsuninvestment.com/soulmate/";

    // UI Components
    private ImageView backButton, profileImageView, heartImage1, heartImage2, heartImage3;
    private TextView aboutDeveloperButton, changePhotoText;
    private TextInputEditText firstNameInput, lastNameInput, phoneInput, locationInput, countryInput, bioInput;
    private TextInputEditText currentPasswordInput, newPasswordInput, confirmPasswordInput;
    private TextInputEditText feedbackInput;
    private Spinner genderSpinner;
    private CheckBox hideProfileCheckbox;
    private CardView saveButton, logoutButton;
    private ProgressBar progressBar;
    private AdView bannerAdView;

    // Data
    private int userId;
    private String authToken;
    private String userEmail;
    private String userName;
    private String currentProfileImagePath;
    private Uri selectedImageUri;
    private boolean isImageChanged = false;

    private OkHttpClient client;
    private Handler blinkHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);

        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        // Get user data
        userId = getIntent().getIntExtra("USER_ID", -1);
        authToken = getIntent().getStringExtra("AUTH_TOKEN");
        userEmail = getIntent().getStringExtra("USER_EMAIL");
        userName = getIntent().getStringExtra("USER_NAME");

        if (userId == -1 || authToken == null) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupAnimations();
        setupGenderSpinner();
        setupClickListeners();
        loadAds();
        startBlinkingAnimation();
        loadProfile();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        profileImageView = findViewById(R.id.profileImageView);
        aboutDeveloperButton = findViewById(R.id.aboutDeveloperButton);
        changePhotoText = findViewById(R.id.changePhotoText);
        heartImage1 = findViewById(R.id.heartImage1);
        heartImage2 = findViewById(R.id.heartImage2);
        heartImage3 = findViewById(R.id.heartImage3);

        firstNameInput = findViewById(R.id.firstNameInput);
        lastNameInput = findViewById(R.id.lastNameInput);
        phoneInput = findViewById(R.id.phoneInput);
        locationInput = findViewById(R.id.locationInput);
        countryInput = findViewById(R.id.countryInput);
        bioInput = findViewById(R.id.bioInput);

        currentPasswordInput = findViewById(R.id.currentPasswordInput);
        newPasswordInput = findViewById(R.id.newPasswordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);

        feedbackInput = findViewById(R.id.feedbackInput);

        genderSpinner = findViewById(R.id.genderSpinner);
        hideProfileCheckbox = findViewById(R.id.hideProfileCheckbox);

        saveButton = findViewById(R.id.saveButton);
        logoutButton = findViewById(R.id.logoutButton);
        progressBar = findViewById(R.id.progressBar);
        bannerAdView = findViewById(R.id.bannerAdView);

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

    private void setupGenderSpinner() {
        String[] genders = {"Select Gender", "Male", "Female", "Lesbian", "Gay", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, genders);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(adapter);
    }

    private void setupClickListeners() {
        aboutDeveloperButton.setOnClickListener(v -> {
            Animation bounce = AnimationUtils.loadAnimation(this, R.anim.bounce);
            aboutDeveloperButton.startAnimation(bounce);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://mabona.firstsuninvestment.com"));
            startActivity(intent);
        });

        changePhotoText.setOnClickListener(v -> checkAndPickImage());

        saveButton.setOnClickListener(v -> {
            Animation bounce = AnimationUtils.loadAnimation(this, R.anim.bounce);
            saveButton.startAnimation(bounce);

            // Check if password change is requested
            String currentPwd = currentPasswordInput.getText().toString().trim();
            String newPwd = newPasswordInput.getText().toString().trim();
            String confirmPwd = confirmPasswordInput.getText().toString().trim();

            if (!currentPwd.isEmpty() || !newPwd.isEmpty() || !confirmPwd.isEmpty()) {
                changePassword(currentPwd, newPwd, confirmPwd);
            } else {
                updateProfile();
            }
        });

        logoutButton.setOnClickListener(v -> {
            Animation bounce = AnimationUtils.loadAnimation(this, R.anim.bounce);
            logoutButton.startAnimation(bounce);
            showLogoutConfirmation();
        });

        TextView privacyLink = findViewById(R.id.privacyPolicyLink);
        TextView termsLink = findViewById(R.id.termsLink);

        privacyLink.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://mabona.firstsuninvestment.com/soulmate/privacy_policy.html"));
            startActivity(intent);
        });

        termsLink.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://mabona.firstsuninvestment.com/soulmate/terms_of_use.html"));
            startActivity(intent);
        });
    }

    private void startBlinkingAnimation() {
        blinkHandler = new Handler();
        Runnable blinkRunnable = new Runnable() {
            private boolean isVisible = true;

            @Override
            public void run() {
                if (aboutDeveloperButton != null) {
                    aboutDeveloperButton.setAlpha(isVisible ? 1.0f : 0.5f);
                    isVisible = !isVisible;
                }
                blinkHandler.postDelayed(this, 500);
            }
        };
        blinkHandler.post(blinkRunnable);
    }

    private void loadProfile() {
        showProgress(true);

        Request request = new Request.Builder()
                .url(BASE_URL + "myprofile.php?action=get_profile&token=" + authToken)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(MyProfileActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body().string();
                runOnUiThread(() -> {
                    showProgress(false);
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        if (json.getBoolean("success")) {
                            JSONObject profile = json.getJSONObject("profile");
                            displayProfile(profile);
                        } else {
                            Toast.makeText(MyProfileActivity.this, json.optString("message", "Failed to load profile"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(MyProfileActivity.this, "Error parsing data", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void displayProfile(JSONObject profile) throws JSONException {
        firstNameInput.setText(profile.optString("first_name", ""));
        lastNameInput.setText(profile.optString("last_name", ""));
        phoneInput.setText(profile.optString("phone_number", ""));
        locationInput.setText(profile.optString("location_name", ""));
        countryInput.setText(profile.optString("country", ""));
        bioInput.setText(profile.optString("bio", ""));

        String gender = profile.optString("gender", "");
        int genderPosition = getGenderPosition(gender);
        if (genderPosition >= 0) {
            genderSpinner.setSelection(genderPosition);
        }

        hideProfileCheckbox.setChecked(profile.optInt("hide_profile_details", 0) == 1);

        currentProfileImagePath = profile.optString("profile_image", null);
        if (currentProfileImagePath != null && !currentProfileImagePath.isEmpty()) {
            String imageUrl = BASE_URL + "get_image.php?path=" + currentProfileImagePath + "&token=" + authToken;
            Glide.with(this)
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .into(profileImageView);
        }
    }

    private int getGenderPosition(String gender) {
        String[] genders = {"male", "female", "lesbian", "gay", "other"};
        for (int i = 0; i < genders.length; i++) {
            if (genders[i].equalsIgnoreCase(gender)) {
                return i + 1; // +1 because first item is "Select Gender"
            }
        }
        return 0;
    }

    private String getSelectedGender() {
        int position = genderSpinner.getSelectedItemPosition();
        String[] genders = {"", "male", "female", "lesbian", "gay", "other"};
        if (position > 0 && position < genders.length) {
            return genders[position];
        }
        return "";
    }

    private void checkAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_REQUEST_CODE);
                return;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                return;
            }
        }
        openImagePicker();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            isImageChanged = true;
            profileImageView.setImageURI(selectedImageUri);
        }
    }

    private void updateProfile() {
        showProgress(true);

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("action", "update_profile")
                .addFormDataPart("token", authToken)
                .addFormDataPart("first_name", firstNameInput.getText().toString().trim())
                .addFormDataPart("last_name", lastNameInput.getText().toString().trim())
                .addFormDataPart("gender", getSelectedGender())
                .addFormDataPart("bio", bioInput.getText().toString().trim())
                .addFormDataPart("phone_number", phoneInput.getText().toString().trim())
                .addFormDataPart("location_name", locationInput.getText().toString().trim())
                .addFormDataPart("country", countryInput.getText().toString().trim())
                .addFormDataPart("hide_profile", hideProfileCheckbox.isChecked() ? "1" : "0");

        if (isImageChanged && selectedImageUri != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                byte[] fileBytes = getBytes(inputStream);
                builder.addFormDataPart("profile_image", "profile.jpg", RequestBody.create(MediaType.parse("image/jpeg"), fileBytes));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        RequestBody body = builder.build();
        Request request = new Request.Builder()
                .url(BASE_URL + "myprofile.php")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(MyProfileActivity.this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body().string();
                runOnUiThread(() -> {
                    showProgress(false);
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        if (json.getBoolean("success")) {
                            Toast.makeText(MyProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                            isImageChanged = false;
                            loadProfile();

                            // Also submit feedback if any
                            String feedback = feedbackInput.getText().toString().trim();
                            if (!feedback.isEmpty()) {
                                submitFeedback(feedback);
                            }
                        } else {
                            Toast.makeText(MyProfileActivity.this, json.optString("message", "Update failed"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(MyProfileActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void submitFeedback(String feedback) {
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("action", "submit_feedback")
                .addFormDataPart("token", authToken)
                .addFormDataPart("feedback_text", feedback)
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + "myprofile.php")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> {
                    feedbackInput.setText("");
                    Toast.makeText(MyProfileActivity.this, "Thank you for your feedback! 💕", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void changePassword(String currentPwd, String newPwd, String confirmPwd) {
        if (currentPwd.isEmpty()) {
            Toast.makeText(this, "Please enter current password", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newPwd.length() < 6) {
            Toast.makeText(this, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newPwd.equals(confirmPwd)) {
            Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress(true);

        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("action", "change_password")
                .addFormDataPart("token", authToken)
                .addFormDataPart("current_password", currentPwd)
                .addFormDataPart("new_password", newPwd)
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + "myprofile.php")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(MyProfileActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body().string();
                runOnUiThread(() -> {
                    showProgress(false);
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        if (json.getBoolean("success")) {
                            Toast.makeText(MyProfileActivity.this, "Password changed successfully!", Toast.LENGTH_SHORT).show();
                            currentPasswordInput.setText("");
                            newPasswordInput.setText("");
                            confirmPasswordInput.setText("");
                        } else {
                            Toast.makeText(MyProfileActivity.this, json.optString("message", "Failed"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(MyProfileActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }
//thinking about this one
    /*
    private void logout() {
        SharedPreferences prefs = getSharedPreferences("SoulmatePrefs", MODE_PRIVATE);
        prefs.edit().clear().apply();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }*/
private void logout() {
    // Clear login data
    LoginManager.forceLogout(this);

    // Stop notification service
    NotificationService.stopNotificationService(this);

    // Go to login
    Intent intent = new Intent(this, MainActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    finish();
}

    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    private void loadAds() {
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

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        saveButton.setEnabled(!show);
        logoutButton.setEnabled(!show);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openImagePicker();
        } else {
            Toast.makeText(this, "Permission needed to change profile photo", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bannerAdView != null) bannerAdView.resume();
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
        if (blinkHandler != null) blinkHandler.removeCallbacksAndMessages(null);
    }
}