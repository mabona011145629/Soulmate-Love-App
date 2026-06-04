package com.mabona.mobilesystems.soulmate;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoveDoctorRequestActivity extends AppCompatActivity {

    // UI Components
    private ImageView backButton;
    private TextView titleText;
    private NestedScrollView scrollView;
    private ProgressBar progressBar;
    private LinearLayout formContainer;

    // Form Fields
    private TextInputLayout summaryLayout;
    private TextInputEditText summaryInput;

    private TextInputLayout qualificationsLayout;
    private TextInputEditText qualificationsInput;

    private TextInputLayout qualificationTypeLayout;
    private TextInputEditText qualificationTypeInput;

    private TextInputLayout facebookLayout;
    private TextInputEditText facebookInput;

    private TextInputLayout youtubeLayout;
    private TextInputEditText youtubeInput;

    private TextInputLayout instagramLayout;
    private TextInputEditText instagramInput;

    private TextInputLayout hourlyRateLayout;
    private TextInputEditText hourlyRateInput;

    private TextInputLayout sessionPriceLayout;
    private TextInputEditText sessionPriceInput;

    private CardView submitButton;
    private TextView submitButtonText;
    private TextView termsText;

    // Floating Hearts
    private ImageView heartImage1, heartImage2, heartImage3;

    // Data
    private int userId;
    private String authToken;
    private String userEmail;
    private String userName;

    // Constants
    private static final String BASE_URL = "https://mabona.firstsuninvestment.com/soulmate/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_love_doctor_request);

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
        setupListeners();
        setupQualificationTypeSelector();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        titleText = findViewById(R.id.titleText);
        scrollView = findViewById(R.id.scrollView);
        progressBar = findViewById(R.id.progressBar);
        formContainer = findViewById(R.id.formContainer);

        summaryLayout = findViewById(R.id.summaryLayout);
        summaryInput = findViewById(R.id.summaryInput);

        qualificationsLayout = findViewById(R.id.qualificationsLayout);
        qualificationsInput = findViewById(R.id.qualificationsInput);

        qualificationTypeLayout = findViewById(R.id.qualificationTypeLayout);
        qualificationTypeInput = findViewById(R.id.qualificationTypeInput);

        facebookLayout = findViewById(R.id.facebookLayout);
        facebookInput = findViewById(R.id.facebookInput);

        youtubeLayout = findViewById(R.id.youtubeLayout);
        youtubeInput = findViewById(R.id.youtubeInput);

        instagramLayout = findViewById(R.id.instagramLayout);
        instagramInput = findViewById(R.id.instagramInput);

        hourlyRateLayout = findViewById(R.id.hourlyRateLayout);
        hourlyRateInput = findViewById(R.id.hourlyRateInput);

        sessionPriceLayout = findViewById(R.id.sessionPriceLayout);
        sessionPriceInput = findViewById(R.id.sessionPriceInput);

        submitButton = findViewById(R.id.submitButton);
        submitButtonText = findViewById(R.id.submitButtonText);
        termsText = findViewById(R.id.termsText);

        heartImage1 = findViewById(R.id.heartImage1);
        heartImage2 = findViewById(R.id.heartImage2);
        heartImage3 = findViewById(R.id.heartImage3);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void setupAnimations() {
        // Floating hearts animation
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
        submitButton.setOnClickListener(v -> {
            Animation bounce = AnimationUtils.loadAnimation(LoveDoctorRequestActivity.this, R.anim.bounce);
            submitButton.startAnimation(bounce);
            validateAndSubmit();
        });

        termsText.setOnClickListener(v -> showTermsDialog());
    }

    private void setupQualificationTypeSelector() {
        qualificationTypeInput.setOnClickListener(v -> showQualificationTypeDialog());
        qualificationTypeInput.setFocusable(false);
        qualificationTypeInput.setClickable(true);
        qualificationTypeInput.setKeyListener(null);
    }

    private void showQualificationTypeDialog() {
        String[] types = {
                "Psychologist - Licensed mental health professional",
                "Motivator - Inspires and encourages positive change",
                "Influencer - Social media personality with relationship expertise",
                "Pastor - Religious/spiritual relationship guidance",
                "Counsellor - Professional relationship counsellor",
                "Other - Other relevant qualification"
        };

        final String[] typeValues = {
                "psychologist",
                "motivator",
                "influencer",
                "pastor",
                "counsellor",
                "other"
        };

        new AlertDialog.Builder(this)
                .setTitle("Select Your Qualification Type")
                .setItems(types, (dialog, which) -> {
                    qualificationTypeInput.setText(types[which].split(" - ")[0]);
                    qualificationTypeInput.setTag(typeValues[which]);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showTermsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("📋 Terms & Guidelines for Love Doctors")
                .setMessage(
                        "💕 **Love Doctor Guidelines**\n\n" +
                                "1. **Professional Conduct**\n" +
                                "   - Always maintain confidentiality\n" +
                                "   - Be respectful and non-judgmental\n" +
                                "   - Never share personal contact information\n\n" +

                                "2. **Qualification Requirements**\n" +
                                "   - You must be at least 21 years old\n" +
                                "   - Provide proof of qualifications via email\n" +
                                "   - Background check may be conducted\n\n" +

                                "3. **Service Standards**\n" +
                                "   - Respond to messages within 24 hours\n" +
                                "   - Provide genuine, helpful advice\n" +
                                "   - Report any concerning behaviour\n\n" +

                                "4. **Prohibited Actions**\n" +
                                "   - No solicitation of personal meetings\n" +
                                "   - No romantic involvement with patients\n" +
                                "   - No sharing of session content\n" +
                                "   - No screenshotting or recording chats\n\n" +

                                "5. **Consequences of Violation**\n" +
                                "   - Warning for first offense\n" +
                                "   - Temporary suspension for second offense\n" +
                                "   - Permanent ban and account termination for serious violations\n\n" +

                                "By submitting this application, you agree to follow all guidelines and understand that Soulmate reserves the right to approve or reject applications at our discretion.\n\n" +

                                "💗 **Thank you for helping others find love and peace!**"
                )
                .setPositiveButton("I Understand", null)
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void validateAndSubmit() {
        String summary = summaryInput.getText().toString().trim();
        String qualifications = qualificationsInput.getText().toString().trim();
        String qualificationType = qualificationTypeInput.getText().toString().trim();
        String facebookUrl = facebookInput.getText().toString().trim();
        String youtubeUrl = youtubeInput.getText().toString().trim();
        String instagramUrl = instagramInput.getText().toString().trim();
        String hourlyRateStr = hourlyRateInput.getText().toString().trim();
        String sessionPriceStr = sessionPriceInput.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(summary)) {
            summaryLayout.setError("Please tell us why you want to be a Love Doctor");
            summaryInput.requestFocus();
            return;
        } else {
            summaryLayout.setError(null);
        }

        if (TextUtils.isEmpty(qualifications)) {
            qualificationsLayout.setError("Please list your qualifications");
            qualificationsInput.requestFocus();
            return;
        } else {
            qualificationsLayout.setError(null);
        }

        if (TextUtils.isEmpty(qualificationType)) {
            qualificationTypeLayout.setError("Please select your qualification type");
            return;
        } else {
            qualificationTypeLayout.setError(null);
        }

        if (TextUtils.isEmpty(hourlyRateStr)) {
            hourlyRateLayout.setError("Please enter your hourly rate");
            hourlyRateInput.requestFocus();
            return;
        } else {
            hourlyRateLayout.setError(null);
        }

        if (TextUtils.isEmpty(sessionPriceStr)) {
            sessionPriceLayout.setError("Please enter your session price");
            sessionPriceInput.requestFocus();
            return;
        } else {
            sessionPriceLayout.setError(null);
        }

        float hourlyRate;
        float sessionPrice;

        try {
            hourlyRate = Float.parseFloat(hourlyRateStr);
            if (hourlyRate < 50) {
                hourlyRateLayout.setError("Minimum hourly rate is E50");
                hourlyRateInput.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            hourlyRateLayout.setError("Please enter a valid number");
            hourlyRateInput.requestFocus();
            return;
        }

        try {
            sessionPrice = Float.parseFloat(sessionPriceStr);
            if (sessionPrice < 50) {
                sessionPriceLayout.setError("Minimum session price is E50");
                sessionPriceInput.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            sessionPriceLayout.setError("Please enter a valid number");
            sessionPriceInput.requestFocus();
            return;
        }

        // Validate social media links (optional but format check)
        if (!TextUtils.isEmpty(facebookUrl) && !isValidUrl(facebookUrl)) {
            facebookLayout.setError("Please enter a valid URL");
            facebookInput.requestFocus();
            return;
        } else {
            facebookLayout.setError(null);
        }

        if (!TextUtils.isEmpty(youtubeUrl) && !isValidUrl(youtubeUrl)) {
            youtubeLayout.setError("Please enter a valid URL");
            youtubeInput.requestFocus();
            return;
        } else {
            youtubeLayout.setError(null);
        }

        if (!TextUtils.isEmpty(instagramUrl) && !isValidUrl(instagramUrl)) {
            instagramLayout.setError("Please enter a valid URL");
            instagramInput.requestFocus();
            return;
        } else {
            instagramLayout.setError(null);
        }

        // Show confirmation dialog
        showConfirmationDialog(
                summary, qualifications, qualificationType,
                facebookUrl, youtubeUrl, instagramUrl,
                hourlyRate, sessionPrice
        );
    }

    private boolean isValidUrl(String url) {
        String urlPattern = "^(http|https)://.*$";
        return url.matches(urlPattern);
    }

    private void showConfirmationDialog(String summary, String qualifications, String qualificationType,
                                        String facebookUrl, String youtubeUrl, String instagramUrl,
                                        float hourlyRate, float sessionPrice) {

        String message = "📝 **Application Summary**\n\n" +
                "**Why you want to be a Love Doctor:**\n" + summary + "\n\n" +
                "**Qualifications:**\n" + qualifications + "\n\n" +
                "**Qualification Type:**\n" + qualificationType + "\n\n" +
                "**Hourly Rate:** E" + hourlyRate + "\n" +
                "**Session Price:** E" + sessionPrice + "\n\n" +
                "**Social Links:**\n" +
                (TextUtils.isEmpty(facebookUrl) ? "• Facebook: Not provided\n" : "• Facebook: " + facebookUrl + "\n") +
                (TextUtils.isEmpty(youtubeUrl) ? "• YouTube: Not provided\n" : "• YouTube: " + youtubeUrl + "\n") +
                (TextUtils.isEmpty(instagramUrl) ? "• Instagram: Not provided\n" : "• Instagram: " + instagramUrl + "\n") +
                "\n⚠️ **Please note:** You will be required to email your supporting documents (certificates, credentials) to verify your qualifications.";

        new AlertDialog.Builder(this)
                .setTitle("💕 Submit Application?")
                .setMessage(message)
                .setPositiveButton("Submit Application", (dialog, which) -> {
                    submitApplication(
                            summary, qualifications, qualificationType,
                            facebookUrl, youtubeUrl, instagramUrl,
                            hourlyRate, sessionPrice
                    );
                })
                .setNegativeButton("Edit", null)
                .show();
    }

    private void submitApplication(String summary, String qualifications, String qualificationType,
                                   String facebookUrl, String youtubeUrl, String instagramUrl,
                                   float hourlyRate, float sessionPrice) {

        showProgress(true);

        OkHttpClient client = new OkHttpClient();

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("action", "request_to_be_doctor")
                .addFormDataPart("token", authToken)
                .addFormDataPart("summary", summary)
                .addFormDataPart("qualifications", qualifications)
                .addFormDataPart("qualification_type", qualificationType)
                .addFormDataPart("hourly_rate", String.valueOf(hourlyRate))
                .addFormDataPart("session_price", String.valueOf(sessionPrice));

        if (!TextUtils.isEmpty(facebookUrl)) {
            builder.addFormDataPart("facebook_url", facebookUrl);
        }
        if (!TextUtils.isEmpty(youtubeUrl)) {
            builder.addFormDataPart("youtube_url", youtubeUrl);
        }
        if (!TextUtils.isEmpty(instagramUrl)) {
            builder.addFormDataPart("instagram_url", instagramUrl);
        }

        RequestBody body = builder.build();

        Request request = new Request.Builder()
                .url(BASE_URL + "dr.php")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(LoveDoctorRequestActivity.this,
                            "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
                            showSuccessDialog(json.getString("message"));
                        } else {
                            Toast.makeText(LoveDoctorRequestActivity.this,
                                    json.getString("message"), Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(LoveDoctorRequestActivity.this,
                                "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void showSuccessDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("✅ Application Submitted!")
                .setMessage(message + "\n\n📧 Please send your supporting documents to:\n**support@soulmate.com**\n\nWe will review your application within 3-5 business days and notify you via email.")
                .setPositiveButton("Great! Thank you", (dialog, which) -> {
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        scrollView.setVisibility(show ? View.GONE : View.VISIBLE);
        formContainer.setEnabled(!show);
        submitButton.setEnabled(!show);
        if (show) {
            if (submitButtonText != null) submitButtonText.setText("Submitting...");
        } else {
            if (submitButtonText != null) submitButtonText.setText("💕 Submit Application");
        }
    }
}