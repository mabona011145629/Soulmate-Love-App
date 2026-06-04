package com.mabona.mobilesystems.soulmate;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProfileViewActivity extends AppCompatActivity {

    private static final String BASE_URL = "https://mabona.firstsuninvestment.com/soulmate/";

    private int viewerId;
    private int profileUserId;
    private String authToken;
    private String userEmail;
    private String userName;
    private boolean isOwner;
    private boolean isProfileHidden;
    private String profileFullName;

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private AdView bannerAdView;
    private ImageView backButton;
    private ImageView editButton;
    private TextView titleText;
    private ImageView heartImage1, heartImage2, heartImage3;

    private ProfileAdapter adapter;
    private List<Object> displayItems = new ArrayList<>();
    private JSONObject profileData;
    private List<Post> postsList = new ArrayList<>();

    private OkHttpClient client;
    private Handler handler = new Handler();
    private boolean isLoading = false;
    private boolean hasMore = true;
    private int currentOffset = 0;
    private static final int PAGE_SIZE = 10;
    private static final int AD_INTERVAL = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_view);

        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        // Get intent data
        viewerId = getIntent().getIntExtra("USER_ID", -1);
        authToken = getIntent().getStringExtra("AUTH_TOKEN");
        userEmail = getIntent().getStringExtra("USER_EMAIL");
        userName = getIntent().getStringExtra("USER_NAME");
        profileUserId = getIntent().getIntExtra("PROFILE_USER_ID", -1);
        profileFullName = getIntent().getStringExtra("PROFILE_USER_NAME");

        if (viewerId == -1 || authToken == null || profileUserId == -1) {
            Toast.makeText(this, "Invalid profile data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupAnimations();
        setupRecyclerView();
        setupSwipeRefresh();
        loadAds();
        loadProfile();
    }

    private void initializeViews() {
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        recyclerView = findViewById(R.id.profileRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        bannerAdView = findViewById(R.id.bannerAdView);
        backButton = findViewById(R.id.backButton);

        titleText = findViewById(R.id.titleText);
        heartImage1 = findViewById(R.id.heartImage1);
        heartImage2 = findViewById(R.id.heartImage2);
        heartImage3 = findViewById(R.id.heartImage3);

        backButton.setOnClickListener(v -> finish());
        editButton.setOnClickListener(v -> showEditOptionsDialog());
    }

    private void setupAnimations() {
        // Floating hearts animation
        animateHeart(heartImage1, -20f, 20f, 2000);
        animateHeart(heartImage2, 20f, -20f, 2200);
        animateHeartX(heartImage3, -15f, 15f, 1800);
    }

    private void animateHeart(ImageView heart, float fromY, float toY, int duration) {
        heart.animate()
                .translationYBy(fromY)
                .setDuration(duration)
                .withEndAction(() -> heart.animate().translationYBy(toY).setDuration(duration).withEndAction(() -> animateHeart(heart, fromY, toY, duration)).start())
                .start();
    }

    private void animateHeartX(ImageView heart, float fromX, float toX, int duration) {
        heart.animate()
                .translationXBy(fromX)
                .setDuration(duration)
                .withEndAction(() -> heart.animate().translationXBy(toX).setDuration(duration).withEndAction(() -> animateHeartX(heart, fromX, toX, duration)).start())
                .start();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProfileAdapter();
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (!isLoading && hasMore && layoutManager != null &&
                        layoutManager.findLastCompletelyVisibleItemPosition() >= adapter.getItemCount() - 3) {
                    loadMorePosts();
                }
            }
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeColors(getColor(R.color.pink), getColor(R.color.purple));
        swipeRefreshLayout.setOnRefreshListener(this::refreshProfile);
    }

    private void refreshProfile() {
        postsList.clear();
        currentOffset = 0;
        hasMore = true;
        loadProfile();
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

    private void loadProfile() {
        showProgress(true);
        swipeRefreshLayout.setRefreshing(true);

        String url = BASE_URL + "profile_posts.php?viewer_id=" + viewerId + "&profile_user_id=" + profileUserId +
                "&token=" + authToken + "&limit=" + PAGE_SIZE + "&offset=" + currentOffset;

        Request request = new Request.Builder().url(url).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    swipeRefreshLayout.setRefreshing(false);
                    isLoading = false;
                    Toast.makeText(ProfileViewActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body().string();
                runOnUiThread(() -> {
                    showProgress(false);
                    swipeRefreshLayout.setRefreshing(false);
                    isLoading = false;

                    try {
                        JSONObject json = new JSONObject(responseBody);
                        if (json.getBoolean("success")) {
                            isOwner = json.getBoolean("is_owner");
                            isProfileHidden = json.getBoolean("is_hidden");
                            profileData = json.getJSONObject("profile");
                            hasMore = json.getBoolean("has_more");

                            // Set title
                            String profileName = profileData.optString("full_name", "Profile");
                            titleText.setText(profileName);

                            // Show edit button if owner
                            if (isOwner) {
                                editButton.setVisibility(View.VISIBLE);
                            } else {
                                editButton.setVisibility(View.GONE);
                            }

                            // Parse posts
                            JSONArray postsArray = json.getJSONArray("posts");
                            List<Post> newPosts = new ArrayList<>();
                            for (int i = 0; i < postsArray.length(); i++) {
                                JSONObject postJson = postsArray.getJSONObject(i);
                                Post post = Post.fromJson(postJson, isOwner);
                                newPosts.add(post);
                            }

                            if (currentOffset == 0) {
                                postsList.clear();
                            }
                            postsList.addAll(newPosts);
                            currentOffset += postsArray.length();

                            updateDisplayItems();
                            adapter.notifyDataSetChanged();

                        } else {
                            Toast.makeText(ProfileViewActivity.this, json.optString("message", "Failed to load profile"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(ProfileViewActivity.this, "Error parsing data", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void loadMorePosts() {
        if (hasMore && !isLoading) {
            loadProfile();
        }
    }

    private void updateDisplayItems() {
        displayItems.clear();
        // Add profile header as a special item
        displayItems.add("HEADER");
        // Add posts
        for (int i = 0; i < postsList.size(); i++) {
            displayItems.add(postsList.get(i));
            // Add ad after every AD_INTERVAL posts
            if ((i + 1) % AD_INTERVAL == 0 && (i + 1) < postsList.size()) {
                displayItems.add("AD");
            }
        }
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showEditOptionsDialog() {
        String[] options = {"Edit Profile", "Change Password", "Delete a Post", "Settings"};
        new AlertDialog.Builder(this)
                .setTitle("Profile Options")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            Toast.makeText(this, "Edit Profile coming soon", Toast.LENGTH_SHORT).show();
                            break;
                        case 1:
                            Toast.makeText(this, "Change Password coming soon", Toast.LENGTH_SHORT).show();
                            break;
                        case 2:
                            showDeletePostDialog();
                            break;
                        case 3:
                            Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show();
                            break;
                    }
                })
                .show();
    }

    private void showDeletePostDialog() {
        String[] postTitles = new String[postsList.size()];
        for (int i = 0; i < postsList.size(); i++) {
            String content = postsList.get(i).content;
            postTitles[i] = content.length() > 50 ? content.substring(0, 50) + "..." : content;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete a Post")
                .setItems(postTitles, (dialog, which) -> {
                    deletePost(postsList.get(which).postId);
                })
                .show();
    }

    private void deletePost(int postId) {
        showProgress(true);

        JSONObject json = new JSONObject();
        try {
            json.put("post_id", postId);
            json.put("user_id", viewerId);
            json.put("token", authToken);
        } catch (Exception e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json.toString());
        Request request = new Request.Builder()
                .url(BASE_URL + "delete_post.php")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(ProfileViewActivity.this, "Failed to delete post", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> {
                    showProgress(false);
                    refreshProfile();
                    Toast.makeText(ProfileViewActivity.this, "Post deleted", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // Post Model Class
    static class Post {
        int postId;
        int userId;
        String content;
        String linkUrl;
        List<MediaItem> media;
        int likesCount;
        int commentsCount;
        boolean userLiked;
        String userReaction;
        JSONObject reactions;
        List<Comment> comments;
        String createdAt;
        String timeAgo;
        boolean isOwner;

        static Post fromJson(JSONObject json, boolean isOwner) throws Exception {
            Post post = new Post();
            post.postId = json.getInt("post_id");
            post.userId = json.getInt("user_id");
            post.content = json.getString("content");
            post.linkUrl = json.optString("link_url", "");
            post.likesCount = json.getInt("likes_count");
            post.commentsCount = json.getInt("comments_count");
            post.userLiked = json.getInt("user_liked") == 1;
            post.userReaction = json.optString("user_reaction", "");
            post.reactions = json.optJSONObject("reactions");
            post.createdAt = json.getString("created_at");
            post.timeAgo = json.getString("time_ago");
            post.isOwner = isOwner;

            // Parse media
            post.media = new ArrayList<>();
            JSONArray mediaArray = json.getJSONArray("media");
            for (int i = 0; i < mediaArray.length(); i++) {
                JSONObject mediaJson = mediaArray.getJSONObject(i);
                MediaItem media = new MediaItem();
                media.id = mediaJson.getInt("id");
                media.path = mediaJson.getString("path");
                media.type = mediaJson.getString("type");
                post.media.add(media);
            }

            // Parse comments
            post.comments = new ArrayList<>();
            JSONArray commentsArray = json.getJSONArray("comments");
            for (int i = 0; i < commentsArray.length(); i++) {
                JSONObject commentJson = commentsArray.getJSONObject(i);
                Comment comment = new Comment();
                comment.id = commentJson.getInt("id");
                comment.text = commentJson.getString("text");
                comment.userName = commentJson.getString("user_name");
                comment.userId = commentJson.getInt("user_id");
                comment.createdAt = commentJson.getString("created_at");
                post.comments.add(comment);
            }

            return post;
        }
    }

    static class MediaItem {
        int id;
        String path;
        String type;
    }

    static class Comment {
        int id;
        int userId;
        String text;
        String userName;
        String createdAt;
    }

    // Profile Adapter
    class ProfileAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_POST = 1;
        private static final int TYPE_AD = 2;

        @Override
        public int getItemViewType(int position) {
            Object item = displayItems.get(position);
            if (item instanceof String && item.equals("HEADER")) {
                return TYPE_HEADER;
            } else if (item instanceof String && item.equals("AD")) {
                return TYPE_AD;
            } else {
                return TYPE_POST;
            }
        }

        @Override
        public int getItemCount() {
            return displayItems.size();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_HEADER) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_profile_header, parent, false);
                return new HeaderViewHolder(view);
            } else if (viewType == TYPE_AD) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ad, parent, false);
                return new AdViewHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
                return new PostViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof HeaderViewHolder) {
                ((HeaderViewHolder) holder).bind();
            } else if (holder instanceof PostViewHolder) {
                Post post = (Post) displayItems.get(position);
                ((PostViewHolder) holder).bind(post);
            }
        }

        class HeaderViewHolder extends RecyclerView.ViewHolder {
            CardView profileImageCard;
            ImageView profileImageView, onlineIndicator;
            TextView nameTextView, genderTextView, loveDoctorBadge, ratingText, sessionsCountText;
            TextView bioTextView, phoneTextView, locationTextView, noPostsText;
            LinearLayout ratingLayout, actionButtonsLayout;
            CardView chatButton, requestLoveButton;

            HeaderViewHolder(View itemView) {
                super(itemView);
                profileImageCard = itemView.findViewById(R.id.profileImageCard);
                profileImageView = itemView.findViewById(R.id.profileImageView);
                onlineIndicator = itemView.findViewById(R.id.onlineIndicator);
                nameTextView = itemView.findViewById(R.id.nameTextView);
                genderTextView = itemView.findViewById(R.id.genderTextView);
                loveDoctorBadge = itemView.findViewById(R.id.loveDoctorBadge);
                ratingText = itemView.findViewById(R.id.ratingText);
                sessionsCountText = itemView.findViewById(R.id.sessionsCountText);
                bioTextView = itemView.findViewById(R.id.bioTextView);
                phoneTextView = itemView.findViewById(R.id.phoneTextView);
                locationTextView = itemView.findViewById(R.id.locationTextView);
                noPostsText = itemView.findViewById(R.id.noPostsText);
                ratingLayout = itemView.findViewById(R.id.ratingLayout);
                actionButtonsLayout = itemView.findViewById(R.id.actionButtonsLayout);
                chatButton = itemView.findViewById(R.id.chatButton);
                requestLoveButton = itemView.findViewById(R.id.requestLoveButton);
            }

            void bind() {
                if (profileData == null) return;

                try {
                    String fullName = profileData.optString("full_name", "User");
                    String gender = profileData.optString("gender", "Not specified");
                    String bio = profileData.optString("bio", "No bio provided");
                    String phone = profileData.optString("phone_number", null);
                    String location = profileData.optString("location_name", null);
                    String country = profileData.optString("country", null);
                    boolean isLoveDoctor = profileData.optBoolean("is_love_doctor", false);
                    double rating = profileData.optDouble("love_doctor_rating", 0);
                    int sessions = profileData.optInt("love_doctor_sessions", 0);
                    boolean isOnline = profileData.optBoolean("is_online", false);
                    String profileImage = profileData.optString("profile_image", null);
                    boolean isHidden = profileData.optBoolean("is_hidden", false);

                    nameTextView.setText(fullName);

                    if (!isHidden) {
                        genderTextView.setText(gender);
                        bioTextView.setText(bio);

                        if (phone != null && !phone.isEmpty() && isOwner) {
                            phoneTextView.setText("📱 " + phone);
                            phoneTextView.setVisibility(View.VISIBLE);
                        } else {
                            phoneTextView.setVisibility(View.GONE);
                        }

                        String locationText = "";
                        if (location != null && !location.isEmpty()) locationText += location;
                        if (country != null && !country.isEmpty()) {
                            if (!locationText.isEmpty()) locationText += ", ";
                            locationText += country;
                        }
                        if (!locationText.isEmpty()) {
                            locationTextView.setText("📍 " + locationText);
                            locationTextView.setVisibility(View.VISIBLE);
                        } else {
                            locationTextView.setVisibility(View.GONE);
                        }

                        if (isLoveDoctor) {
                            loveDoctorBadge.setVisibility(View.VISIBLE);
                            ratingLayout.setVisibility(View.VISIBLE);
                            ratingText.setText("⭐ " + rating);
                            sessionsCountText.setText("(" + sessions + " sessions)");
                        }

                        onlineIndicator.setVisibility(isOnline ? View.VISIBLE : View.GONE);

                        // Show action buttons for non-owner, non-hidden profiles
                        if (!isOwner && !isHidden) {
                            actionButtonsLayout.setVisibility(View.VISIBLE);
                            chatButton.setOnClickListener(v -> openChat());
                            requestLoveButton.setOnClickListener(v -> sendLoveRequest());
                        }
                    } else {
                        genderTextView.setText("Private");
                        bioTextView.setText("This profile is private");
                        phoneTextView.setVisibility(View.GONE);
                        locationTextView.setVisibility(View.GONE);
                        actionButtonsLayout.setVisibility(View.GONE);
                        onlineIndicator.setVisibility(View.GONE);
                    }

                    // Load profile image
                    if (profileImage != null && !profileImage.isEmpty() && !profileImage.equals("default_profile") && !isHidden) {
                        String imageUrl = BASE_URL + "get_image.php?path=" + profileImage + "&token=" + authToken;
                        Glide.with(ProfileViewActivity.this)
                                .load(imageUrl)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .placeholder(R.drawable.default_profile)
                                .error(R.drawable.default_profile)
                                .into(profileImageView);
                    } else {
                        profileImageView.setImageResource(R.drawable.default_profile);
                    }

                    profileImageView.setOnClickListener(v -> {
                        if (!isHidden && profileImage != null && !profileImage.equals("default_profile")) {
                            zoomProfileImage(profileImage);
                        }
                    });

                    if (postsList.isEmpty()) {
                        noPostsText.setVisibility(View.VISIBLE);
                    } else {
                        noPostsText.setVisibility(View.GONE);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            private void openChat() {
                Intent intent = new Intent(ProfileViewActivity.this, ChatActivity.class);
                intent.putExtra("OTHER_USER_ID", profileUserId);
                intent.putExtra("OTHER_USER_NAME", profileFullName);
                intent.putExtra("USER_ID", viewerId);
                intent.putExtra("AUTH_TOKEN", authToken);
                intent.putExtra("USER_EMAIL", userEmail);
                intent.putExtra("USER_NAME", userName);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }

            private void sendLoveRequest() {
                // Implement love request
                Toast.makeText(ProfileViewActivity.this, "Love request sent to " + profileFullName, Toast.LENGTH_SHORT).show();
            }

            private void zoomProfileImage(String path) {
                Dialog zoomDialog = new Dialog(ProfileViewActivity.this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                zoomDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                zoomDialog.setContentView(R.layout.dialog_image_zoom);
                zoomDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));

                ImageView zoomImageView = zoomDialog.findViewById(R.id.zoomImageView);
                ImageView saveImageView = zoomDialog.findViewById(R.id.saveImageView);

                String imageUrl = BASE_URL + "get_image.php?path=" + path + "&token=" + authToken;
                Glide.with(ProfileViewActivity.this).load(imageUrl).into(zoomImageView);

                if (saveImageView != null) {
                    saveImageView.setVisibility(View.GONE);
                }

                zoomImageView.setOnClickListener(v -> zoomDialog.dismiss());
                zoomDialog.show();
            }
        }

        class PostViewHolder extends RecyclerView.ViewHolder {
            // Same as in PostsActivity - reuse the same layout
            ImageView profileImageView;
            TextView userNameText, timeAgoText, postContentText, likeCountText, commentCountText;
            ImageView likeIcon, chatButton;
            LinearLayout likeButton, commentButton, shareButton, mediaContainer;
            CardView singleImageCard, linkPreviewCard, videoCard;
            ImageView singleImageView, videoThumbnail, playButton;
            TextView linkPreviewText, readMoreButton;
            ImageView deleteButton;
            LinearLayout reactionsSummary, reactionIconsContainer;
            TextView reactionCountText;
            LinearLayout commentsPreview;
            TextView comment1Text, comment2Text, viewAllCommentsText;

            PostViewHolder(View itemView) {
                super(itemView);
                profileImageView = itemView.findViewById(R.id.profileImageView);
                userNameText = itemView.findViewById(R.id.userNameText);
                timeAgoText = itemView.findViewById(R.id.timeAgoText);
                postContentText = itemView.findViewById(R.id.postContentText);
                likeCountText = itemView.findViewById(R.id.likeCountText);
                commentCountText = itemView.findViewById(R.id.commentCountText);
                likeIcon = itemView.findViewById(R.id.likeIcon);
                likeButton = itemView.findViewById(R.id.likeButton);
                commentButton = itemView.findViewById(R.id.commentButton);
                shareButton = itemView.findViewById(R.id.shareButton);
                mediaContainer = itemView.findViewById(R.id.mediaContainer);
                singleImageCard = itemView.findViewById(R.id.singleImageCard);
                singleImageView = itemView.findViewById(R.id.singleImageView);
                linkPreviewCard = itemView.findViewById(R.id.linkPreviewCard);
                linkPreviewText = itemView.findViewById(R.id.linkPreviewText);
                videoCard = itemView.findViewById(R.id.videoCard);
                videoThumbnail = itemView.findViewById(R.id.videoThumbnail);
                playButton = itemView.findViewById(R.id.playButton);
                readMoreButton = itemView.findViewById(R.id.readMoreButton);
                chatButton = itemView.findViewById(R.id.chatButton);
                reactionsSummary = itemView.findViewById(R.id.reactionsSummary);
                reactionIconsContainer = itemView.findViewById(R.id.reactionIconsContainer);
                reactionCountText = itemView.findViewById(R.id.reactionCountText);
                commentsPreview = itemView.findViewById(R.id.commentsPreview);
                comment1Text = itemView.findViewById(R.id.comment1Text);
                comment2Text = itemView.findViewById(R.id.comment2Text);
                viewAllCommentsText = itemView.findViewById(R.id.viewAllCommentsText);

                // Add delete button for owner
                deleteButton = new ImageView(itemView.getContext());
                // Will be added dynamically
            }

            void bind(Post post) {
                // Hide chat button in profile view (they can use header chat button)
                if (chatButton != null) chatButton.setVisibility(View.GONE);

                userNameText.setText(profileFullName != null ? profileFullName : "User");
                timeAgoText.setText(post.timeAgo);
                profileImageView.setVisibility(View.GONE); // Hide profile image in posts (already in header)

                // Set content
                if (post.content != null && !post.content.isEmpty()) {
                    if (post.content.length() > 200) {
                        String shortContent = post.content.substring(0, 200) + "...";
                        postContentText.setText(shortContent);
                        readMoreButton.setVisibility(View.VISIBLE);
                        readMoreButton.setOnClickListener(v -> {
                            postContentText.setText(post.content);
                            readMoreButton.setVisibility(View.GONE);
                        });
                    } else {
                        postContentText.setText(post.content);
                        readMoreButton.setVisibility(View.GONE);
                    }
                    postContentText.setVisibility(View.VISIBLE);
                } else {
                    postContentText.setVisibility(View.GONE);
                }

                // Set link preview
                if (post.linkUrl != null && !post.linkUrl.isEmpty()) {
                    linkPreviewText.setText(post.linkUrl);
                    linkPreviewCard.setVisibility(View.VISIBLE);
                    linkPreviewCard.setOnClickListener(v -> {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(post.linkUrl));
                        startActivity(intent);
                    });
                } else {
                    linkPreviewCard.setVisibility(View.GONE);
                }

                // Set media
                if (post.media != null && !post.media.isEmpty()) {
                    mediaContainer.setVisibility(View.VISIBLE);
                    // Simplified media setup - similar to PostsActivity
                } else {
                    mediaContainer.setVisibility(View.GONE);
                }

                // Set like state
                if (post.userLiked) {
                    likeIcon.setImageResource(R.drawable.ic_heart_filled);
                    likeIcon.setColorFilter(getColor(R.color.red));
                } else {
                    likeIcon.setImageResource(R.drawable.ic_heart_outline);
                    likeIcon.setColorFilter(getColor(R.color.medium_gray));
                }
                likeCountText.setText(String.valueOf(post.likesCount));
                commentCountText.setText(String.valueOf(post.commentsCount));

                // Setup click listeners
                likeButton.setOnClickListener(v -> sendReaction(post, "like"));
                commentButton.setOnClickListener(v -> showCommentDialog(post));
                shareButton.setOnClickListener(v -> sharePost(post));

                // Show delete button for owner
                if (isOwner) {
                    // Add delete button to the post item
                    ViewGroup parent = (ViewGroup) itemView;
                    // Implementation for delete button
                }

                // Show reactions
                if (post.likesCount > 0 && post.reactions != null) {
                    reactionsSummary.setVisibility(View.VISIBLE);
                    // Populate reactions
                } else {
                    reactionsSummary.setVisibility(View.GONE);
                }

                // Show comments preview
                if (post.comments != null && !post.comments.isEmpty()) {
                    commentsPreview.setVisibility(View.VISIBLE);
                    Comment comment1 = post.comments.get(0);
                    SpannableString comment1Span = new SpannableString(comment1.userName + ": " + comment1.text);
                    comment1Span.setSpan(new ForegroundColorSpan(getColor(R.color.pink_dark)), 0, comment1.userName.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    comment1Text.setText(comment1Span);

                    if (post.comments.size() >= 2) {
                        Comment comment2 = post.comments.get(1);
                        comment2Text.setVisibility(View.VISIBLE);
                        SpannableString comment2Span = new SpannableString(comment2.userName + ": " + comment2.text);
                        comment2Span.setSpan(new ForegroundColorSpan(getColor(R.color.pink_dark)), 0, comment2.userName.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        comment2Text.setText(comment2Span);
                    } else {
                        comment2Text.setVisibility(View.GONE);
                    }

                    if (post.commentsCount > 2) {
                        viewAllCommentsText.setVisibility(View.VISIBLE);
                        viewAllCommentsText.setText("View all " + post.commentsCount + " comments");
                    } else {
                        viewAllCommentsText.setVisibility(View.GONE);
                    }
                } else {
                    commentsPreview.setVisibility(View.GONE);
                }
            }

            private void sendReaction(Post post, String reactionType) {
                JSONObject json = new JSONObject();
                try {
                    json.put("post_id", post.postId);
                    json.put("user_id", viewerId);
                    json.put("token", authToken);
                    json.put("reaction_type", reactionType);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                RequestBody body = RequestBody.create(MediaType.parse("application/json"), json.toString());
                Request request = new Request.Builder()
                        .url(BASE_URL + "reactions.php")
                        .post(body)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {}

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        runOnUiThread(() -> refreshProfile());
                    }
                });
            }

            private void showCommentDialog(Post post) {
                Dialog commentDialog = new Dialog(ProfileViewActivity.this);
                commentDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                commentDialog.setContentView(R.layout.dialog_comment);
                commentDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                commentDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                EditText commentInput = commentDialog.findViewById(R.id.commentInput);
                CardView cancelButton = commentDialog.findViewById(R.id.cancelCommentButton);
                CardView submitButton = commentDialog.findViewById(R.id.submitCommentButton);

                submitButton.setOnClickListener(v -> {
                    String comment = commentInput.getText().toString().trim();
                    if (!comment.isEmpty()) {
                        submitComment(post, comment);
                        commentDialog.dismiss();
                    } else {
                        Toast.makeText(ProfileViewActivity.this, "Please enter a comment", Toast.LENGTH_SHORT).show();
                    }
                });

                cancelButton.setOnClickListener(v -> commentDialog.dismiss());
                commentDialog.show();
            }

            private void submitComment(Post post, String comment) {
                JSONObject json = new JSONObject();
                try {
                    json.put("post_id", post.postId);
                    json.put("user_id", viewerId);
                    json.put("token", authToken);
                    json.put("comment", comment);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                RequestBody body = RequestBody.create(MediaType.parse("application/json"), json.toString());
                Request request = new Request.Builder()
                        .url(BASE_URL + "comments.php?action=add")
                        .post(body)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {}

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        runOnUiThread(() -> refreshProfile());
                    }
                });
            }

            private void sharePost(Post post) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                String shareText = "Check out this post: " + post.content;
                if (post.linkUrl != null && !post.linkUrl.isEmpty()) {
                    shareText += "\n\n" + post.linkUrl;
                }
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                startActivity(Intent.createChooser(shareIntent, "Share via"));
            }
        }

        class AdViewHolder extends RecyclerView.ViewHolder {
            AdView adView;

            AdViewHolder(View itemView) {
                super(itemView);
                adView = itemView.findViewById(R.id.nativeAdView);
                loadAd();
            }

            private void loadAd() {
                AdRequest adRequest = new AdRequest.Builder().build();
                adView.loadAd(adRequest);
            }
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
        handler.removeCallbacksAndMessages(null);
    }
}