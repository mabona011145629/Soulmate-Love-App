package com.mabona.mobilesystems.soulmate;

import android.Manifest;
import android.animation.ObjectAnimator;
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
import android.text.ClipboardManager;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
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

public class LoveDoctorChatActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final int PICK_IMAGE_REQUEST = 100;
    private static final int PICK_VIDEO_REQUEST = 101;
    private static final int PICK_AUDIO_REQUEST = 104;

    // UI Components
    private ImageView backButton;
    private TextView doctorNameText;
    private TextView onlineStatusText;
    private RecyclerView messagesRecyclerView;
    private EditText messageInput;
    private ImageView sendButton;
    private ImageView attachButton;
    private ImageView voiceButton;
    private LinearLayout attachmentPanel;
    private CardView attachImageBtn, attachVideoBtn, attachMusicBtn;
    private LinearLayout messageInputContainer;
    private ImageView invitePartnerIcon;
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
    private int doctorId;
    private int sessionId;
    private String doctorName;
    private String sessionPin;

    private TherapyMessageAdapter messagesAdapter;
    private List<TherapyMessage> messagesList = new ArrayList<>();
    private OkHttpClient client;
    private Handler handler = new Handler();
    private boolean isLoading = false;
    private boolean hasMore = true;
    private int currentOffset = 0;
    private static final int PAGE_SIZE = 50;

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;

    private static final String BASE_URL = "https://mabona.firstsuninvestment.com/soulmate/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_love_doctor_chat);

        client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        // Get intent data
        userId = getIntent().getIntExtra("USER_ID", -1);
        authToken = getIntent().getStringExtra("AUTH_TOKEN");
        doctorId = getIntent().getIntExtra("DOCTOR_ID", -1);
        sessionId = getIntent().getIntExtra("SESSION_ID", -1);
        doctorName = getIntent().getStringExtra("DOCTOR_NAME");
        sessionPin = getIntent().getStringExtra("SESSION_PIN");

        if (userId == -1 || authToken == null || doctorId == -1 || sessionId == -1) {
            Toast.makeText(this, "Invalid chat session", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupRecyclerView();
        setupMessageInput();
        setupVoiceRecording();
        setupInvitePartner();
        animateHearts();
        loadMessages();
        startPolling();

        // Disable copying to clipboard
        disableCopyPaste();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        doctorNameText = findViewById(R.id.doctorNameText);
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
        invitePartnerIcon = findViewById(R.id.invitePartnerIcon);
        heartImage1 = findViewById(R.id.heartImage1);
        heartImage2 = findViewById(R.id.heartImage2);
        heartImage3 = findViewById(R.id.heartImage3);
        voiceRecordingPanel = findViewById(R.id.voiceRecordingPanel);
        voiceRecordingTimer = findViewById(R.id.voiceRecordingTimer);
        cancelVoiceBtn = findViewById(R.id.cancelVoiceBtn);
        sendVoiceBtn = findViewById(R.id.sendVoiceBtn);

        doctorNameText.setText(doctorName != null ? doctorName : "Love Doctor");
        onlineStatusText.setText("Therapy Session Active 💕");

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
                Toast.makeText(this, "Microphone permission needed for voice messages", Toast.LENGTH_LONG).show();
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

    private void disableCopyPaste() {
        messageInput.setCustomSelectionActionModeCallback(new android.view.ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(android.view.ActionMode mode, android.view.Menu menu) { return false; }
            @Override
            public boolean onPrepareActionMode(android.view.ActionMode mode, android.view.Menu menu) { return false; }
            @Override
            public boolean onActionItemClicked(android.view.ActionMode mode, android.view.MenuItem item) { return false; }
            @Override
            public void onDestroyActionMode(android.view.ActionMode mode) {}
        });
        messageInput.setLongClickable(false);
        messageInput.setTextIsSelectable(false);
    }

    private void setupInvitePartner() {
        invitePartnerIcon.setOnClickListener(v -> {
            showInvitePartnerDialog();
        });
    }

    private void showInvitePartnerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_invite_partner, null);
        EditText emailInput = view.findViewById(R.id.partnerEmailInput);

        builder.setView(view)
                .setTitle("💕 Invite Your Partner")
                .setPositiveButton("Send Invite", (dialog, which) -> {
                    String email = emailInput.getText().toString().trim();
                    if (!email.isEmpty()) {
                        sendInviteToPartner(email);
                    } else {
                        Toast.makeText(this, "Please enter partner's email", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendInviteToPartner(String email) {
        OkHttpClient client = new OkHttpClient();
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("action", "invite_partner")
                .addFormDataPart("token", authToken)
                .addFormDataPart("session_id", String.valueOf(sessionId))
                .addFormDataPart("partner_email", email)
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + "dr.php")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(LoveDoctorChatActivity.this, "Failed to send invite", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> Toast.makeText(LoveDoctorChatActivity.this, "Invitation sent to your partner!", Toast.LENGTH_LONG).show());
            }
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

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show();
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
                sendVoiceMessage();
            } else {
                Toast.makeText(this, "Voice message too short", Toast.LENGTH_SHORT).show();
                new File(voiceFilePath).delete();
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

    private void sendVoiceMessage() {
        if (voiceFilePath == null) return;

        final File voiceFile = new File(voiceFilePath);
        if (!voiceFile.exists()) return;

        vibrate(30);

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("action", "send_media")
                .addFormDataPart("token", authToken)
                .addFormDataPart("session_id", String.valueOf(sessionId))
                .addFormDataPart("duration", String.valueOf(voiceDuration))
                .addFormDataPart("media", "voice.3gp",
                        RequestBody.create(MediaType.parse("audio/3gpp"), voiceFile));

        Request request = new Request.Builder()
                .url(BASE_URL + "dr.php")
                .post(builder.build())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(LoveDoctorChatActivity.this, "Failed to send voice", Toast.LENGTH_SHORT).show();
                    messageInputContainer.setVisibility(View.VISIBLE);
                    voiceRecordingPanel.setVisibility(View.GONE);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> {
                    voiceFile.delete();
                    messageInputContainer.setVisibility(View.VISIBLE);
                    voiceRecordingPanel.setVisibility(View.GONE);
                    voiceRecordingTimer.setText("0:00");
                    voiceDuration = 0;
                    loadMessages();
                });
            }
        });
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
        messagesAdapter = new TherapyMessageAdapter();
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
                sendMediaMessage(selectedUri, "image");
            } else if (requestCode == PICK_VIDEO_REQUEST) {
                sendMediaMessage(selectedUri, "video");
            } else if (requestCode == PICK_AUDIO_REQUEST) {
                sendMediaMessage(selectedUri, "audio");
            }
        }
    }

    private void sendTextMessage(String message) {
        messageInput.setText("");
        vibrate(30);

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("action", "send_message")
                .addFormDataPart("token", authToken)
                .addFormDataPart("session_id", String.valueOf(sessionId))
                .addFormDataPart("message", message)
                .addFormDataPart("message_type", "text");

        Request request = new Request.Builder()
                .url(BASE_URL + "dr.php")
                .post(builder.build())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(LoveDoctorChatActivity.this, "Failed to send", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> loadMessages());
            }
        });
    }

    private void sendMediaMessage(Uri uri, String type) {
        vibrate(30);

        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            byte[] fileBytes = getBytes(inputStream);
            String fileName = type + "_" + System.currentTimeMillis() + ".jpg";

            String mimeType = getContentResolver().getType(uri);
            if (mimeType == null) mimeType = "image/jpeg";

            MultipartBody.Builder builder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("action", "send_media")
                    .addFormDataPart("token", authToken)
                    .addFormDataPart("session_id", String.valueOf(sessionId))
                    .addFormDataPart("media", fileName, RequestBody.create(MediaType.parse(mimeType), fileBytes));

            Request request = new Request.Builder()
                    .url(BASE_URL + "dr.php")
                    .post(builder.build())
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> Toast.makeText(LoveDoctorChatActivity.this, "Failed to send", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    runOnUiThread(() -> loadMessages());
                }
            });
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
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

        String url = BASE_URL + "dr.php?action=get_messages&token=" + authToken +
                "&session_id=" + sessionId + "&limit=" + PAGE_SIZE + "&offset=" + currentOffset;

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

                            if (currentOffset == 0) {
                                messagesList.clear();
                            }

                            for (int i = 0; i < messagesArray.length(); i++) {
                                JSONObject msg = messagesArray.getJSONObject(i);
                                TherapyMessage message = new TherapyMessage();
                                message.id = msg.getInt("id");
                                message.senderId = msg.getInt("sender_id");
                                message.text = msg.getString("text");
                                message.type = msg.getString("type");
                                message.mediaPath = msg.optString("media_path", "");
                                message.isDeleted = msg.optBoolean("is_deleted", false);
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

    private void startPolling() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing() && !isDestroyed()) {
                    loadMessages();
                    handler.postDelayed(this, 3000);
                }
            }
        }, 3000);
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

    private String formatVoiceDuration(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", minutes, secs);
    }

    // ==================== INNER CLASSES ====================

    static class TherapyMessage {
        int id;
        int senderId;
        String text;
        String type;
        String mediaPath;
        boolean isDeleted;
        String createdAt;
    }

    class TherapyMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
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
            return new MessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            TherapyMessage msg = messagesList.get(position);
            MessageViewHolder vh = (MessageViewHolder) holder;

            vh.messageText.setText(msg.text);
            vh.timeText.setText(formatTime(msg.createdAt));

            // Long press to delete message
            vh.itemView.setOnLongClickListener(v -> {
                showDeleteMessageDialog(msg.id);
                return true;
            });
        }

        private void showDeleteMessageDialog(int messageId) {
            new AlertDialog.Builder(LoveDoctorChatActivity.this)
                    .setTitle("Delete Message")
                    .setMessage("Delete this message? It will be replaced with a warm note.")
                    .setPositiveButton("Delete", (dialog, which) -> deleteMessage(messageId))
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        private void deleteMessage(int messageId) {
            OkHttpClient client = new OkHttpClient();
            RequestBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("action", "delete_message")
                    .addFormDataPart("token", authToken)
                    .addFormDataPart("message_id", String.valueOf(messageId))
                    .build();

            Request request = new Request.Builder()
                    .url(BASE_URL + "dr.php")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {}

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    runOnUiThread(() -> loadMessages());
                }
            });
        }

        @Override
        public int getItemCount() {
            return messagesList.size();
        }

        class MessageViewHolder extends RecyclerView.ViewHolder {
            TextView messageText, timeText;

            MessageViewHolder(View itemView) {
                super(itemView);
                messageText = itemView.findViewById(R.id.messageText);
                timeText = itemView.findViewById(R.id.timeText);
            }
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
}