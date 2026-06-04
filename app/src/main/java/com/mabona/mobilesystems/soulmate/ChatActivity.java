package com.mabona.mobilesystems.soulmate;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final int PICK_IMAGE_REQUEST = 100;
    private static final int PICK_VIDEO_REQUEST = 101;
    private static final int PICK_AUDIO_REQUEST = 104;

    // UI Components
    private ImageView backButton;
    private TextView otherUserNameText;
    private TextView onlineStatusText;
    private RecyclerView messagesRecyclerView;
    private EditText messageInput;
    private ImageView sendButton;
    private ImageView attachButton;
    private ImageView voiceButton;
    private LinearLayout attachmentPanel;
    private CardView attachImageBtn, attachVideoBtn, attachMusicBtn;
    private CardView messageInputContainer;
    private ImageView voiceCallIcon, videoCallIcon;
    private ImageView heartImage1, heartImage2, heartImage3;

    // Voice Recording
    private LinearLayout voiceRecordingPanel;
    private TextView voiceRecordingTimer;
    private ImageView cancelVoiceBtn, sendVoiceBtn;
    private MediaRecorder mediaRecorder;
    private String voiceFilePath;
    private int voiceDuration = 0;
    private Handler voiceTimerHandler = new Handler();
    private boolean isRecording = false;

    // Data
    private int userId;
    private String authToken;
    private int otherUserId;
    private String otherUserName;
    private String otherUserProfileImage;

    private MessageAdapter messagesAdapter;
    private List<Message> messagesList = new ArrayList<>();
    private OkHttpClient client;
    private Handler handler = new Handler();
    private boolean isLoading = false;
    private boolean hasMore = true;
    private int currentOffset = 0;
    private static final int PAGE_SIZE = 30;

    // Sound Player
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;

    private static final String BASE_URL = "https://mabona.firstsuninvestment.com/soulmate/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        userId = getIntent().getIntExtra("USER_ID", -1);
        authToken = getIntent().getStringExtra("AUTH_TOKEN");
        otherUserId = getIntent().getIntExtra("OTHER_USER_ID", -1);
        otherUserName = getIntent().getStringExtra("OTHER_USER_NAME");
        otherUserProfileImage = getIntent().getStringExtra("OTHER_USER_PROFILE_IMAGE");

        if (userId == -1 || authToken == null || otherUserId == -1) {
            Toast.makeText(this, "Invalid chat session", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupRecyclerView();
        setupMessageInput();
        setupVoiceRecording();
        setupCallButtons();
        animateHearts();

        loadMessages();
        markMessagesAsRead();
        startPolling();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        otherUserNameText = findViewById(R.id.otherUserNameText);
        onlineStatusText = findViewById(R.id.onlineStatusText);
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        attachButton = findViewById(R.id.attachButton);
        voiceButton = findViewById(R.id.voiceButton);
        messageInputContainer = findViewById(R.id.messageInputContainer);
        attachmentPanel = findViewById(R.id.attachmentPanel);
        attachImageBtn = findViewById(R.id.attachImageBtn);
        attachVideoBtn = findViewById(R.id.attachVideoBtn);
        attachMusicBtn = findViewById(R.id.attachMusicBtn);
        voiceCallIcon = findViewById(R.id.voiceCallIcon);
        videoCallIcon = findViewById(R.id.videoCallIcon);
        heartImage1 = findViewById(R.id.heartImage1);
        heartImage2 = findViewById(R.id.heartImage2);
        heartImage3 = findViewById(R.id.heartImage3);
        voiceRecordingPanel = findViewById(R.id.voiceRecordingPanel);
        voiceRecordingTimer = findViewById(R.id.voiceRecordingTimer);
        cancelVoiceBtn = findViewById(R.id.cancelVoiceBtn);
        sendVoiceBtn = findViewById(R.id.sendVoiceBtn);

        otherUserNameText.setText(otherUserName != null ? otherUserName : "User");
        onlineStatusText.setText("Online");

        backButton.setOnClickListener(v -> finish());

        attachButton.setOnClickListener(v -> {
            attachmentPanel.setVisibility(attachmentPanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });

        attachImageBtn.setOnClickListener(v -> {
            attachmentPanel.setVisibility(View.GONE);
            openImagePicker();
        });

        attachVideoBtn.setOnClickListener(v -> {
            attachmentPanel.setVisibility(View.GONE);
            openVideoPicker();
        });

        attachMusicBtn.setOnClickListener(v -> {
            attachmentPanel.setVisibility(View.GONE);
            openAudioPicker();
        });

        voiceButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
                Toast.makeText(this, "Microphone permission needed to record voice messages", Toast.LENGTH_LONG).show();
                return;
            }
            messageInputContainer.setVisibility(View.GONE);
            voiceRecordingPanel.setVisibility(View.VISIBLE);
        });

        cancelVoiceBtn.setOnClickListener(v -> {
            messageInputContainer.setVisibility(View.VISIBLE);
            voiceRecordingPanel.setVisibility(View.GONE);
            if (mediaRecorder != null) {
                try {
                    mediaRecorder.stop();
                    mediaRecorder.release();
                    mediaRecorder = null;
                } catch (Exception e) {}
            }
            isRecording = false;
            voiceTimerHandler.removeCallbacksAndMessages(null);
            voiceRecordingTimer.setText("0:00");
        });

        sendVoiceBtn.setOnClickListener(v -> {
            if (mediaRecorder != null && isRecording) {
                try {
                    mediaRecorder.stop();
                    mediaRecorder.release();
                    mediaRecorder = null;
                } catch (Exception e) {}
                isRecording = false;
                voiceTimerHandler.removeCallbacksAndMessages(null);
                sendVoiceMessage();
            }
            messageInputContainer.setVisibility(View.VISIBLE);
            voiceRecordingPanel.setVisibility(View.GONE);
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void openVideoPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_VIDEO_REQUEST);
    }

    private void openAudioPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        startActivityForResult(Intent.createChooser(intent, "Select Audio"), PICK_AUDIO_REQUEST);
    }

    private void setupVoiceRecording() {
        voiceRecordingPanel.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startRecording();
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (isRecording) {
                            stopRecordingAndSend();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Microphone permission needed", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File voiceDir = new File(getCacheDir(), "voice_notes");
            if (!voiceDir.exists()) voiceDir.mkdirs();
            voiceFilePath = new File(voiceDir, "voice_" + System.currentTimeMillis() + ".3gp").getAbsolutePath();

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setOutputFile(voiceFilePath);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;

            voiceDuration = 0;
            voiceTimerHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    voiceDuration++;
                    voiceRecordingTimer.setText(formatVoiceDuration(voiceDuration));
                    voiceTimerHandler.postDelayed(this, 1000);
                }
            }, 1000);

            vibrate(50);
            playSound("record_start");

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to start recording: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecordingAndSend() {
        if (mediaRecorder != null && isRecording) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
            } catch (Exception e) {}
            isRecording = false;
            voiceTimerHandler.removeCallbacksAndMessages(null);

            if (voiceDuration >= 1) {
                // Send the voice message immediately
                sendVoiceMessageAndWait();
            } else {
                Toast.makeText(this, "Voice message too short", Toast.LENGTH_SHORT).show();
                new File(voiceFilePath).delete();
                // Return to normal mode
                messageInputContainer.setVisibility(View.VISIBLE);
                voiceRecordingPanel.setVisibility(View.GONE);
                voiceRecordingTimer.setText("0:00");
            }
        } else {
            messageInputContainer.setVisibility(View.VISIBLE);
            voiceRecordingPanel.setVisibility(View.GONE);
            voiceRecordingTimer.setText("0:00");
        }
    }

    private void sendVoiceMessageAndWait() {
        if (voiceFilePath == null) return;

        final File voiceFile = new File(voiceFilePath);
        if (!voiceFile.exists()) return;

        playSound("send");
        vibrate(30);

        // Use voicenotes.php for voice uploads
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("token", authToken)
                .addFormDataPart("receiver_id", String.valueOf(otherUserId))
                .addFormDataPart("duration", String.valueOf(voiceDuration))
                .addFormDataPart("voice", "voice.3gp",
                        RequestBody.create(MediaType.parse("audio/3gpp"), voiceFile));

        Request request = new Request.Builder()
                .url(BASE_URL + "voicenotes.php")
                .post(builder.build())
                .build();

        runOnUiThread(() -> Toast.makeText(this, "Sending voice message...", Toast.LENGTH_SHORT).show());

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(ChatActivity.this, "Failed to send voice: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    messageInputContainer.setVisibility(View.VISIBLE);
                    voiceRecordingPanel.setVisibility(View.GONE);
                    voiceRecordingTimer.setText("0:00");
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d("VOICE", "Server response: " + responseBody);

                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        if (json.getBoolean("success")) {
                            voiceFile.delete();
                            messageInputContainer.setVisibility(View.VISIBLE);
                            voiceRecordingPanel.setVisibility(View.GONE);
                            voiceRecordingTimer.setText("0:00");
                            voiceDuration = 0;
                            loadMessages();
                            Toast.makeText(ChatActivity.this, "Voice message sent!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ChatActivity.this, "Send failed: " + json.optString("message"), Toast.LENGTH_SHORT).show();
                            messageInputContainer.setVisibility(View.VISIBLE);
                            voiceRecordingPanel.setVisibility(View.GONE);
                            voiceRecordingTimer.setText("0:00");
                        }
                    } catch (Exception e) {
                        Log.e("VOICE", "Parse error: " + e.getMessage());
                        Toast.makeText(ChatActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        messageInputContainer.setVisibility(View.VISIBLE);
                        voiceRecordingPanel.setVisibility(View.GONE);
                        voiceRecordingTimer.setText("0:00");
                    }
                });
            }
        });
    }

    // Keep old method for compatibility
    private void sendVoiceMessage() {
        sendVoiceMessageAndWait();
    }

    private void setupCallButtons() {
        voiceCallIcon.setOnClickListener(v -> Toast.makeText(ChatActivity.this, "🔜 Voice calls coming soon!", Toast.LENGTH_SHORT).show());
        videoCallIcon.setOnClickListener(v -> Toast.makeText(ChatActivity.this, "🔜 Video calls coming soon!", Toast.LENGTH_SHORT).show());
    }

    private void animateHearts() {
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

    private void setupRecyclerView() {
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messagesAdapter = new MessageAdapter();
        messagesRecyclerView.setAdapter(messagesAdapter);
    }

    private void setupMessageInput() {
        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                sendTextMessage(message);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri selectedUri = data.getData();
            if (requestCode == PICK_IMAGE_REQUEST) {
                sendImageMessage(selectedUri);
            } else if (requestCode == PICK_VIDEO_REQUEST) {
                sendVideoMessage(selectedUri);
            } else if (requestCode == PICK_AUDIO_REQUEST) {
                sendAudioMessage(selectedUri);
            }
        }
    }

    private void sendTextMessage(String message) {
        messageInput.setText("");
        playSound("send");
        vibrate(30);

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("action", "send_text")
                .addFormDataPart("token", authToken)
                .addFormDataPart("receiver_id", String.valueOf(otherUserId))
                .addFormDataPart("message", message);

        Request request = new Request.Builder()
                .url(BASE_URL + "msg.php")
                .post(builder.build())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Failed to send message: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> loadMessages());
            }
        });
    }

    private void sendImageMessage(Uri uri) {
        playSound("send");
        vibrate(30);
        sendMediaFile(uri, "image/jpeg", "image_");
    }

    private void sendVideoMessage(Uri uri) {
        playSound("send");
        vibrate(30);
        sendMediaFile(uri, "video/mp4", "video_");
    }

    private void sendAudioMessage(Uri uri) {
        playSound("send");
        vibrate(30);
        String mimeType = getContentResolver().getType(uri);
        if (mimeType == null) mimeType = "audio/mpeg";
        sendMediaFile(uri, mimeType, "audio_");
    }

    private void sendMediaFile(Uri uri, String mimeType, String prefix) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            byte[] fileBytes = getBytes(inputStream);
            String fileName = prefix + System.currentTimeMillis() + getFileExtension(mimeType);

            runOnUiThread(() -> Toast.makeText(this, "Sending " + prefix + "...", Toast.LENGTH_SHORT).show());

            MultipartBody.Builder builder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("action", "send_media")
                    .addFormDataPart("token", authToken)
                    .addFormDataPart("receiver_id", String.valueOf(otherUserId))
                    .addFormDataPart("media", fileName, RequestBody.create(MediaType.parse(mimeType), fileBytes));

            Request request = new Request.Builder()
                    .url(BASE_URL + "msg.php")
                    .post(builder.build())
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    runOnUiThread(() -> loadMessages());
                }
            });
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    private String getFileExtension(String mimeType) {
        if (mimeType.contains("jpeg") || mimeType.contains("jpg")) return ".jpg";
        if (mimeType.contains("png")) return ".png";
        if (mimeType.contains("mp4")) return ".mp4";
        if (mimeType.contains("mp3")) return ".mp3";
        if (mimeType.contains("mpeg")) return ".mp3";
        return ".file";
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

    private void loadMessages() {
        if (isLoading) return;
        isLoading = true;

        String url = BASE_URL + "msg.php?action=get&token=" + authToken + "&other_user_id=" + otherUserId +
                "&limit=" + PAGE_SIZE + "&offset=" + currentOffset;

        Request request = new Request.Builder().url(url).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> isLoading = false);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body().string();
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        if (json.getBoolean("success")) {
                            JSONArray messagesArray = json.getJSONArray("messages");
                            hasMore = json.getBoolean("has_more");

                            if (currentOffset == 0) {
                                messagesList.clear();
                            }

                            for (int i = 0; i < messagesArray.length(); i++) {
                                JSONObject msg = messagesArray.getJSONObject(i);
                                Message message = new Message();
                                message.id = msg.getInt("id");
                                message.senderId = msg.getInt("sender_id");
                                message.text = msg.getString("text");
                                message.type = msg.getString("type");
                                message.duration = msg.optInt("duration");
                                message.mediaId = msg.optInt("media_id");
                                message.mediaPath = msg.optString("media_path", "");
                                message.fileName = msg.optString("file_name", "");
                                message.fileSize = msg.optString("file_size", "0");
                                message.createdAt = msg.getString("created_at");
                                messagesList.add(message);
                            }

                            currentOffset += messagesArray.length();
                            messagesAdapter.notifyDataSetChanged();

                            if (messagesList.size() > 0) {
                                messagesRecyclerView.scrollToPosition(messagesList.size() - 1);
                            }
                        }
                        isLoading = false;
                    } catch (Exception e) {
                        isLoading = false;
                    }
                });
            }
        });
    }

    private void markMessagesAsRead() {
        try {
            JSONObject json = new JSONObject();
            json.put("action", "mark_read");
            json.put("token", authToken);
            json.put("other_user_id", otherUserId);

            RequestBody body = RequestBody.create(MediaType.parse("application/json"), json.toString());
            Request request = new Request.Builder().url(BASE_URL + "msg.php").post(body).build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {}

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {}
            });
        } catch (Exception e) {}
    }

    private void startPolling() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing() && !isDestroyed()) {
                    loadMessages();
                    handler.postDelayed(this, 5000);
                }
            }
        }, 5000);
    }

    private void playSound(String type) {
        try {
            int soundRes = 0;
            switch (type) {
                case "send":
                    soundRes = getResources().getIdentifier("send_sound", "raw", getPackageName());
                    break;
                case "receive":
                    soundRes = getResources().getIdentifier("receive_sound", "raw", getPackageName());
                    break;
                case "record_start":
                    soundRes = getResources().getIdentifier("record_start", "raw", getPackageName());
                    break;
            }
            if (soundRes != 0 && mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(this, soundRes);
                if (mediaPlayer != null) {
                    mediaPlayer.start();
                    mediaPlayer.setOnCompletionListener(mp -> {
                        mp.release();
                        mediaPlayer = null;
                    });
                }
            }
        } catch (Exception e) {}
    }

    private void vibrate(long duration) {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(duration);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mediaRecorder != null) {
            try {
                mediaRecorder.release();
            } catch (Exception e) {}
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Microphone permission granted! Press and hold to record.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Microphone permission denied. Voice messages won't work.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Message Model
    static class Message {
        int id;
        int senderId;
        String text;
        String type;
        int duration;
        int mediaId;
        String mediaPath;
        String fileName;
        String fileSize;
        String createdAt;
    }

    // Message Adapter
    class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_SENT = 0;
        private static final int TYPE_RECEIVED = 1;
        private MediaPlayer voicePlayer = null;
        private int currentPlayingPosition = -1;

        @Override
        public int getItemViewType(int position) {
            return messagesList.get(position).senderId == userId ? TYPE_SENT : TYPE_RECEIVED;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View view = inflater.inflate(viewType == TYPE_SENT ? R.layout.item_chat_sent : R.layout.item_chat_received, parent, false);
            return viewType == TYPE_SENT ? new SentViewHolder(view) : new ReceivedViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Message msg = messagesList.get(position);
            if (holder instanceof SentViewHolder) {
                ((SentViewHolder) holder).bind(msg, position);
            } else {
                ((ReceivedViewHolder) holder).bind(msg, position);
            }
        }

        @Override
        public int getItemCount() {
            return messagesList.size();
        }

        class SentViewHolder extends RecyclerView.ViewHolder {
            TextView messageText, timeText;
            LinearLayout mediaContainer, voiceContainer;
            CardView imageCard, videoCard;
            ImageView messageImage, videoThumbnail, playButton, downloadImageBtn;
            TextView voiceDurationText, voicePlayPause;

            SentViewHolder(View itemView) {
                super(itemView);
                messageText = itemView.findViewById(R.id.messageText);
                timeText = itemView.findViewById(R.id.timeText);
                mediaContainer = itemView.findViewById(R.id.mediaContainer);
                voiceContainer = itemView.findViewById(R.id.voiceContainer);
                imageCard = itemView.findViewById(R.id.imageCard);
                videoCard = itemView.findViewById(R.id.videoCard);
                messageImage = itemView.findViewById(R.id.messageImage);
                videoThumbnail = itemView.findViewById(R.id.videoThumbnail);
                playButton = itemView.findViewById(R.id.playButton);
                downloadImageBtn = itemView.findViewById(R.id.downloadImageBtn);
                voiceDurationText = itemView.findViewById(R.id.voiceDurationText);
                voicePlayPause = itemView.findViewById(R.id.voicePlayPause);
            }

            void bind(Message msg, int position) {
                messageText.setText(msg.text);
                timeText.setText(formatTime(msg.createdAt));

                mediaContainer.setVisibility(View.GONE);
                imageCard.setVisibility(View.GONE);
                videoCard.setVisibility(View.GONE);
                voiceContainer.setVisibility(View.GONE);

                if (msg.type.equals("voice") && msg.mediaPath != null && !msg.mediaPath.isEmpty()) {
                    voiceContainer.setVisibility(View.VISIBLE);
                    voiceDurationText.setText(formatVoiceDuration(msg.duration));
                    voicePlayPause.setOnClickListener(v -> toggleVoicePlayback(msg.mediaPath, voicePlayPause, position));
                } else if (msg.type.equals("image") && msg.mediaPath != null && !msg.mediaPath.isEmpty()) {
                    mediaContainer.setVisibility(View.VISIBLE);
                    imageCard.setVisibility(View.VISIBLE);
                    String url = BASE_URL + "get_image.php?path=" + msg.mediaPath + "&token=" + authToken;
                    Glide.with(ChatActivity.this).load(url).into(messageImage);
                    messageImage.setOnClickListener(v -> zoomImage(msg.mediaPath));
                    if (downloadImageBtn != null) {
                        downloadImageBtn.setOnClickListener(v -> saveImageToDevice(url));
                    }
                } else if (msg.type.equals("video") && msg.mediaPath != null && !msg.mediaPath.isEmpty()) {
                    mediaContainer.setVisibility(View.VISIBLE);
                    videoCard.setVisibility(View.VISIBLE);
                    String url = BASE_URL + "get_image.php?path=" + msg.mediaPath + "&token=" + authToken;
                    Glide.with(ChatActivity.this).load(url).into(videoThumbnail);
                    playButton.setOnClickListener(v -> playVideo(msg.mediaPath));
                } else if (msg.type.equals("audio") && msg.mediaPath != null && !msg.mediaPath.isEmpty()) {
                    mediaContainer.setVisibility(View.VISIBLE);
                    videoCard.setVisibility(View.VISIBLE);
                    videoThumbnail.setImageResource(R.drawable.ic_music);
                    playButton.setOnClickListener(v -> playAudio(msg.mediaPath));
                }
            }
        }

        class ReceivedViewHolder extends RecyclerView.ViewHolder {
            TextView messageText, timeText;
            LinearLayout mediaContainer, voiceContainer;
            CardView imageCard, videoCard;
            ImageView messageImage, videoThumbnail, playButton, downloadImageBtn;
            TextView voiceDurationText, voicePlayPause;

            ReceivedViewHolder(View itemView) {
                super(itemView);
                messageText = itemView.findViewById(R.id.messageText);
                timeText = itemView.findViewById(R.id.timeText);
                mediaContainer = itemView.findViewById(R.id.mediaContainer);
                voiceContainer = itemView.findViewById(R.id.voiceContainer);
                imageCard = itemView.findViewById(R.id.imageCard);
                videoCard = itemView.findViewById(R.id.videoCard);
                messageImage = itemView.findViewById(R.id.messageImage);
                videoThumbnail = itemView.findViewById(R.id.videoThumbnail);
                playButton = itemView.findViewById(R.id.playButton);
                downloadImageBtn = itemView.findViewById(R.id.downloadImageBtn);
                voiceDurationText = itemView.findViewById(R.id.voiceDurationText);
                voicePlayPause = itemView.findViewById(R.id.voicePlayPause);
            }

            void bind(Message msg, int position) {
                messageText.setText(msg.text);
                timeText.setText(formatTime(msg.createdAt));

                mediaContainer.setVisibility(View.GONE);
                imageCard.setVisibility(View.GONE);
                videoCard.setVisibility(View.GONE);
                voiceContainer.setVisibility(View.GONE);

                if (msg.type.equals("voice") && msg.mediaPath != null && !msg.mediaPath.isEmpty()) {
                    voiceContainer.setVisibility(View.VISIBLE);
                    voiceDurationText.setText(formatVoiceDuration(msg.duration));
                    voicePlayPause.setOnClickListener(v -> toggleVoicePlayback(msg.mediaPath, voicePlayPause, position));
                } else if (msg.type.equals("image") && msg.mediaPath != null && !msg.mediaPath.isEmpty()) {
                    mediaContainer.setVisibility(View.VISIBLE);
                    imageCard.setVisibility(View.VISIBLE);
                    String url = BASE_URL + "get_image.php?path=" + msg.mediaPath + "&token=" + authToken;
                    Glide.with(ChatActivity.this).load(url).into(messageImage);
                    messageImage.setOnClickListener(v -> zoomImage(msg.mediaPath));
                    if (downloadImageBtn != null) {
                        downloadImageBtn.setOnClickListener(v -> saveImageToDevice(url));
                    }
                } else if (msg.type.equals("video") && msg.mediaPath != null && !msg.mediaPath.isEmpty()) {
                    mediaContainer.setVisibility(View.VISIBLE);
                    videoCard.setVisibility(View.VISIBLE);
                    String url = BASE_URL + "get_image.php?path=" + msg.mediaPath + "&token=" + authToken;
                    Glide.with(ChatActivity.this).load(url).into(videoThumbnail);
                    playButton.setOnClickListener(v -> playVideo(msg.mediaPath));
                } else if (msg.type.equals("audio") && msg.mediaPath != null && !msg.mediaPath.isEmpty()) {
                    mediaContainer.setVisibility(View.VISIBLE);
                    videoCard.setVisibility(View.VISIBLE);
                    videoThumbnail.setImageResource(R.drawable.ic_music);
                    playButton.setOnClickListener(v -> playAudio(msg.mediaPath));
                }
            }
        }

        private void toggleVoicePlayback(String path, TextView playPauseBtn, int position) {
            String url = BASE_URL + "get_image.php?path=" + path + "&token=" + authToken;

            if (voicePlayer != null && currentPlayingPosition == position) {
                if (voicePlayer.isPlaying()) {
                    voicePlayer.pause();
                    playPauseBtn.setText("▶");
                } else {
                    voicePlayer.start();
                    playPauseBtn.setText("⏸");
                }
                return;
            }

            if (voicePlayer != null) {
                voicePlayer.release();
                voicePlayer = null;
            }

            try {
                voicePlayer = new MediaPlayer();
                voicePlayer.setDataSource(url);
                voicePlayer.prepareAsync();
                voicePlayer.setOnPreparedListener(mp -> {
                    mp.start();
                    playPauseBtn.setText("⏸");
                    currentPlayingPosition = position;
                });
                voicePlayer.setOnCompletionListener(mp -> {
                    playPauseBtn.setText("▶");
                    currentPlayingPosition = -1;
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void playAudio(String path) {
            String url = BASE_URL + "get_image.php?path=" + path + "&token=" + authToken;
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(url), "audio/*");
            startActivity(intent);
        }

        private void zoomImage(String path) {
            Dialog dialog = new Dialog(ChatActivity.this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_image_zoom);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
            ImageView image = dialog.findViewById(R.id.zoomImageView);
            ImageView saveBtn = dialog.findViewById(R.id.saveImageView);
            String url = BASE_URL + "get_image.php?path=" + path + "&token=" + authToken;
            Glide.with(ChatActivity.this).load(url).into(image);
            if (saveBtn != null) {
                saveBtn.setOnClickListener(v -> saveImageToDevice(url));
            }
            image.setOnClickListener(v -> dialog.dismiss());
            dialog.show();
        }

        private void saveImageToDevice(String imageUrl) {
            Toast.makeText(ChatActivity.this, "Saving...", Toast.LENGTH_SHORT).show();
            new Thread(() -> {
                try {
                    java.net.URL url = new java.net.URL(imageUrl);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setDoInput(true);
                    conn.connect();
                    Bitmap bitmap = BitmapFactory.decodeStream(conn.getInputStream());
                    conn.disconnect();

                    if (bitmap != null) {
                        runOnUiThread(() -> saveBitmap(bitmap));
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Save failed", Toast.LENGTH_SHORT).show());
                }
            }).start();
        }

        private void saveBitmap(Bitmap bitmap) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentResolver resolver = getContentResolver();
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, "Soulmate_Image_" + System.currentTimeMillis() + ".jpg");
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Soulmate");
                    Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    if (uri != null) {
                        OutputStream os = resolver.openOutputStream(uri);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, os);
                        os.close();
                        Toast.makeText(ChatActivity.this, "Image saved to Gallery", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Soulmate");
                    if (!dir.exists()) dir.mkdirs();
                    File file = new File(dir, "Soulmate_Image_" + System.currentTimeMillis() + ".jpg");
                    FileOutputStream fos = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                    fos.close();
                    Toast.makeText(ChatActivity.this, "Image saved to Gallery", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(ChatActivity.this, "Save failed", Toast.LENGTH_SHORT).show();
            }
        }

        private void playVideo(String path) {
            String url = BASE_URL + "get_image.php?path=" + path + "&token=" + authToken;
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(url), "video/mp4");
            startActivity(intent);
        }
    }

    private String formatTime(String timestamp) {
        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = input.parse(timestamp);
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
        } catch (Exception e) {
            return "";
        }
    }

    private String formatVoiceDuration(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", minutes, secs);
    }
}