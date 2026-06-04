package com.mabona.mobilesystems.soulmate;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoveDoctorActivity extends AppCompatActivity {

    // UI Components
    private ImageView backButton;
    private EditText searchBar;
    private ImageView clearSearchIcon;
    private CardView requestDoctorButton;
    private RecyclerView doctorsRecyclerView;
    private RecyclerView chatsRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyStateText;
    private AdView bannerAdView;
    private LinearLayout doctorsSection;
    private LinearLayout chatsSection;
    private TextView doctorsSectionTitle;
    private TextView chatsSectionTitle;
    private ImageView heartImage1, heartImage2, heartImage3;

    // Data
    private int userId;
    private String authToken;
    private String userEmail;
    private String userName;
    private String sessionPin;
    private int existingSessionId;
    private int existingDoctorId;

    // Adapters
    private DoctorAdapter doctorAdapter;
    private LoveDoctorChatAdapter chatAdapter;
    private List<DoctorItem> doctorList = new ArrayList<>();
    private List<LoveDoctorChatItem> chatList = new ArrayList<>();

    // Constants
    private static final String BASE_URL = "https://mabona.firstsuninvestment.com/soulmate/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_love_doctor);

        // Get user data
        userId = getIntent().getIntExtra("USER_ID", -1);
        authToken = getIntent().getStringExtra("AUTH_TOKEN");
        userEmail = getIntent().getStringExtra("USER_EMAIL");
        userName = getIntent().getStringExtra("USER_NAME");
        sessionPin = getIntent().getStringExtra("SESSION_PIN");
        existingSessionId = getIntent().getIntExtra("SESSION_ID", -1);
        existingDoctorId = getIntent().getIntExtra("DOCTOR_ID", -1);

        if (userId == -1 || authToken == null) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupAnimations();
        setupListeners();
        setupSearch();
        loadAds();

        // Load doctors and chats
        loadDoctors("");
        loadChats();

        // If there's an existing session, open chat directly
        if (existingSessionId != -1 && existingDoctorId != -1) {
            openDoctorChat(existingDoctorId, existingSessionId);
        }
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        searchBar = findViewById(R.id.searchBar);
        clearSearchIcon = findViewById(R.id.clearSearchIcon);
        requestDoctorButton = findViewById(R.id.requestDoctorButton);
        doctorsRecyclerView = findViewById(R.id.doctorsRecyclerView);
        chatsRecyclerView = findViewById(R.id.chatsRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyStateText = findViewById(R.id.emptyStateText);
        bannerAdView = findViewById(R.id.bannerAdView);
        doctorsSection = findViewById(R.id.doctorsSection);
        chatsSection = findViewById(R.id.chatsSection);
        doctorsSectionTitle = findViewById(R.id.doctorsSectionTitle);
        chatsSectionTitle = findViewById(R.id.chatsSectionTitle);
        heartImage1 = findViewById(R.id.heartImage1);
        heartImage2 = findViewById(R.id.heartImage2);
        heartImage3 = findViewById(R.id.heartImage3);

        doctorsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        backButton.setOnClickListener(v -> finish());
        clearSearchIcon.setOnClickListener(v -> {
            searchBar.setText("");
            loadDoctors("");
        });

        requestDoctorButton.setOnClickListener(v -> {
            Animation bounce = AnimationUtils.loadAnimation(this, R.anim.bounce);
            requestDoctorButton.startAnimation(bounce);

            Intent intent = new Intent(LoveDoctorActivity.this, LoveDoctorRequestActivity.class);
            intent.putExtra("USER_ID", userId);
            intent.putExtra("AUTH_TOKEN", authToken);
            intent.putExtra("USER_EMAIL", userEmail);
            intent.putExtra("USER_NAME", userName);
            startActivity(intent);
        });
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
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadDoctors(s.toString());
                clearSearchIcon.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupSearch() {
        // Search is already set up via TextWatcher
    }

    private void loadAds() {
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
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
            }
        });
    }

    private void loadDoctors(String searchQuery) {
        showProgress(true);

        OkHttpClient client = new OkHttpClient();
        String url = BASE_URL + "dr.php?action=get_doctors&token=" + authToken + "&search=" + searchQuery;

        Request request = new Request.Builder().url(url).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(LoveDoctorActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                            JSONArray doctorsArray = json.getJSONArray("doctors");
                            doctorList.clear();

                            for (int i = 0; i < doctorsArray.length(); i++) {
                                JSONObject doc = doctorsArray.getJSONObject(i);
                                DoctorItem doctor = new DoctorItem(
                                        doc.getInt("doctor_id"),
                                        doc.getString("full_name"),
                                        doc.getString("gender"),
                                        doc.getString("bio"),
                                        doc.optString("profile_image", null),
                                        (float)doc.optDouble("rating", 0),
                                        doc.getInt("sessions_count"),
                                        doc.getInt("review_count"),
                                        doc.getString("qualification_type"),
                                        (float)doc.optDouble("session_price", 0),
                                        doc.getBoolean("is_online"),
                                        doc.getBoolean("has_active_session")
                                );
                                doctorList.add(doctor);
                            }

                            if (doctorList.isEmpty() && !searchQuery.isEmpty()) {
                                emptyStateText.setText("No love doctors found for \"" + searchQuery + "\"");
                                emptyStateText.setVisibility(View.VISIBLE);
                                doctorsRecyclerView.setVisibility(View.GONE);
                            } else if (doctorList.isEmpty()) {
                                emptyStateText.setText("💕 No love doctors available yet.\nBe the first to request!");
                                emptyStateText.setVisibility(View.VISIBLE);
                                doctorsRecyclerView.setVisibility(View.GONE);
                            } else {
                                emptyStateText.setVisibility(View.GONE);
                                doctorsRecyclerView.setVisibility(View.VISIBLE);
                                doctorAdapter = new DoctorAdapter(doctorList);
                                doctorsRecyclerView.setAdapter(doctorAdapter);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    private void loadChats() {
        OkHttpClient client = new OkHttpClient();
        String url = BASE_URL + "dr.php?action=get_chats&token=" + authToken;

        Request request = new Request.Builder().url(url).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Silent fail for chats
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body().string();
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        if (json.getBoolean("success")) {
                            JSONArray chatsArray = json.getJSONArray("chats");
                            chatList.clear();

                            for (int i = 0; i < chatsArray.length(); i++) {
                                JSONObject chat = chatsArray.getJSONObject(i);
                                LoveDoctorChatItem item = new LoveDoctorChatItem(
                                        chat.getInt("session_id"),
                                        chat.getInt("doctor_id"),
                                        chat.getString("other_person_name"),
                                        chat.getString("other_person_image"),
                                        chat.getString("last_message"),
                                        chat.getString("time")
                                );
                                chatList.add(item);
                            }

                            if (!chatList.isEmpty()) {
                                chatsSection.setVisibility(View.VISIBLE);
                                chatAdapter = new LoveDoctorChatAdapter(chatList);
                                chatsRecyclerView.setAdapter(chatAdapter);
                            } else {
                                chatsSection.setVisibility(View.GONE);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    private void openDoctorChat(int doctorId, int sessionId) {
        Intent intent = new Intent(LoveDoctorActivity.this, LoveDoctorChatActivity.class);
        intent.putExtra("USER_ID", userId);
        intent.putExtra("AUTH_TOKEN", authToken);
        intent.putExtra("USER_EMAIL", userEmail);
        intent.putExtra("USER_NAME", userName);
        intent.putExtra("DOCTOR_ID", doctorId);
        intent.putExtra("SESSION_ID", sessionId);
        intent.putExtra("SESSION_PIN", sessionPin);
        startActivity(intent);
    }

    private void startNewSession(int doctorId, String pin) {
        showProgress(true);

        OkHttpClient client = new OkHttpClient();
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("action", "start_session")
                .addFormDataPart("token", authToken)
                .addFormDataPart("doctor_id", String.valueOf(doctorId))
                .addFormDataPart("session_pin", pin)
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + "dr.php")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(LoveDoctorActivity.this, "Failed to start session: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                            int sessionId = json.getInt("session_id");
                            openDoctorChat(doctorId, sessionId);
                        } else {
                            Toast.makeText(LoveDoctorActivity.this, json.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(LoveDoctorActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // ==================== INNER CLASSES ====================

    class DoctorItem {
        int doctorId;
        String fullName;
        String gender;
        String bio;
        String profileImage;
        float rating;
        int sessionsCount;
        int reviewCount;
        String qualificationType;
        float sessionPrice;
        boolean isOnline;
        boolean hasActiveSession;

        DoctorItem(int doctorId, String fullName, String gender, String bio, String profileImage,
                   float rating, int sessionsCount, int reviewCount, String qualificationType,
                   float sessionPrice, boolean isOnline, boolean hasActiveSession) {
            this.doctorId = doctorId;
            this.fullName = fullName;
            this.gender = gender;
            this.bio = bio;
            this.profileImage = profileImage;
            this.rating = rating;
            this.sessionsCount = sessionsCount;
            this.reviewCount = reviewCount;
            this.qualificationType = qualificationType;
            this.sessionPrice = sessionPrice;
            this.isOnline = isOnline;
            this.hasActiveSession = hasActiveSession;
        }
    }

    class DoctorAdapter extends RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder> {
        private List<DoctorItem> doctors;

        DoctorAdapter(List<DoctorItem> doctors) {
            this.doctors = doctors;
        }

        @NonNull
        @Override
        public DoctorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_love_doctor, parent, false);
            return new DoctorViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DoctorViewHolder holder, int position) {
            DoctorItem doctor = doctors.get(position);
            holder.bind(doctor);
        }

        @Override
        public int getItemCount() {
            return doctors.size();
        }

        class DoctorViewHolder extends RecyclerView.ViewHolder {
            ImageView profileImage, onlineIndicator;
            TextView nameText, qualificationText, ratingText, sessionsText, bioText, priceText;
            CardView chatButton, profileButton;

            DoctorViewHolder(@NonNull View itemView) {
                super(itemView);
                profileImage = itemView.findViewById(R.id.profileImage);
                onlineIndicator = itemView.findViewById(R.id.onlineIndicator);
                nameText = itemView.findViewById(R.id.nameText);
                qualificationText = itemView.findViewById(R.id.qualificationText);
                ratingText = itemView.findViewById(R.id.ratingText);
                sessionsText = itemView.findViewById(R.id.sessionsText);
                bioText = itemView.findViewById(R.id.bioText);
                priceText = itemView.findViewById(R.id.priceText);
                chatButton = itemView.findViewById(R.id.chatButton);
                profileButton = itemView.findViewById(R.id.profileButton);
            }

            void bind(DoctorItem doctor) {
                nameText.setText(doctor.fullName);
                qualificationText.setText(doctor.qualificationType);
                ratingText.setText("⭐ " + doctor.rating);
                sessionsText.setText(doctor.sessionsCount + " sessions");
                bioText.setText(doctor.bio.length() > 100 ? doctor.bio.substring(0, 100) + "..." : doctor.bio);
                priceText.setText("E" + doctor.sessionPrice + "/session");
                onlineIndicator.setVisibility(doctor.isOnline ? View.VISIBLE : View.GONE);

                if (doctor.profileImage != null && !doctor.profileImage.isEmpty()) {
                    String imageUrl = BASE_URL + "get_image.php?path=" + doctor.profileImage + "&token=" + authToken;
                    Glide.with(LoveDoctorActivity.this)
                            .load(imageUrl)
                            .placeholder(R.drawable.default_profile)
                            .error(R.drawable.default_profile)
                            .into(profileImage);
                }

                chatButton.setOnClickListener(v -> {
                    if (doctor.hasActiveSession) {
                        Toast.makeText(LoveDoctorActivity.this, "You already have an active session with this doctor", Toast.LENGTH_LONG).show();
                    } else {
                        showPinDialog(doctor.doctorId);
                    }
                });

                profileButton.setOnClickListener(v -> {
                    Intent intent = new Intent(LoveDoctorActivity.this, DoctorProfileActivity.class);
                    intent.putExtra("USER_ID", userId);
                    intent.putExtra("AUTH_TOKEN", authToken);
                    intent.putExtra("USER_EMAIL", userEmail);
                    intent.putExtra("USER_NAME", userName);
                    intent.putExtra("DOCTOR_ID", doctor.doctorId);
                    startActivity(intent);
                });
            }
        }
    }

    class LoveDoctorChatItem {
        int sessionId;
        int doctorId;
        String otherPersonName;
        String otherPersonImage;
        String lastMessage;
        String time;

        LoveDoctorChatItem(int sessionId, int doctorId, String otherPersonName,
                           String otherPersonImage, String lastMessage, String time) {
            this.sessionId = sessionId;
            this.doctorId = doctorId;
            this.otherPersonName = otherPersonName;
            this.otherPersonImage = otherPersonImage;
            this.lastMessage = lastMessage;
            this.time = time;
        }
    }

    class LoveDoctorChatAdapter extends RecyclerView.Adapter<LoveDoctorChatAdapter.ChatViewHolder> {
        private List<LoveDoctorChatItem> chats;

        LoveDoctorChatAdapter(List<LoveDoctorChatItem> chats) {
            this.chats = chats;
        }

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_love_doctor_chat, parent, false);
            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            LoveDoctorChatItem chat = chats.get(position);
            holder.nameText.setText(chat.otherPersonName);
            holder.lastMessageText.setText(chat.lastMessage);
            holder.timeText.setText(chat.time);

            if (chat.otherPersonImage != null && !chat.otherPersonImage.isEmpty()) {
                String imageUrl = BASE_URL + "get_image.php?path=" + chat.otherPersonImage + "&token=" + authToken;
                Glide.with(LoveDoctorActivity.this)
                        .load(imageUrl)
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .into(holder.profileImage);
            }

            holder.itemView.setOnClickListener(v -> {
                openDoctorChat(chat.doctorId, chat.sessionId);
            });
        }

        @Override
        public int getItemCount() {
            return chats.size();
        }

        class ChatViewHolder extends RecyclerView.ViewHolder {
            ImageView profileImage;
            TextView nameText, lastMessageText, timeText;

            ChatViewHolder(@NonNull View itemView) {
                super(itemView);
                profileImage = itemView.findViewById(R.id.profileImage);
                nameText = itemView.findViewById(R.id.nameText);
                lastMessageText = itemView.findViewById(R.id.lastMessageText);
                timeText = itemView.findViewById(R.id.timeText);
            }
        }
    }

    private void showPinDialog(int doctorId) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_therapy_pin);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        EditText pinInput = dialog.findViewById(R.id.pinInput);
        CardView confirmButton = dialog.findViewById(R.id.confirmButton);
        CardView cancelButton = dialog.findViewById(R.id.cancelButton);

        confirmButton.setOnClickListener(v -> {
            String pin = pinInput.getText().toString().trim();
            if (pin.length() < 4) {
                Toast.makeText(this, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            startNewSession(doctorId, pin);
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}