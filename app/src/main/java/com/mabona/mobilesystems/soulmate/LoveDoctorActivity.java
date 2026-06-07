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
import android.view.Gravity;
import android.view.LayoutInflater;

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
    private CardView searchCard;
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

        if (userId == -1 || authToken == null) {
            showPinkToast("Session expired");
            finish();
            return;
        }

        initializeViews();
        setupAnimations();
        setupListeners();
        loadAds();

        // Load chats initially
        loadChats();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        searchCard = findViewById(R.id.searchCard);
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
            searchBar.clearFocus();
            doctorsSection.setVisibility(View.GONE);
            if (!chatList.isEmpty()) {
                chatsSection.setVisibility(View.VISIBLE);
            }
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
                if (s.length() > 0) {
                    doctorsSection.setVisibility(View.VISIBLE);
                    chatsSection.setVisibility(View.GONE);
                    loadDoctors(s.toString());
                } else {
                    doctorsSection.setVisibility(View.GONE);
                    if (chatList != null && !chatList.isEmpty()) {
                        chatsSection.setVisibility(View.VISIBLE);
                    } else {
                        doctorsSection.setVisibility(View.VISIBLE);
                        loadDoctors("");
                    }
                }
                clearSearchIcon.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchBar.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                doctorsSection.setVisibility(View.VISIBLE);
                chatsSection.setVisibility(View.GONE);
                if (doctorList.isEmpty()) {
                    loadDoctors("");
                }
            }
        });
    }

    private void loadAds() {
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
                    showPinkToast("Network error");
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseBody = response.body().string();
                runOnUiThread(() -> {
                    showProgress(false);
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        if (json.optBoolean("success", false)) {
                            JSONArray doctorsArray = json.optJSONArray("doctors");
                            doctorList.clear();

                            if (doctorsArray != null) {
                                for (int i = 0; i < doctorsArray.length(); i++) {
                                    JSONObject doc = doctorsArray.getJSONObject(i);
                                    DoctorItem doctor = new DoctorItem(
                                            doc.optInt("doctor_id", 0),
                                            doc.optString("full_name", "Doctor"),
                                            doc.optString("profile_image", null),
                                            doc.optString("bio", "No bio available"),
                                            doc.optString("qualification_type", "Specialist"),
                                            (float)doc.optDouble("session_price", 0.0),
                                            (float)doc.optDouble("rating", 0.0)
                                    );
                                    doctorList.add(doctor);
                                }
                            }

                            doctorsRecyclerView.setVisibility(doctorList.isEmpty() ? View.GONE : View.VISIBLE);
                            emptyStateText.setVisibility(doctorList.isEmpty() ? View.VISIBLE : View.GONE);
                            if (doctorList.isEmpty()) {
                                emptyStateText.setText(searchQuery.isEmpty() ? "No Love Doctors available" : "No results for \"" + searchQuery + "\"");
                            }

                            doctorAdapter = new DoctorAdapter(doctorList);
                            doctorsRecyclerView.setAdapter(doctorAdapter);
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
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseBody = response.body().string();
                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        if (json.optBoolean("success", false)) {
                            JSONArray chatsArray = json.optJSONArray("chats");
                            chatList.clear();

                            if (chatsArray != null) {
                                for (int i = 0; i < chatsArray.length(); i++) {
                                    JSONObject chat = chatsArray.getJSONObject(i);
                                    chatList.add(new LoveDoctorChatItem(
                                            chat.optInt("session_id", 0),
                                            chat.optInt("doctor_id", 0),
                                            chat.optString("other_person_name", "Chat Session"),
                                            chat.optString("other_person_image", ""),
                                            chat.optString("last_message", "Click to continue"),
                                            chat.optString("time", "")
                                    ));
                                }
                            }

                            if (!chatList.isEmpty()) {
                                chatsSection.setVisibility(View.VISIBLE);
                                doctorsSection.setVisibility(View.GONE);
                                chatAdapter = new LoveDoctorChatAdapter(chatList);
                                chatsRecyclerView.setAdapter(chatAdapter);
                            } else {
                                chatsSection.setVisibility(View.GONE);
                                doctorsSection.setVisibility(View.VISIBLE);
                                loadDoctors("");
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

    private void startNewSession(int doctorId) {
        showProgress(true);

        OkHttpClient client = new OkHttpClient();
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("action", "start_session")
                .addFormDataPart("token", authToken)
                .addFormDataPart("doctor_id", String.valueOf(doctorId))
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
                    showPinkToast("Failed to start session");
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
                            openDoctorChat(doctorId, json.getInt("session_id"));
                        } else {
                            showPinkToast(json.optString("message", "Error"));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
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

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onBackPressed() {
        if (doctorsSection.getVisibility() == View.VISIBLE && !chatList.isEmpty()) {
            searchBar.setText("");
            searchBar.clearFocus();
            doctorsSection.setVisibility(View.GONE);
            chatsSection.setVisibility(View.VISIBLE);
        } else {
            super.onBackPressed();
        }
    }

    // ==================== INNER CLASSES ====================

    class DoctorItem {
        int doctorId;
        String fullName;
        String profileImage;
        String bio;
        String qualificationType;
        float sessionPrice;
        float rating;

        DoctorItem(int doctorId, String fullName, String profileImage, String bio, String qualificationType, float sessionPrice, float rating) {
            this.doctorId = doctorId;
            this.fullName = fullName;
            this.profileImage = profileImage;
            this.bio = bio;
            this.qualificationType = qualificationType;
            this.sessionPrice = sessionPrice;
            this.rating = rating;
        }
    }

    class DoctorAdapter extends RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder> {
        private List<DoctorItem> doctors;
        DoctorAdapter(List<DoctorItem> doctors) { this.doctors = doctors; }

        @NonNull
        @Override
        public DoctorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new DoctorViewHolder(getLayoutInflater().inflate(R.layout.item_love_doctor, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull DoctorViewHolder holder, int position) {
            DoctorItem doc = doctors.get(position);
            holder.nameText.setText(doc.fullName);
            holder.qualificationText.setText(doc.qualificationType);
            holder.priceText.setText("E" + doc.sessionPrice + "/session");
            holder.ratingText.setText("⭐ " + doc.rating);
            holder.bioText.setText(doc.bio.length() > 60 ? doc.bio.substring(0, 60) + "..." : doc.bio);

            if (doc.profileImage != null && !doc.profileImage.isEmpty()) {
                Glide.with(LoveDoctorActivity.this).load(BASE_URL + "get_image.php?path=" + doc.profileImage + "&token=" + authToken).placeholder(R.drawable.default_profile).into(holder.profileImage);
            }

            holder.chatButton.setOnClickListener(v -> startNewSession(doc.doctorId));
            holder.profileButton.setOnClickListener(v -> {
                Intent intent = new Intent(LoveDoctorActivity.this, DoctorProfileActivity.class);
                intent.putExtra("USER_ID", userId);
                intent.putExtra("AUTH_TOKEN", authToken);
                intent.putExtra("DOCTOR_ID", doc.doctorId);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() { return doctors.size(); }

        class DoctorViewHolder extends RecyclerView.ViewHolder {
            ImageView profileImage;
            TextView nameText, qualificationText, priceText, ratingText, bioText;
            CardView chatButton, profileButton;

            DoctorViewHolder(View v) {
                super(v);
                profileImage = v.findViewById(R.id.profileImage);
                nameText = v.findViewById(R.id.nameText);
                qualificationText = v.findViewById(R.id.qualificationText);
                priceText = v.findViewById(R.id.priceText);
                ratingText = v.findViewById(R.id.ratingText);
                bioText = v.findViewById(R.id.bioText);
                chatButton = v.findViewById(R.id.chatButton);
                profileButton = v.findViewById(R.id.profileButton);
            }
        }
    }

    class LoveDoctorChatItem {
        int sessionId, doctorId;
        String otherPersonName, otherPersonImage, lastMessage, time;
        LoveDoctorChatItem(int sId, int dId, String name, String img, String msg, String t) {
            this.sessionId = sId; this.doctorId = dId; this.otherPersonName = name;
            this.otherPersonImage = img; this.lastMessage = msg; this.time = t;
        }
    }

    class LoveDoctorChatAdapter extends RecyclerView.Adapter<LoveDoctorChatAdapter.ChatViewHolder> {
        private List<LoveDoctorChatItem> chats;
        LoveDoctorChatAdapter(List<LoveDoctorChatItem> chats) { this.chats = chats; }

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ChatViewHolder(getLayoutInflater().inflate(R.layout.item_love_doctor_chat, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            LoveDoctorChatItem chat = chats.get(position);
            holder.nameText.setText(chat.otherPersonName);
            holder.lastMessageText.setText(chat.lastMessage);
            holder.timeText.setText(chat.time);
            if (chat.otherPersonImage != null && !chat.otherPersonImage.isEmpty()) {
                Glide.with(LoveDoctorActivity.this).load(BASE_URL + "get_image.php?path=" + chat.otherPersonImage + "&token=" + authToken).placeholder(R.drawable.default_profile).into(holder.profileImage);
            }

            // Clicking the profile image takes you to the Doctor's Profile
            holder.profileImage.setOnClickListener(v -> {
                Intent intent = new Intent(LoveDoctorActivity.this, DoctorProfileActivity.class);
                intent.putExtra("USER_ID", userId);
                intent.putExtra("AUTH_TOKEN", authToken);
                intent.putExtra("DOCTOR_ID", chat.doctorId);
                startActivity(intent);
            });

            // Clicking the item (center/rest) takes you to the Chat
            holder.itemView.setOnClickListener(v -> openDoctorChat(chat.doctorId, chat.sessionId));
        }

        @Override
        public int getItemCount() { return chats.size(); }

        class ChatViewHolder extends RecyclerView.ViewHolder {
            ImageView profileImage;
            TextView nameText, lastMessageText, timeText;
            ChatViewHolder(View v) {
                super(v);
                profileImage = v.findViewById(R.id.profileImage);
                nameText = v.findViewById(R.id.nameText);
                lastMessageText = v.findViewById(R.id.lastMessageText);
                timeText = v.findViewById(R.id.timeText);
            }
        }
    }
}
