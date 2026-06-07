package com.mabona.mobilesystems.soulmate;

import android.Manifest;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.util.Log;
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
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RequestLoveActivity extends AppCompatActivity {

    // UI Components
    private ImageView backButton, profileImageView, onlineIndicator, viewProfileTextLink;
    private TextView nameText, ageGenderText, locationText, bioText, statusText;
    private CardView loveRequestCard, profileRequestCard, chatCard;
    private TextView loveRequestText, profileRequestText, chatText;
    private ProgressBar progressBar;
    private AdView bannerAdView;
    private ImageView heartImage1, heartImage2, heartImage3;

    // Data - ALL from Intent
    private int myUserId;
    private String authToken;
    private String myUserName;
    private String myUserEmail;
    private int targetUserId;
    private String targetUserName;
    private String targetUserImagePath;
    private String targetUserGender;
    private int targetUserAge;
    private String targetUserBio;
    private String targetUserLocation;
    private boolean targetUserOnline;
    private String profileType;

    // For incoming requests
    private int incomingRequestId;
    private String incomingRequestType;
    private String mode; // "incoming" or "new"

    // Constants
    private static final String BASE_URL = "https://mabona.firstsuninvestment.com/soulmate/";
    private static final int REQUEST_CODE_SAVE_IMAGE = 200;
    private static final String TAG = "RequestLoveActivity";

    // MediaPlayer for sounds
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_request_love);

            // ========== GET ALL DATA FROM INTENT ==========
            getIntentData();

            // Validation
            if (myUserId == -1 || targetUserId == -1) {
                showPinkToast("Invalid session. Please login again.");
                finish();
                return;
            }

            initializeViews();
            setupAnimations();
            loadUserData();
            setupClickListeners();
            loadAd();
            setupImageZoom();
            setupImageSave();

        } catch (Exception e) {
            Log.e(TAG, "onCreate error: " + e.getMessage(), e);
            showPinkToast("Error: " + e.getMessage());
            finish();
        }

    }

    private void getIntentData() {
        // Get MY user data
        myUserId = getIntent().getIntExtra("MY_USER_ID", -1);
        authToken = getIntent().getStringExtra("MY_AUTH_TOKEN");
        myUserName = getIntent().getStringExtra("MY_USER_NAME");
        myUserEmail = getIntent().getStringExtra("MY_USER_EMAIL");

        // Get TARGET user data
        targetUserId = getIntent().getIntExtra("TARGET_USER_ID", -1);
        targetUserName = getIntent().getStringExtra("TARGET_USER_NAME");
        targetUserImagePath = getIntent().getStringExtra("TARGET_USER_IMAGE_PATH");
        targetUserGender = getIntent().getStringExtra("TARGET_USER_GENDER");
        targetUserAge = getIntent().getIntExtra("TARGET_USER_AGE", 0);
        targetUserBio = getIntent().getStringExtra("TARGET_USER_BIO");
        targetUserLocation = getIntent().getStringExtra("TARGET_USER_LOCATION");
        targetUserOnline = getIntent().getBooleanExtra("TARGET_USER_ONLINE", false);
        profileType = getIntent().getStringExtra("PROFILE_TYPE");

        // Get incoming request data (if coming from RequestsActivity)
        incomingRequestId = getIntent().getIntExtra("REQUEST_ID", -1);
        incomingRequestType = getIntent().getStringExtra("REQUEST_TYPE");
        mode = getIntent().getStringExtra("MODE"); // "incoming" or "new"

        Log.d(TAG, "Image path received: " + targetUserImagePath);
        Log.d(TAG, "My User ID: " + myUserId);
        Log.d(TAG, "Target User ID: " + targetUserId);
        Log.d(TAG, "Mode: " + mode);
        Log.d(TAG, "Request ID: " + incomingRequestId);
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        profileImageView = findViewById(R.id.profileImageView);
        onlineIndicator = findViewById(R.id.onlineIndicator);
        nameText = findViewById(R.id.nameText);
        ageGenderText = findViewById(R.id.ageGenderText);
        locationText = findViewById(R.id.locationText);
        bioText = findViewById(R.id.bioText);
        statusText = findViewById(R.id.statusText);
        progressBar = findViewById(R.id.progressBar);
        bannerAdView = findViewById(R.id.bannerAdView);
        heartImage1 = findViewById(R.id.heartImage1);
        heartImage2 = findViewById(R.id.heartImage2);
        heartImage3 = findViewById(R.id.heartImage3);


        loveRequestCard = findViewById(R.id.loveRequestCard);
        profileRequestCard = findViewById(R.id.profileRequestCard);
        chatCard = findViewById(R.id.chatCard);
        loveRequestText = findViewById(R.id.loveRequestText);
        profileRequestText = findViewById(R.id.profileRequestText);
        chatText = findViewById(R.id.chatText);
        viewProfileTextLink=findViewById(R.id.viewProfileTextLink);
        viewProfileTextLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                Intent intent = new Intent(RequestLoveActivity.this, ProfileViewActivity.class);
                intent.putExtra("USER_ID", myUserId);
                intent.putExtra("AUTH_TOKEN", authToken);
                intent.putExtra("USER_EMAIL", myUserEmail);
                intent.putExtra("USER_NAME", myUserName);
                intent.putExtra("PROFILE_USER_ID", targetUserId);
                intent.putExtra("PROFILE_USER_NAME", targetUserName);
                startActivity(intent);
                Animation bounce = AnimationUtils.loadAnimation(RequestLoveActivity.this, R.anim.bounce);
                viewProfileTextLink.startAnimation(bounce);
                playNotificationSound("view");
                // vibrate();
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

            }
        });

        // Back button
        backButton.setOnClickListener(v -> {
            Animation bounce = AnimationUtils.loadAnimation(RequestLoveActivity.this, R.anim.bounce);
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

    private void loadUserData() {
        nameText.setText(targetUserName);

        String ageGender = targetUserAge + " yrs • " +
                targetUserGender.substring(0, 1).toUpperCase() +
                targetUserGender.substring(1);
        ageGenderText.setText(ageGender);

        if (targetUserLocation != null && !targetUserLocation.isEmpty()) {
            locationText.setText("📍 " + targetUserLocation);
            locationText.setVisibility(View.VISIBLE);
        } else {
            locationText.setVisibility(View.GONE);
        }

        if (targetUserBio != null && !targetUserBio.isEmpty()) {
            bioText.setText(targetUserBio);
        } else {
            bioText.setText("No bio yet. Say hello!");
        }

        // Show private profile indicator
        if ("private".equals(profileType)) {
            showPinkToast("This profile is private");
        }

        // Online indicator
        if (targetUserOnline) {
            onlineIndicator.setVisibility(View.VISIBLE);
        } else {
            onlineIndicator.setVisibility(View.GONE);
        }

        // Load profile image
        loadProfileImage();
    }

    private void loadProfileImage() {
        profileImageView.setImageResource(R.drawable.default_profile);

        if (targetUserImagePath == null || targetUserImagePath.isEmpty()) {
            Log.d(TAG, "No image path provided");
            return;
        }

        String imageUrl = BASE_URL + "get_image.php?path=" + Uri.encode(targetUserImagePath) + "&token=" + authToken;

        Log.d(TAG, "Loading image from URL: " + imageUrl);

        try {
            Glide.with(this)
                    .load(imageUrl)
                    .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .timeout(15000)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(profileImageView);
        } catch (Exception e) {
            Log.e(TAG, "Error loading image: " + e.getMessage());
            profileImageView.setImageResource(R.drawable.default_profile);
        }
    }

    private void setupClickListeners() {
        boolean isIncoming = "incoming".equals(mode);
        boolean isPrivate = "private".equals(profileType);

        if (isIncoming) {
            // ========== INCOMING REQUEST MODE - Show Accept/Decline ==========
            loveRequestText.setText("Accept");
            profileRequestText.setText("Decline");
            loveRequestCard.setCardBackgroundColor(getColor(R.color.green));
            profileRequestCard.setCardBackgroundColor(getColor(R.color.red));

            loveRequestCard.setOnClickListener(v -> {
                Animation bounce = AnimationUtils.loadAnimation(RequestLoveActivity.this, R.anim.bounce);
                loveRequestCard.startAnimation(bounce);
                playNotificationSound("accept");
                // vibrate();
                respondToRequest("accept");
            });

            profileRequestCard.setOnClickListener(v -> {
                Animation bounce = AnimationUtils.loadAnimation(RequestLoveActivity.this, R.anim.bounce);
                profileRequestCard.startAnimation(bounce);
                playNotificationSound("decline");
                // vibrate();
                respondToRequest("decline");
            });

        } else {
            // ========== NORMAL MODE - Send Requests ==========

            // Love Request Button
            loveRequestCard.setOnClickListener(v -> {
                Animation bounce = AnimationUtils.loadAnimation(RequestLoveActivity.this, R.anim.bounce);
                loveRequestCard.startAnimation(bounce);
                playNotificationSound("love");
                // vibrate();
                sendRequest("love");
            });

            // Profile View Request Button
            if (isPrivate) {
                profileRequestText.setText("Request View");
                profileRequestCard.setOnClickListener(v -> {
                    Animation bounce = AnimationUtils.loadAnimation(RequestLoveActivity.this, R.anim.bounce);
                    profileRequestCard.startAnimation(bounce);
                    playNotificationSound("request");
                    // vibrate();
                    sendRequest("profile_view");
                });
            } else {
                profileRequestText.setText("View Profile");
                profileRequestCard.setOnClickListener(v -> {
                    Animation bounce = AnimationUtils.loadAnimation(RequestLoveActivity.this, R.anim.bounce);
                    profileRequestCard.startAnimation(bounce);
                    playNotificationSound("request");
                    // vibrate();
                    showPinkToast("Profile is already public!");
                });
            }
        }

        // ========== CHAT BUTTON - Always works ==========
        chatCard.setOnClickListener(v -> {
            Animation bounce = AnimationUtils.loadAnimation(RequestLoveActivity.this, R.anim.bounce);
            chatCard.startAnimation(bounce);
            playNotificationSound("chat");
            // vibrate();

            Intent intent = new Intent(RequestLoveActivity.this, ChatActivity.class);
            intent.putExtra("USER_ID", myUserId);
            intent.putExtra("AUTH_TOKEN", authToken);
            intent.putExtra("USER_EMAIL", myUserEmail);
            intent.putExtra("USER_NAME", myUserName);
            intent.putExtra("OTHER_USER_ID", targetUserId);
            intent.putExtra("OTHER_USER_NAME", targetUserName);
            intent.putExtra("OTHER_USER_PROFILE_IMAGE", targetUserImagePath);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    private void respondToRequest(String action) {
        showProgress(true);

        OkHttpClient client = new OkHttpClient();

        RequestBody formBody = new FormBody.Builder()
                .add("user_id", String.valueOf(myUserId))
                .add("token", authToken)
                .add("request_id", String.valueOf(incomingRequestId))
                .add("action", action)
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + "loveactions.php")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    showPinkToast("Network error. Please try again.");
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
                        String message = jsonResponse.getString("message");
                        showPinkToast(message);
                        if (success) {
                            finish();
                        }
                    } catch (JSONException e) {
                        showPinkToast("Error processing request");
                    }
                });
            }
        });
    }

    private void sendRequest(String requestType) {
        showProgress(true);

        OkHttpClient client = new OkHttpClient();

        RequestBody formBody = new FormBody.Builder()
                .add("user_id", String.valueOf(myUserId))
                .add("target_id", String.valueOf(targetUserId))
                .add("token", authToken)
                .add("type", requestType)
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + "request.php")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    showPinkToast("Network error. Please try again.");
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
                        String message = jsonResponse.getString("message");

                        showPinkToast(message);

                        if (success) {
                            loveRequestCard.setEnabled(false);
                            profileRequestCard.setEnabled(false);
                            loveRequestCard.setAlpha(0.5f);
                            profileRequestCard.setAlpha(0.5f);

                            statusText.setText("Request sent! Check Replies tab for status.");
                            statusText.setVisibility(View.VISIBLE);

                            new Handler().postDelayed(() -> {
                                statusText.setVisibility(View.GONE);
                            }, 5000);
                        }
                    } catch (JSONException e) {
                        showPinkToast("Error sending request");
                    }
                });
            }
        });
    }

    private void playNotificationSound(String type) {
        try {
            int soundResId = 0;
            switch (type) {
                case "love":
                    soundResId = getResources().getIdentifier("notification_love", "raw", getPackageName());
                    break;
                case "request":
                    soundResId = getResources().getIdentifier("notification_request", "raw", getPackageName());
                    break;
                case "chat":
                    soundResId = getResources().getIdentifier("notification_chat", "raw", getPackageName());
                    break;
                case "accept":
                    soundResId = getResources().getIdentifier("notification_accept", "raw", getPackageName());
                    break;
                case "decline":
                    soundResId = getResources().getIdentifier("notification_decline", "raw", getPackageName());
                    break;
            }

            if (soundResId != 0) {
                if (mediaPlayer != null) {
                    mediaPlayer.release();
                }
                mediaPlayer = MediaPlayer.create(this, soundResId);
                if (mediaPlayer != null) {
                    mediaPlayer.start();
                    mediaPlayer.setOnCompletionListener(mp -> mp.release());
                }
            } else {
                android.media.RingtoneManager.getRingtone(this,
                                android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
                        .play();
            }
        } catch (Exception e) {
            Log.e(TAG, "Sound error: " + e.getMessage());
        }
    }

    private void vibrate() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(100);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Vibration error: " + e.getMessage());
        }
    }

    private void setupImageZoom() {
        profileImageView.setOnClickListener(v -> {
            if (targetUserImagePath == null || targetUserImagePath.isEmpty()) {
                showPinkToast("No image to zoom");
                return;
            }

            try {
                Dialog zoomDialog = new Dialog(RequestLoveActivity.this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                zoomDialog.setContentView(R.layout.dialog_image_zoom);

                ImageView zoomImage = zoomDialog.findViewById(R.id.zoomImageView);
                ImageView saveButton = zoomDialog.findViewById(R.id.saveImageView);

                String imageUrl = BASE_URL + "get_image.php?path=" + Uri.encode(targetUserImagePath) + "&token=" + authToken;

                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .timeout(15000)
                        .into(zoomImage);

                if (saveButton != null) {
                    saveButton.setOnClickListener(v1 -> {
                        saveImageToGalleryFromUrl(imageUrl);
                    });
                }

                zoomImage.setOnClickListener(v1 -> zoomDialog.dismiss());
                zoomDialog.show();
            } catch (Exception e) {
                Log.e(TAG, "Zoom error: " + e.getMessage());
                showPinkToast("Cannot load image");
            }
        });
    }

    private void saveImageToGalleryFromUrl(String imageUrl) {
        showPinkToast("Saving image...");

        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(imageUrl);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                java.io.InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                input.close();
                connection.disconnect();

                if (bitmap != null) {
                    runOnUiThread(() -> saveBitmapToGallery(bitmap));
                } else {
                    runOnUiThread(() -> showPinkToast("Failed to save image"));
                }
            } catch (Exception e) {
                runOnUiThread(() -> showPinkToast("Failed to save image: " + e.getMessage()));
            }
        }).start();
    }

    private void saveBitmapToGallery(Bitmap bitmap) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "Soulmate_Profile_" + System.currentTimeMillis() + ".jpg");
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Soulmate");

                Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                if (uri != null) {
                    OutputStream outputStream = resolver.openOutputStream(uri);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
                    if (outputStream != null) {
                        outputStream.close();
                    }
                    showPinkToast("Image saved to Gallery");
                }
            } else {
                File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File soulmateDir = new File(picturesDir, "Soulmate");
                if (!soulmateDir.exists()) {
                    soulmateDir.mkdirs();
                }

                File imageFile = new File(soulmateDir, "Soulmate_Profile_" + System.currentTimeMillis() + ".jpg");
                FileOutputStream fos = new FileOutputStream(imageFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                fos.close();

                showPinkToast("Image saved to Gallery");

                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(Uri.fromFile(imageFile));
                sendBroadcast(mediaScanIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Save error: " + e.getMessage());
            showPinkToast("Failed to save image");
        }
    }

    private void setupImageSave() {
        profileImageView.setOnLongClickListener(v -> {
            checkAndSaveImage();
            return true;
        });
    }

    private void checkAndSaveImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_SAVE_IMAGE);
                return;
            }
            saveImageToGallery();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveImageToGallery();
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_SAVE_IMAGE);
                return;
            }
            saveImageToGallery();
        }
    }

    private void saveImageToGallery() {
        try {
            Bitmap bitmap = ((BitmapDrawable) profileImageView.getDrawable()).getBitmap();
            if (bitmap == null) {
                showPinkToast("No image to save");
                return;
            }
            saveBitmapToGallery(bitmap);
        } catch (Exception e) {
            Log.e(TAG, "Save image error: " + e.getMessage());
            showPinkToast("Failed to save image");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_SAVE_IMAGE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            saveImageToGallery();
        } else {
            showPinkToast("Permission denied to save image");
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

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        loveRequestCard.setEnabled(!show);
        profileRequestCard.setEnabled(!show);
        chatCard.setEnabled(!show);
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
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bannerAdView != null) {
            bannerAdView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bannerAdView != null) {
            bannerAdView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bannerAdView != null) {
            bannerAdView.destroy();
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }
}