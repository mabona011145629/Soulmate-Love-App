package com.mabona.mobilesystems.soulmate;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Gravity;
import android.view.LayoutInflater;

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
import okhttp3.FormBody;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DoctorProfileActivity extends AppCompatActivity {

    private ImageView backButton, profileImageView, onlineIndicator;
    private TextView nameTextView, qualificationBadge, ratingText, reviewCountText, sessionsText, bioTextView, qualificationsText, sessionPriceText, hourlyRateText;
    private CardView chatButton, submitRatingButton;
    private RatingBar doctorRatingBar;
    private ProgressBar progressBar;
    private ImageView heartImage1, heartImage2, heartImage3;

    private int userId, doctorId;
    private String authToken, userEmail, userName;
    private static final String BASE_URL = "https://mabona.firstsuninvestment.com/soulmate/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_profile);

        userId = getIntent().getIntExtra("USER_ID", -1);
        authToken = getIntent().getStringExtra("AUTH_TOKEN");
        doctorId = getIntent().getIntExtra("DOCTOR_ID", -1);

        if (userId == -1 || doctorId == -1) { finish(); return; }

        initializeViews();
        setupAnimations();
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
        submitRatingButton = findViewById(R.id.submitRatingButton);
        doctorRatingBar = findViewById(R.id.doctorRatingBar);
        progressBar = findViewById(R.id.progressBar);
        heartImage1 = findViewById(R.id.heartImage1);
        heartImage2 = findViewById(R.id.heartImage2);
        heartImage3 = findViewById(R.id.heartImage3);

        backButton.setOnClickListener(v -> finish());
        chatButton.setOnClickListener(v -> checkAndStartTherapy());
        submitRatingButton.setOnClickListener(v -> submitRating());
    }

    private void loadDoctorProfile() {
        showProgress(true);
        new OkHttpClient().newCall(new Request.Builder().url(BASE_URL + "dr.php?action=get_doctor&token=" + authToken + "&doctor_id=" + doctorId).get().build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) { runOnUiThread(() -> showProgress(false)); }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                runOnUiThread(() -> {
                    showProgress(false);
                    try {
                        JSONObject json = new JSONObject(body);
                        if (json.getBoolean("success")) displayProfile(json.getJSONObject("doctor"));
                    } catch (Exception e) { showPinkToast("Parse Error"); }
                });
            }
        });
    }

    private void displayProfile(JSONObject doc) throws JSONException {
        nameTextView.setText(doc.optString("full_name", "Doctor"));
        qualificationBadge.setText(doc.optString("qualification_type", "Specialist"));
        bioTextView.setText(doc.optString("bio", "No bio available"));
        qualificationsText.setText(doc.optString("qualifications", "Not specified"));
        
        double rating = doc.optDouble("love_doctor_rating", doc.optDouble("rating", 0.0));
        ratingText.setText("⭐ " + rating);
        
        reviewCountText.setText("(" + doc.optInt("review_count", 0) + " reviews)");
        sessionsText.setText(doc.optInt("love_doctor_sessions", 0) + " sessions");
        sessionPriceText.setText("E" + doc.optDouble("session_price", 0.0) + "/session");
        hourlyRateText.setText("E" + doc.optDouble("hourly_rate", 0.0) + "/hour");
        onlineIndicator.setVisibility(doc.optInt("is_online", 0) == 1 ? View.VISIBLE : View.GONE);
        
        if (doc.has("user_rating")) {
            doctorRatingBar.setRating((float) doc.optDouble("user_rating", 0.0));
        }

        String profileImg = doc.optString("profile_image", "");
        Glide.with(this)
                .load(BASE_URL + "get_image.php?path=" + profileImg + "&token=" + authToken)
                .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                .placeholder(R.drawable.default_profile)
                .into(profileImageView);
    }

    private void submitRating() {
        float rating = doctorRatingBar.getRating();
        if (rating < 1) { showPinkToast("Please select at least 1 star"); return; }

        RequestBody body = new FormBody.Builder()
                .add("action", "rate_doctor")
                .add("token", authToken)
                .add("doctor_id", String.valueOf(doctorId))
                .add("rating", String.valueOf((int)rating))
                .build();

        new OkHttpClient().newCall(new Request.Builder().url(BASE_URL + "dr.php").post(body).build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}
            @Override
            public void onResponse(Call call, Response response) {
                runOnUiThread(() -> {
                    showPinkToast("Thank you for your rating! 💖");
                    loadDoctorProfile();
                });
            }
        });
    }

    private void checkAndStartTherapy() {
        showProgress(true);

        RequestBody body = new FormBody.Builder()
                .add("action", "start_session")
                .add("token", authToken)
                .add("doctor_id", String.valueOf(doctorId))
                .build();

        new OkHttpClient().newCall(new Request.Builder().url(BASE_URL + "dr.php").post(body).build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    showPinkToast("Connection failed");
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
                            int sessionId = json.getInt("session_id");
                            Intent intent = new Intent(DoctorProfileActivity.this, LoveDoctorChatActivity.class);
                            intent.putExtra("USER_ID", userId);
                            intent.putExtra("AUTH_TOKEN", authToken);
                            intent.putExtra("DOCTOR_ID", doctorId);
                            intent.putExtra("SESSION_ID", sessionId);
                            intent.putExtra("DOCTOR_NAME", nameTextView.getText().toString());
                            startActivity(intent);
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        } else {
                            showPinkToast("Could not start session");
                        }
                    } catch (Exception e) {
                        showPinkToast("Error starting therapy");
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

    private void showProgress(boolean show) { progressBar.setVisibility(show ? View.VISIBLE : View.GONE); }

    private void setupAnimations() {
        ObjectAnimator anim1 = ObjectAnimator.ofFloat(heartImage1, "translationY", -20f, 20f);
        anim1.setDuration(2000);
        anim1.setRepeatCount(ObjectAnimator.INFINITE);
        anim1.setRepeatMode(ObjectAnimator.REVERSE);
        anim1.start();

        ObjectAnimator anim2 = ObjectAnimator.ofFloat(heartImage2, "translationY", 20f, -20f);
        anim2.setDuration(2200);
        anim2.setRepeatCount(ObjectAnimator.INFINITE);
        anim2.setRepeatMode(ObjectAnimator.REVERSE);
        anim2.start();

        ObjectAnimator anim3 = ObjectAnimator.ofFloat(heartImage3, "translationX", -15f, 15f);
        anim3.setDuration(1800);
        anim3.setRepeatCount(ObjectAnimator.INFINITE);
        anim3.setRepeatMode(ObjectAnimator.REVERSE);
        anim3.start();
    }
}
