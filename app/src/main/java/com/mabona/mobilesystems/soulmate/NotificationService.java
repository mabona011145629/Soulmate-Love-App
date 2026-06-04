package com.mabona.mobilesystems.soulmate;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NotificationService extends Service {

    private static final String CHANNEL_ID = "soulmate_notifications";
    private static final String PREFS_NAME = "SoulmatePrefs";
    private static final String KEY_LAST_NOTIFICATION_CHECK = "last_notification_check";
    private static final String BASE_URL = "https://mabona.firstsuninvestment.com/soulmate/";
    private static final int NOTIFICATION_ID = 9999;

    private Handler handler;
    private Runnable notificationRunnable;
    private SharedPreferences sharedPreferences;
    private int userId;
    private String authToken;
    private MediaPlayer mediaPlayer;

    private static final long CHECK_INTERVAL = 60000; // 60 seconds
    private static final String TAG = "NotificationService";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            createNotificationChannel();
            sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            handler = new Handler(Looper.getMainLooper());
            Log.d(TAG, "NotificationService created successfully");
        } catch (Exception e) {
            Log.e(TAG, "onCreate error: " + e.getMessage(), e);
        }
    }

    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand called");

        try {
            if (intent != null) {
                userId = intent.getIntExtra("USER_ID", -1);
                authToken = intent.getStringExtra("AUTH_TOKEN");

                Log.d(TAG, "Received userId: " + userId);

                if (userId != -1 && authToken != null && !authToken.isEmpty()) {
                    // IMPORTANT: Call startForeground with type for Android 14+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(NOTIFICATION_ID, createForegroundNotification(),
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
                    } else {
                        startForeground(NOTIFICATION_ID, createForegroundNotification());
                    }

                    // Small delay to let app stabilize
                    handler.postDelayed(() -> startNotificationLoop(), 5000);
                } else {
                    Log.e(TAG, "Invalid userId or authToken - stopping service");
                    stopSelf();
                }
            } else {
                Log.e(TAG, "Intent is null - stopping service");
                stopSelf();
            }
        } catch (Exception e) {
            Log.e(TAG, "onStartCommand error: " + e.getMessage(), e);
            stopSelf();
        }
        return START_STICKY;
    }

    private Notification createForegroundNotification() {
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Soulmate")
                .setContentText("Checking for love requests...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void startNotificationLoop() {
        Log.d(TAG, "Starting notification loop with interval: " + CHECK_INTERVAL + "ms");

        notificationRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    checkForNotifications();
                } catch (Exception e) {
                    Log.e(TAG, "Notification loop error: " + e.getMessage(), e);
                }
                handler.postDelayed(this, CHECK_INTERVAL);
            }
        };
        handler.post(notificationRunnable);
    }

    private void checkForNotifications() {
        Log.d(TAG, "Checking for notifications for user: " + userId);

        try {
            String url = BASE_URL + "notifications.php?user_id=" + userId + "&token=" + authToken;
            Log.d(TAG, "URL: " + url);

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Failed to check notifications: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final String responseBody = response.body().string();
                    Log.d(TAG, "Response received");

                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        boolean success = jsonResponse.optBoolean("success", false);

                        if (success) {
                            JSONArray notifications = jsonResponse.optJSONArray("notifications");
                            if (notifications != null && notifications.length() > 0) {
                                int maxToShow = Math.min(notifications.length(), 3);
                                Log.d(TAG, "Processing " + maxToShow + " notifications");

                                for (int i = 0; i < maxToShow; i++) {
                                    try {
                                        JSONObject notif = notifications.getJSONObject(i);
                                        showNotification(notif);
                                        if (i == 0) {
                                            playNotificationSound(notif.optString("type", ""));
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error processing notification: " + e.getMessage());
                                    }
                                }
                            } else {
                                Log.d(TAG, "No new notifications");
                            }

                            // Update last check time
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString(KEY_LAST_NOTIFICATION_CHECK,
                                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
                            editor.apply();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "checkForNotifications error: " + e.getMessage(), e);
        }
    }

    private void showNotification(JSONObject notification) {
        try {
            String type = notification.optString("type", "system");
            String title = notification.optString("title", "Soulmate");
            String message = notification.optString("message", "You have a new notification");
            String fromName = notification.optString("from_name", "Someone");
            int relatedId = notification.optInt("related_id", 0);
            int fromUserId = notification.optInt("from_user_id", 0);
            int notificationId = notification.optInt("notification_id", (int) System.currentTimeMillis());

            Log.d(TAG, "Building notification ID: " + notificationId + ", Type: " + type);

            Intent intent;

            switch (type) {
                case "love_request":
                case "profile_request":
                    intent = new Intent(this, RequestsActivity.class);
                    break;
                case "request_accepted":
                case "request_declined":
                    intent = new Intent(this, RepliesActivity.class);
                    break;
                case "new_message":
                    intent = new Intent(this, DashboardActivity.class);
                    break;
                case "post_like":
                case "post_comment":
                    intent = new Intent(this, PostsActivity.class);
                    break;
                default:
                    intent = new Intent(this, DashboardActivity.class);
            }

            intent.putExtra("USER_ID", userId);
            intent.putExtra("AUTH_TOKEN", authToken);
            intent.putExtra("FROM_USER_ID", fromUserId);
            intent.putExtra("RELATED_ID", relatedId);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(this, notificationId,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            int iconId = getResources().getIdentifier("ic_heart_pink", "drawable", getPackageName());
            if (iconId == 0) {
                iconId = android.R.drawable.ic_dialog_info;
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(iconId)
                    .setContentTitle(title)
                    .setContentText(fromName + ": " + message)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(fromName + ": " + message))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(notificationId, builder.build());
                Log.d(TAG, "Notification shown successfully: " + notificationId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing notification: " + e.getMessage(), e);
        }
    }

    private void playNotificationSound(String type) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            return;
        }

        try {
            int soundResId = 0;
            if (type != null) {
                switch (type) {
                    case "love_request":
                        soundResId = getResources().getIdentifier("notification_love", "raw", getPackageName());
                        break;
                    case "profile_request":
                    case "request_accepted":
                    case "request_declined":
                        soundResId = getResources().getIdentifier("notification_request", "raw", getPackageName());
                        break;
                    case "new_message":
                        soundResId = getResources().getIdentifier("notification_chat", "raw", getPackageName());
                        break;
                    default:
                        soundResId = getResources().getIdentifier("notification_reply", "raw", getPackageName());
                }
            }

            if (soundResId != 0) {
                if (mediaPlayer != null) {
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
                mediaPlayer = MediaPlayer.create(this, soundResId);
                if (mediaPlayer != null) {
                    mediaPlayer.start();
                    mediaPlayer.setOnCompletionListener(mp -> {
                        mp.release();
                        mediaPlayer = null;
                    });
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing sound: " + e.getMessage());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Soulmate Notifications",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Love requests, messages, and updates");
                channel.enableVibration(true);
                channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                    Log.d(TAG, "Notification channel created");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating notification channel: " + e.getMessage());
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "NotificationService destroyed");

        if (handler != null && notificationRunnable != null) {
            handler.removeCallbacks(notificationRunnable);
        }
        if (mediaPlayer != null) {
            try {
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing media player: " + e.getMessage());
            }
            mediaPlayer = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}