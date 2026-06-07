package com.mabona.mobilesystems.soulmate;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.text.Editable;
import android.text.TextWatcher;
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
    private CardView messageInputContainer;
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

    private MessageAdapter messagesAdapter;
    private List<Message> messagesList = new ArrayList<>();
    private OkHttpClient client;
    private Handler handler = new Handler();
    private boolean isLoading = false;
    private static final int PAGE_SIZE = 50;

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;

    private static final String BASE_URL = "https://mabona.firstsuninvestment.com/soulmate/";
    public static int activeSessionId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_love_doctor_chat);
        
        activeSessionId = getIntent().getIntExtra("SESSION_ID", -1);

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
                Toast.makeText(this, "Microphone permission needed", Toast.LENGTH_LONG).show();
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
        invitePartnerIcon.setOnClickListener(v -> showSearchInviteDialog());
    }

    private void showSearchInviteDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_search_partner);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        EditText searchInput = dialog.findViewById(R.id.partnerSearchInput);
        RecyclerView resultsList = dialog.findViewById(R.id.partnerSearchResults);
        resultsList.setLayoutManager(new LinearLayoutManager(this));
        
        List<SearchUser> searchResults = new ArrayList<>();
        SearchUserAdapter adapter = new SearchUserAdapter(searchResults, user -> {
            invitePartnerToTherapy(user.id);
            dialog.dismiss();
        });
        resultsList.setAdapter(adapter);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 2) {
                    searchPartners(s.toString(), searchResults, adapter);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        dialog.show();
    }

    private void searchPartners(String query, List<SearchUser> list, SearchUserAdapter adapter) {
        String url = BASE_URL + "dr.php?action=search_partner&token=" + authToken + "&q=" + query;
        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Log.e("LoveDoctorChat", "Search failed: " + e.getMessage());
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                final String body = response.body().string();
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(body);
                        if (json.optBoolean("success", false)) {
                            JSONArray users = json.optJSONArray("users");
                            list.clear();
                            if (users != null) {
                                for (int i = 0; i < users.length(); i++) {
                                    JSONObject u = users.getJSONObject(i);
                                    list.add(new SearchUser(
                                            u.optInt("dater_id", 0),
                                            u.optString("first_name", "") + " " + u.optString("last_name", ""),
                                            u.optString("profile_image", ""),
                                            u.optString("bio", "")
                                    ));
                                }
                            }
                            adapter.notifyDataSetChanged();
                        }
                    } catch (Exception e) {
                        Log.e("LoveDoctorChat", "Search parse error: " + e.getMessage());
                    }
                });
            }
        });
    }

    private void invitePartnerToTherapy(int partnerId) {
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("action", "invite_partner")
                .addFormDataPart("token", authToken)
                .addFormDataPart("session_id", String.valueOf(sessionId))
                .addFormDataPart("target_id", String.valueOf(partnerId));

        Request request = new Request.Builder()
                .url(BASE_URL + "dr.php")
                .post(builder.build())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> Toast.makeText(LoveDoctorChatActivity.this, "Invitation sent! 💕", Toast.LENGTH_SHORT).show());
            }
        });
    }

    static class SearchUser {
        int id; String name, image, bio;
        SearchUser(int id, String name, String image, String bio) { this.id = id; this.name = name; this.image = image; this.bio = bio; }
    }

    class SearchUserAdapter extends RecyclerView.Adapter<SearchUserAdapter.VH> {
        List<SearchUser> list; OnUserClickListener listener;
        SearchUserAdapter(List<SearchUser> list, OnUserClickListener listener) { this.list = list; this.listener = listener; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_search_invite, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            SearchUser u = list.get(pos);
            h.name.setText(u.name); h.bio.setText(u.bio);
            Glide.with(LoveDoctorChatActivity.this).load(BASE_URL + "get_image.php?path=" + u.image + "&token=" + authToken).placeholder(R.drawable.default_profile).into(h.img);
            h.itemView.setOnClickListener(v -> listener.onUserClick(u));
        }
        @Override public int getItemCount() { return list.size(); }
        class VH extends RecyclerView.ViewHolder {
            ImageView img; TextView name, bio;
            VH(View v) { super(v); img = v.findViewById(R.id.partnerImage); name = v.findViewById(R.id.partnerName); bio = v.findViewById(R.id.partnerBio); }
        }
    }
    interface OnUserClickListener { void onUserClick(SearchUser user); }

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
            @Override public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: startRecording(); return true;
                    case MotionEvent.ACTION_UP: if (isRecording) stopRecordingAndSend(); return true;
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
            mediaRecorder.prepare(); mediaRecorder.start();
            isRecording = true; voiceDuration = 0;
            voiceTimerHandler.postDelayed(new Runnable() {
                @Override public void run() { voiceDuration++; voiceRecordingTimer.setText(formatVoiceDuration(voiceDuration)); voiceTimerHandler.postDelayed(this, 1000); }
            }, 1000);
            vibrate(50);
        } catch (Exception e) { Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show(); }
    }

    private void stopRecordingAndSend() {
        if (mediaRecorder != null && isRecording) {
            try { mediaRecorder.stop(); mediaRecorder.release(); mediaRecorder = null; } catch (Exception e) {}
            isRecording = false; voiceTimerHandler.removeCallbacksAndMessages(null);
            if (voiceDuration >= 1) sendVoiceMessage();
            else { Toast.makeText(this, "Too short", Toast.LENGTH_SHORT).show(); new File(voiceFilePath).delete(); }
        }
        messageInputContainer.setVisibility(View.VISIBLE);
        voiceRecordingPanel.setVisibility(View.GONE);
        voiceRecordingTimer.setText("0:00");
    }

    private void sendVoiceMessage() {
        if (voiceFilePath == null) return;
        final File voiceFile = new File(voiceFilePath);
        if (!voiceFile.exists()) return;
        vibrate(30);
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("token", authToken).addFormDataPart("session_id", String.valueOf(sessionId))
                .addFormDataPart("duration", String.valueOf(voiceDuration))
                .addFormDataPart("voice", "voice.3gp", RequestBody.create(MediaType.parse("audio/3gpp"), voiceFile));
        Request request = new Request.Builder().url(BASE_URL + "drvoicenotes.php").post(builder.build()).build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> { voiceFile.delete(); loadMessages(); });
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
        messagesAdapter = new MessageAdapter();
        messagesRecyclerView.setAdapter(messagesAdapter);
    }

    private void setupMessageInput() {
        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) sendTextMessage(message);
        });
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            if (requestCode == PICK_IMAGE_REQUEST) sendMediaMessage(uri, "image");
            else if (requestCode == PICK_VIDEO_REQUEST) sendMediaMessage(uri, "video");
            else if (requestCode == PICK_AUDIO_REQUEST) sendMediaMessage(uri, "audio");
        }
    }

    private void sendTextMessage(String message) {
        messageInput.setText(""); vibrate(30);
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("action", "send_message").addFormDataPart("token", authToken)
                .addFormDataPart("session_id", String.valueOf(sessionId))
                .addFormDataPart("message", message).addFormDataPart("message_type", "text");
        Request request = new Request.Builder().url(BASE_URL + "dr.php").post(builder.build()).build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> loadMessages());
            }
        });
    }

    private void sendMediaMessage(Uri uri, String type) {
        vibrate(30);
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            byte[] fileBytes = getBytes(inputStream);
            String fileName = type + "_" + System.currentTimeMillis() + (type.equals("video") ? ".mp4" : ".jpg");
            String mimeType = getContentResolver().getType(uri);
            if (mimeType == null) mimeType = type.equals("video") ? "video/mp4" : "image/jpeg";
            MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("action", "send_media").addFormDataPart("token", authToken)
                    .addFormDataPart("session_id", String.valueOf(sessionId))
                    .addFormDataPart("media", fileName, RequestBody.create(MediaType.parse(mimeType), fileBytes));
            Request request = new Request.Builder().url(BASE_URL + "dr.php").post(builder.build()).build();
            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {}
                @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    runOnUiThread(() -> loadMessages());
                }
            });
        } catch (Exception e) {}
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192]; int len;
        while ((len = inputStream.read(buffer)) != -1) byteBuffer.write(buffer, 0, len);
        return byteBuffer.toByteArray();
    }

    private void loadMessages() {
        if (isLoading) return; isLoading = true;
        String url = BASE_URL + "dr.php?action=get_messages&token=" + authToken + "&session_id=" + sessionId;
        client.newCall(new Request.Builder().url(url).get().build()).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { isLoading = false; }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String body = response.body().string();
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(body);
                        if (json.optBoolean("success", false)) {
                            JSONArray array = json.optJSONArray("messages");
                            if (array != null) {
                                messagesList.clear();
                                for (int i = 0; i < array.length(); i++) {
                                    JSONObject m = array.getJSONObject(i);
                                    Message msg = new Message();
                                    msg.id = m.optInt("message_id", 0);
                                    msg.senderId = m.optInt("sender_id", 0);
                                    msg.text = m.optString("message_text", "");
                                    msg.type = m.optString("message_type", "text");
                                    msg.mediaPath = m.optString("media_path", "");
                                    msg.duration = m.optInt("voice_duration", 0);
                                    msg.createdAt = m.optString("created_at", "");
                                    messagesList.add(msg);
                                }
                                messagesAdapter.notifyDataSetChanged();
                                if (!messagesList.isEmpty()) messagesRecyclerView.scrollToPosition(messagesList.size() - 1);
                            }

                            // Update Online Status
                            if (json.has("is_online")) {
                                boolean isOnline = json.getBoolean("is_online");
                                onlineStatusText.setText(isOnline ? "Doctor is Online 💕" : "Doctor is Away");
                                onlineStatusText.setTextColor(isOnline ? Color.parseColor("#FF1493") : Color.GRAY);
                            }
                        }
                    } catch (Exception e) {
                        Log.e("LoveDoctorChat", "Load messages error: " + e.getMessage());
                    }
                    isLoading = false;
                });
            }
        });
    }

    private void startPolling() {
        handler.postDelayed(new Runnable() {
            @Override public void run() { if (!isFinishing() && !isDestroyed()) { loadMessages(); handler.postDelayed(this, 3000); } }
        }, 3000);
    }

    private void vibrate(long duration) {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
            else vibrator.vibrate(duration);
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy(); handler.removeCallbacksAndMessages(null);
        if (activeSessionId == sessionId) activeSessionId = -1;
        if (mediaPlayer != null) { mediaPlayer.release(); mediaPlayer = null; }
        if (mediaRecorder != null) { try { mediaRecorder.release(); } catch (Exception e) {} }
    }

    @Override
    protected void onResume() {
        super.onResume();
        activeSessionId = sessionId;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (activeSessionId == sessionId) activeSessionId = -1;
    }

    private String formatVoiceDuration(int seconds) { return String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60); }

    static class Message { int id, senderId, duration; String text, type, mediaPath, createdAt; }

    class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_SENT = 0; private static final int TYPE_RECEIVED = 1;
        private MediaPlayer voicePlayer = null; private int currentPlayingPosition = -1;

        @Override public int getItemViewType(int pos) { return messagesList.get(pos).senderId == userId ? TYPE_SENT : TYPE_RECEIVED; }
        @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return vt == TYPE_SENT ? new SentVH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_chat_sent, p, false)) : new ReceivedVH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_chat_received, p, false));
        }
        @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
            Message m = messagesList.get(pos); if (h instanceof SentVH) ((SentVH) h).bind(m, pos); else ((ReceivedVH) h).bind(m, pos);
        }
        @Override public int getItemCount() { return messagesList.size(); }

        class SentVH extends RecyclerView.ViewHolder {
            TextView txt, time, vDur, vPlay; LinearLayout media, voice; CardView imgCard, vidCard; ImageView img, vid, play;
            SentVH(View v) { super(v); txt = v.findViewById(R.id.messageText); time = v.findViewById(R.id.timeText); media = v.findViewById(R.id.mediaContainer); voice = v.findViewById(R.id.voiceContainer); imgCard = v.findViewById(R.id.imageCard); vidCard = v.findViewById(R.id.videoCard); img = v.findViewById(R.id.messageImage); vid = v.findViewById(R.id.videoThumbnail); play = v.findViewById(R.id.playButton); vDur = v.findViewById(R.id.voiceDurationText); vPlay = v.findViewById(R.id.voicePlayPause); }
            void bind(Message m, int pos) {
                txt.setText(m.text); time.setText(formatTime(m.createdAt));
                media.setVisibility(View.GONE); imgCard.setVisibility(View.GONE); vidCard.setVisibility(View.GONE); voice.setVisibility(View.GONE);
                if (m.type.equals("voice")) { voice.setVisibility(View.VISIBLE); vDur.setText(formatVoiceDuration(m.duration)); vPlay.setOnClickListener(v -> toggleVoice(m.mediaPath, vPlay, pos)); }
                else if (m.type.equals("image")) { media.setVisibility(View.VISIBLE); imgCard.setVisibility(View.VISIBLE); String url = BASE_URL + "get_image.php?path=" + m.mediaPath + "&token=" + authToken; Glide.with(LoveDoctorChatActivity.this).load(url).into(img); img.setOnClickListener(v -> zoomImage(m.mediaPath)); }
                else if (m.type.equals("video")) { media.setVisibility(View.VISIBLE); vidCard.setVisibility(View.VISIBLE); String url = BASE_URL + "get_image.php?path=" + m.mediaPath + "&token=" + authToken; Glide.with(LoveDoctorChatActivity.this).load(url).into(vid); play.setOnClickListener(v -> playVideo(m.mediaPath)); }
            }
        }

        class ReceivedVH extends RecyclerView.ViewHolder {
            TextView txt, time, vDur, vPlay; LinearLayout media, voice; CardView imgCard, vidCard; ImageView img, vid, play;
            ReceivedVH(View v) { super(v); txt = v.findViewById(R.id.messageText); time = v.findViewById(R.id.timeText); media = v.findViewById(R.id.mediaContainer); voice = v.findViewById(R.id.voiceContainer); imgCard = v.findViewById(R.id.imageCard); vidCard = v.findViewById(R.id.videoCard); img = v.findViewById(R.id.messageImage); vid = v.findViewById(R.id.videoThumbnail); play = v.findViewById(R.id.playButton); vDur = v.findViewById(R.id.voiceDurationText); vPlay = v.findViewById(R.id.voicePlayPause); }
            void bind(Message m, int pos) {
                txt.setText(m.text); time.setText(formatTime(m.createdAt));
                media.setVisibility(View.GONE); imgCard.setVisibility(View.GONE); vidCard.setVisibility(View.GONE); voice.setVisibility(View.GONE);
                if (m.type.equals("voice")) { voice.setVisibility(View.VISIBLE); vDur.setText(formatVoiceDuration(m.duration)); vPlay.setOnClickListener(v -> toggleVoice(m.mediaPath, vPlay, pos)); }
                else if (m.type.equals("image")) { media.setVisibility(View.VISIBLE); imgCard.setVisibility(View.VISIBLE); String url = BASE_URL + "get_image.php?path=" + m.mediaPath + "&token=" + authToken; Glide.with(LoveDoctorChatActivity.this).load(url).into(img); img.setOnClickListener(v -> zoomImage(m.mediaPath)); }
                else if (m.type.equals("video")) { media.setVisibility(View.VISIBLE); vidCard.setVisibility(View.VISIBLE); String url = BASE_URL + "get_image.php?path=" + m.mediaPath + "&token=" + authToken; Glide.with(LoveDoctorChatActivity.this).load(url).into(vid); play.setOnClickListener(v -> playVideo(m.mediaPath)); }
            }
        }

        private void toggleVoice(String path, TextView btn, int pos) {
            String url = BASE_URL + "get_image.php?path=" + path + "&token=" + authToken;
            if (voicePlayer != null && currentPlayingPosition == pos) { if (voicePlayer.isPlaying()) { voicePlayer.pause(); btn.setText("▶"); } else { voicePlayer.start(); btn.setText("⏸"); } return; }
            if (voicePlayer != null) { voicePlayer.release(); voicePlayer = null; }
            try {
                voicePlayer = new MediaPlayer(); voicePlayer.setDataSource(url); voicePlayer.prepareAsync();
                voicePlayer.setOnPreparedListener(mp -> { mp.start(); btn.setText("⏸"); currentPlayingPosition = pos; });
                voicePlayer.setOnCompletionListener(mp -> { btn.setText("▶"); currentPlayingPosition = -1; });
            } catch (Exception e) {}
        }
        private void zoomImage(String path) {
            Dialog dialog = new Dialog(LoveDoctorChatActivity.this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            dialog.setContentView(R.layout.dialog_image_zoom);
            ImageView image = dialog.findViewById(R.id.zoomImageView);
            ImageView saveBtn = dialog.findViewById(R.id.saveImageView);
            String url = BASE_URL + "get_image.php?path=" + path + "&token=" + authToken;
            Glide.with(LoveDoctorChatActivity.this).load(url).into(image);
            if (saveBtn != null) saveBtn.setOnClickListener(v -> saveImageToDevice(url));
            image.setOnClickListener(v -> dialog.dismiss()); dialog.show();
        }
        private void saveImageToDevice(String imageUrl) {
            Toast.makeText(LoveDoctorChatActivity.this, "Saving...", Toast.LENGTH_SHORT).show();
            new Thread(() -> {
                try {
                    java.net.URL url = new java.net.URL(imageUrl);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setDoInput(true); conn.connect();
                    Bitmap bitmap = BitmapFactory.decodeStream(conn.getInputStream());
                    conn.disconnect();
                    if (bitmap != null) runOnUiThread(() -> saveBitmap(bitmap));
                } catch (Exception e) { runOnUiThread(() -> Toast.makeText(LoveDoctorChatActivity.this, "Save failed", Toast.LENGTH_SHORT).show()); }
            }).start();
        }
        private void saveBitmap(Bitmap bitmap) {
            try {
                ContentResolver res = getContentResolver(); ContentValues vals = new ContentValues();
                vals.put(MediaStore.MediaColumns.DISPLAY_NAME, "Soulmate_LD_" + System.currentTimeMillis() + ".jpg");
                vals.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                vals.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Soulmate");
                Uri uri = res.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, vals);
                if (uri != null) { OutputStream os = res.openOutputStream(uri); bitmap.compress(Bitmap.CompressFormat.JPEG, 90, os); os.close(); Toast.makeText(LoveDoctorChatActivity.this, "Saved to Gallery", Toast.LENGTH_SHORT).show(); }
            } catch (Exception e) { Toast.makeText(LoveDoctorChatActivity.this, "Save failed", Toast.LENGTH_SHORT).show(); }
        }
        private void playVideo(String path) {
            String url = BASE_URL + "get_image.php?path=" + path + "&token=" + authToken;
            Intent intent = new Intent(Intent.ACTION_VIEW); intent.setDataAndType(Uri.parse(url), "video/mp4"); startActivity(intent);
        }
        private String formatTime(String ts) {
            try {
                SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(in.parse(ts));
            } catch (Exception e) { return ""; }
        }
    }
}
