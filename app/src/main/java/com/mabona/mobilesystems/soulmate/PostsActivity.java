package com.mabona.mobilesystems.soulmate;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PostsActivity extends AppCompatActivity {

    private static final String BASE_URL = "https://mabona.firstsuninvestment.com/soulmate/";

    private int userId;
    private String authToken;
    private String userEmail;
    private String userName;

    private RecyclerView postsRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView emptyStateText;
    private LinearLayout emptyStateLayout;
    private AdView bannerAdView;
    private ImageView backButton;
    private ImageView refreshButton;
    private ImageView heartImage1, heartImage2, heartImage3;

    private PostsAdapter postsAdapter;
    private List<Post> postsList = new ArrayList<>();
    private boolean isLoading = false;
    private boolean hasMore = true;
    private int currentOffset = 0;
    private static final int PAGE_SIZE = 10;
    private static final int AD_INTERVAL = 5;

    private OkHttpClient client;
    private Handler handler = new Handler();

    // Animation for hearts
    private Runnable heartAnimation1, heartAnimation2, heartAnimation3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_posts);

        // Get user data
        userId = getIntent().getIntExtra("USER_ID", -1);
        authToken = getIntent().getStringExtra("AUTH_TOKEN");
        userEmail = getIntent().getStringExtra("USER_EMAIL");
        userName = getIntent().getStringExtra("USER_NAME");

        if (userId == -1 || authToken == null || authToken.isEmpty()) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initializeViews();
        setupAnimations();
        setupRecyclerView();
        setupSwipeRefresh();
        loadAds();
        loadPosts();
    }

    private void initializeViews() {
        postsRecyclerView = findViewById(R.id.postsRecyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        progressBar = findViewById(R.id.progressBar);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        emptyStateText = findViewById(R.id.emptyStateText);
        bannerAdView = findViewById(R.id.bannerAdView);
        backButton = findViewById(R.id.backButton);
        refreshButton = findViewById(R.id.refreshButton);
        heartImage1 = findViewById(R.id.heartImage1);
        heartImage2 = findViewById(R.id.heartImage2);
        heartImage3 = findViewById(R.id.heartImage3);

        backButton.setOnClickListener(v -> finish());
        refreshButton.setOnClickListener(v -> refreshPosts());

        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    private void setupAnimations() {
        heartAnimation1 = () -> {
            heartImage1.animate()
                    .translationYBy(-20f)
                    .setDuration(2000)
                    .withEndAction(() -> heartImage1.animate().translationYBy(20f).setDuration(2000).withEndAction(heartAnimation1).start())
                    .start();
        };

        heartAnimation2 = () -> {
            heartImage2.animate()
                    .translationYBy(20f)
                    .setDuration(2200)
                    .withEndAction(() -> heartImage2.animate().translationYBy(-20f).setDuration(2200).withEndAction(heartAnimation2).start())
                    .start();
        };

        heartAnimation3 = () -> {
            heartImage3.animate()
                    .translationXBy(15f)
                    .setDuration(1800)
                    .withEndAction(() -> heartImage3.animate().translationXBy(-15f).setDuration(1800).withEndAction(heartAnimation3).start())
                    .start();
        };

        heartAnimation1.run();
        heartAnimation2.run();
        heartAnimation3.run();
    }

    private void setupRecyclerView() {
        postsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        postsAdapter = new PostsAdapter();
        postsRecyclerView.setAdapter(postsAdapter);

        postsRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (!isLoading && hasMore && layoutManager != null &&
                        layoutManager.findLastCompletelyVisibleItemPosition() >= postsAdapter.getItemCount() - 3) {
                    loadMorePosts();
                }
            }
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeColors(getColor(R.color.pink), getColor(R.color.purple));
        swipeRefreshLayout.setOnRefreshListener(this::refreshPosts);
    }

    private void refreshPosts() {
        postsList.clear();
        currentOffset = 0;
        hasMore = true;
        loadPosts();
    }

    private void loadPosts() {
        if (currentOffset == 0) {
            showProgress(true);
        }
        isLoading = true;

        String url = BASE_URL + "postview.php?user_id=" + userId + "&token=" + authToken +
                "&limit=" + PAGE_SIZE + "&offset=" + currentOffset;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    swipeRefreshLayout.setRefreshing(false);
                    isLoading = false;
                    Toast.makeText(PostsActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                            JSONArray postsArray = json.getJSONArray("posts");
                            hasMore = json.getBoolean("has_more");

                            List<Post> newPosts = new ArrayList<>();
                            for (int i = 0; i < postsArray.length(); i++) {
                                JSONObject postJson = postsArray.getJSONObject(i);
                                Post post = Post.fromJson(postJson);
                                newPosts.add(post);
                            }

                            if (currentOffset == 0) {
                                postsList.clear();
                            }
                            postsList.addAll(newPosts);
                            currentOffset += postsArray.length();
                            postsAdapter.notifyDataSetChanged();

                            if (postsList.isEmpty()) {
                                emptyStateLayout.setVisibility(View.VISIBLE);
                                postsRecyclerView.setVisibility(View.GONE);
                            } else {
                                emptyStateLayout.setVisibility(View.GONE);
                                postsRecyclerView.setVisibility(View.VISIBLE);
                            }
                        } else {
                            Toast.makeText(PostsActivity.this, json.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(PostsActivity.this, "Error parsing data", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void loadMorePosts() {
        if (hasMore && !isLoading) {
            loadPosts();
        }
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show && postsList.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
        }
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

    // Post Model Class
    static class Post {
        int postId;
        int userId;
        String userName;
        String userProfileImage;
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

        static Post fromJson(JSONObject json) throws Exception {
            Post post = new Post();
            post.postId = json.getInt("post_id");
            post.userId = json.getInt("user_id");
            post.userName = json.getString("user_name");
            post.userProfileImage = json.getString("user_profile_image");
            post.content = json.getString("content");
            post.linkUrl = json.optString("link_url", "");
            post.likesCount = json.getInt("likes_count");
            post.commentsCount = json.getInt("comments_count");
            post.userLiked = json.getInt("user_liked") == 1;
            post.userReaction = json.optString("user_reaction", "");
            post.reactions = json.optJSONObject("reactions");
            post.createdAt = json.getString("created_at");
            post.timeAgo = json.getString("time_ago");

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

    // Posts Adapter
    class PostsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_POST = 0;
        private static final int TYPE_AD = 1;

        @Override
        public int getItemViewType(int position) {
            if (position > 0 && position % AD_INTERVAL == 0 && position <= postsList.size()) {
                return TYPE_AD;
            }
            return TYPE_POST;
        }

        @Override
        public int getItemCount() {
            int adCount = postsList.size() / AD_INTERVAL;
            return postsList.size() + adCount;
        }

        private int getRealPostPosition(int position) {
            int adBefore = position / AD_INTERVAL;
            return position - adBefore;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_AD) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ad, parent, false);
                return new AdViewHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
                return new PostViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof PostViewHolder) {
                int postPosition = getRealPostPosition(position);
                if (postPosition < postsList.size()) {
                    ((PostViewHolder) holder).bind(postsList.get(postPosition));
                }
            }
        }

        class PostViewHolder extends RecyclerView.ViewHolder {
            CardView profileImageCard;
            ImageView profileImageView;
            TextView userNameText;
            TextView timeAgoText;
            ImageView chatButton;
            TextView postContentText;
            CardView linkPreviewCard;
            TextView linkPreviewText;
            LinearLayout mediaContainer;
            CardView singleImageCard;
            ImageView singleImageView;
            LinearLayout imagesGridContainer;
            LinearLayout imageRow1;
            LinearLayout imageRow2;
            CardView videoCard;
            ImageView videoThumbnail;
            ImageView playButton;
            TextView readMoreButton;
            LinearLayout likeButton;
            ImageView likeIcon;
            TextView likeCountText;
            LinearLayout commentButton;
            TextView commentCountText;
            LinearLayout shareButton;
            LinearLayout reactionsSummary;
            LinearLayout reactionIconsContainer;
            TextView reactionCountText;
            LinearLayout commentsPreview;
            TextView comment1Text;
            TextView comment2Text;
            TextView viewAllCommentsText;

            PostViewHolder(View itemView) {
                super(itemView);
                profileImageCard = itemView.findViewById(R.id.profileImageCard);
                profileImageView = itemView.findViewById(R.id.profileImageView);
                userNameText = itemView.findViewById(R.id.userNameText);
                timeAgoText = itemView.findViewById(R.id.timeAgoText);
                chatButton = itemView.findViewById(R.id.chatButton);
                postContentText = itemView.findViewById(R.id.postContentText);
                linkPreviewCard = itemView.findViewById(R.id.linkPreviewCard);
                linkPreviewText = itemView.findViewById(R.id.linkPreviewText);
                mediaContainer = itemView.findViewById(R.id.mediaContainer);
                singleImageCard = itemView.findViewById(R.id.singleImageCard);
                singleImageView = itemView.findViewById(R.id.singleImageView);
                imagesGridContainer = itemView.findViewById(R.id.imagesGridContainer);
                imageRow1 = itemView.findViewById(R.id.imageRow1);
                imageRow2 = itemView.findViewById(R.id.imageRow2);
                videoCard = itemView.findViewById(R.id.videoCard);
                videoThumbnail = itemView.findViewById(R.id.videoThumbnail);
                playButton = itemView.findViewById(R.id.playButton);
                readMoreButton = itemView.findViewById(R.id.readMoreButton);
                likeButton = itemView.findViewById(R.id.likeButton);
                likeIcon = itemView.findViewById(R.id.likeIcon);
                likeCountText = itemView.findViewById(R.id.likeCountText);
                commentButton = itemView.findViewById(R.id.commentButton);
                commentCountText = itemView.findViewById(R.id.commentCountText);
                shareButton = itemView.findViewById(R.id.shareButton);
                reactionsSummary = itemView.findViewById(R.id.reactionsSummary);
                reactionIconsContainer = itemView.findViewById(R.id.reactionIconsContainer);
                reactionCountText = itemView.findViewById(R.id.reactionCountText);
                commentsPreview = itemView.findViewById(R.id.commentsPreview);
                comment1Text = itemView.findViewById(R.id.comment1Text);
                comment2Text = itemView.findViewById(R.id.comment2Text);
                viewAllCommentsText = itemView.findViewById(R.id.viewAllCommentsText);
            }

            void bind(Post post) {
                // Set user info
                userNameText.setText(post.userName);
                timeAgoText.setText(post.timeAgo);

                // Load profile image
                if (!post.userProfileImage.equals("default_profile") && !post.userProfileImage.isEmpty()) {
                    String imageUrl = BASE_URL + "get_image.php?path=" + post.userProfileImage + "&token=" + authToken;
                    Glide.with(PostsActivity.this)
                            .load(imageUrl)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.default_profile)
                            .error(R.drawable.default_profile)
                            .into(profileImageView);
                } else {
                    profileImageView.setImageResource(R.drawable.default_profile);
                }

                // Set content with read more if needed
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
                    setupMedia(post);
                } else {
                    mediaContainer.setVisibility(View.GONE);
                }

                // Set like button state
                if (post.userLiked) {
                    likeIcon.setImageResource(R.drawable.ic_heart_filled);
                    likeIcon.setColorFilter(getColor(R.color.red));
                } else {
                    likeIcon.setImageResource(R.drawable.ic_heart_outline);
                    likeIcon.setColorFilter(getColor(R.color.medium_gray));
                }
                likeCountText.setText(String.valueOf(post.likesCount));
                commentCountText.setText(String.valueOf(post.commentsCount));

                // Setup reaction on long press
                likeButton.setOnLongClickListener(v -> {
                    showReactionPicker(post);
                    return true;
                });

                likeButton.setOnClickListener(v -> {
                    sendReaction(post, "like");
                });

                commentButton.setOnClickListener(v -> {
                    showCommentDialog(post);
                });

                shareButton.setOnClickListener(v -> {
                    sharePost(post);
                });

                chatButton.setOnClickListener(v -> {
                    openChat(post);
                });

                // Show reactions summary
                setupReactionsSummary(post);

                // Show comments preview
                setupCommentsPreview(post);
            }

            private void setupMedia(Post post) {
                List<MediaItem> images = new ArrayList<>();
                MediaItem video = null;

                for (MediaItem media : post.media) {
                    if (media.type.equals("image")) {
                        images.add(media);
                    } else if (media.type.equals("video")) {
                        video = media;
                    }
                }

                // Handle images
                if (!images.isEmpty()) {
                    if (images.size() == 1) {
                        // Single image
                        singleImageCard.setVisibility(View.VISIBLE);
                        imagesGridContainer.setVisibility(View.GONE);
                        loadImage(images.get(0).path, singleImageView);
                        singleImageView.setOnClickListener(v -> zoomImage(images.get(0).path));
                    } else {
                        // Multiple images in grid
                        singleImageCard.setVisibility(View.GONE);
                        imagesGridContainer.setVisibility(View.VISIBLE);
                        imageRow1.removeAllViews();
                        imageRow2.removeAllViews();

                        for (int i = 0; i < images.size(); i++) {
                            ImageView imageView = createImageViewForGrid();
                            loadImage(images.get(i).path, imageView);
                            final String imagePath = images.get(i).path;
                            imageView.setOnClickListener(v -> zoomImage(imagePath));

                            if (i < 2) {
                                imageRow1.addView(imageView);
                            } else {
                                if (imageRow2.getVisibility() != View.VISIBLE) {
                                    imageRow2.setVisibility(View.VISIBLE);
                                }
                                imageRow2.addView(imageView);
                            }
                        }
                    }
                } else {
                    singleImageCard.setVisibility(View.GONE);
                    imagesGridContainer.setVisibility(View.GONE);
                }

                // Handle video
                if (video != null) {
                    videoCard.setVisibility(View.VISIBLE);
                    loadImage(video.path, videoThumbnail);
                    final String videoPath = video.path;
                    playButton.setOnClickListener(v -> playVideo(videoPath));
                } else {
                    videoCard.setVisibility(View.GONE);
                }
            }

            private ImageView createImageViewForGrid() {
                ImageView imageView = new ImageView(PostsActivity.this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, 200, 1);
                params.setMargins(4, 0, 4, 0);
                imageView.setLayoutParams(params);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                return imageView;
            }

            private void loadImage(String path, ImageView imageView) {
                String imageUrl = BASE_URL + "get_image.php?path=" + path + "&token=" + authToken;
                Glide.with(PostsActivity.this)
                        .load(imageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .into(imageView);
            }

            private void zoomImage(String path) {
                Dialog zoomDialog = new Dialog(PostsActivity.this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                zoomDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                zoomDialog.setContentView(R.layout.dialog_image_zoom);
                zoomDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));

                ImageView zoomImageView = zoomDialog.findViewById(R.id.zoomImageView);
                ImageView saveImageView = zoomDialog.findViewById(R.id.saveImageView);

                String imageUrl = BASE_URL + "get_image.php?path=" + path + "&token=" + authToken;
                Glide.with(PostsActivity.this).load(imageUrl).into(zoomImageView);

                if (saveImageView != null) {
                    saveImageView.setOnClickListener(v -> saveImageToDevice(imageUrl));
                }
                
                if (zoomImageView != null) {
                    zoomImageView.setOnClickListener(v -> zoomDialog.dismiss());
                }

                zoomDialog.show();
            }

            private void saveImageToDevice(String imageUrl) {
                Toast.makeText(PostsActivity.this, "Downloading image...", Toast.LENGTH_SHORT).show();
                // Implement image download
            }

            private void playVideo(String path) {
                String videoUrl = BASE_URL + "get_image.php?path=" + path + "&token=" + authToken;
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(videoUrl), "video/mp4");
                startActivity(intent);
            }

            private void setupReactionsSummary(Post post) {
                if (post.likesCount > 0) {
                    reactionsSummary.setVisibility(View.VISIBLE);
                    reactionIconsContainer.removeAllViews();

                    // Add reaction icons
                    if (post.reactions != null) {
                        if (post.reactions.has("like")) addReactionIcon(R.drawable.ic_like_small);
                        if (post.reactions.has("love")) addReactionIcon(R.drawable.ic_love_small);
                        if (post.reactions.has("haha")) addReactionIcon(R.drawable.ic_haha);
                        if (post.reactions.has("wow")) addReactionIcon(R.drawable.ic_wow);
                        if (post.reactions.has("sad")) addReactionIcon(R.drawable.ic_sad);
                        if (post.reactions.has("angry")) addReactionIcon(R.drawable.ic_angry);
                    }

                    if (post.likesCount == 1) {
                        reactionCountText.setText("1 person reacted");
                    } else {
                        reactionCountText.setText(post.likesCount + " people reacted");
                    }
                } else {
                    reactionsSummary.setVisibility(View.GONE);
                }
            }

            private void addReactionIcon(int drawable) {
                ImageView icon = new ImageView(PostsActivity.this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(28, 28);
                params.setMargins(0, 0, 4, 0);
                icon.setLayoutParams(params);
                icon.setImageResource(drawable);
                reactionIconsContainer.addView(icon);
            }

            private void setupCommentsPreview(Post post) {
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
                        viewAllCommentsText.setOnClickListener(v -> showAllComments(post));
                    } else {
                        viewAllCommentsText.setVisibility(View.GONE);
                    }
                } else {
                    commentsPreview.setVisibility(View.GONE);
                }
            }
        }

        class AdViewHolder extends RecyclerView.ViewHolder {
            AdView adView;

            AdViewHolder(View itemView) {
                super(itemView);
                adView = (AdView) itemView;
                loadBannerAd();
            }

            private void loadBannerAd() {
                AdRequest adRequest = new AdRequest.Builder().build();
                adView.loadAd(adRequest);
                adView.setAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                        adView.setVisibility(View.GONE);
                    }
                });
            }
        }
    }

    private void showReactionPicker(Post post) {
        Dialog reactionDialog = new Dialog(this);
        reactionDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        reactionDialog.setContentView(R.layout.reaction_popup);
        reactionDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        reactionDialog.getWindow().setGravity(Gravity.CENTER);

        ImageView reactionLike = reactionDialog.findViewById(R.id.reactionLike);
        ImageView reactionLove = reactionDialog.findViewById(R.id.reactionLove);
        ImageView reactionHaha = reactionDialog.findViewById(R.id.reactionHaha);
        ImageView reactionWow = reactionDialog.findViewById(R.id.reactionWow);
        ImageView reactionSad = reactionDialog.findViewById(R.id.reactionSad);
        ImageView reactionAngry = reactionDialog.findViewById(R.id.reactionAngry);

        reactionLike.setOnClickListener(v -> { sendReaction(post, "like"); reactionDialog.dismiss(); });
        reactionLove.setOnClickListener(v -> { sendReaction(post, "love"); reactionDialog.dismiss(); });
        reactionHaha.setOnClickListener(v -> { sendReaction(post, "haha"); reactionDialog.dismiss(); });
        reactionWow.setOnClickListener(v -> { sendReaction(post, "wow"); reactionDialog.dismiss(); });
        reactionSad.setOnClickListener(v -> { sendReaction(post, "sad"); reactionDialog.dismiss(); });
        reactionAngry.setOnClickListener(v -> { sendReaction(post, "angry"); reactionDialog.dismiss(); });

        reactionDialog.show();

        new Handler().postDelayed(reactionDialog::dismiss, 3000);
    }

    private void sendReaction(Post post, String reactionType) {
        JSONObject json = new JSONObject();
        try {
            json.put("post_id", post.postId);
            json.put("user_id", userId);
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
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(PostsActivity.this, "Failed to send reaction", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> refreshPosts());
            }
        });
    }

    private void showCommentDialog(Post post) {
        Dialog commentDialog = new Dialog(this);
        commentDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        commentDialog.setContentView(R.layout.dialog_comment);
        commentDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        commentDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        EditText commentInput = commentDialog.findViewById(R.id.commentInput);
        // FIX: Changed from TextView to CardView for cancel button
        CardView cancelButton = commentDialog.findViewById(R.id.cancelCommentButton);
        // FIX: Changed from TextView to CardView for submit button
        CardView submitButton = commentDialog.findViewById(R.id.submitCommentButton);

        submitButton.setOnClickListener(v -> {
            String comment = commentInput.getText().toString().trim();
            if (!comment.isEmpty()) {
                submitComment(post, comment);
                commentDialog.dismiss();
            } else {
                Toast.makeText(this, "Please enter a comment", Toast.LENGTH_SHORT).show();
            }
        });

        cancelButton.setOnClickListener(v -> commentDialog.dismiss());
        commentDialog.show();
    }

    private void submitComment(Post post, String comment) {
        JSONObject json = new JSONObject();
        try {
            json.put("post_id", post.postId);
            json.put("user_id", userId);
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
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(PostsActivity.this, "Failed to post comment", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> refreshPosts());
            }
        });
    }

    private void showAllComments(Post post) {
        Dialog commentsDialog = new Dialog(this);
        commentsDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        commentsDialog.setContentView(R.layout.dialog_comments_list);
        commentsDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        commentsDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        RecyclerView commentsRecyclerView = commentsDialog.findViewById(R.id.commentsRecyclerView);
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<Comment> comments = new ArrayList<>();
        // Load all comments for this post

        commentsDialog.show();
    }

    private void sharePost(Post post) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String shareText = post.userName + " shared: " + post.content;
        if (post.linkUrl != null && !post.linkUrl.isEmpty()) {
            shareText += "\n\n" + post.linkUrl;
        }
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    private void openChat(Post post) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("OTHER_USER_ID", post.userId);
        intent.putExtra("OTHER_USER_NAME", post.userName);
        intent.putExtra("USER_ID", userId);
        intent.putExtra("AUTH_TOKEN", authToken);
        intent.putExtra("USER_EMAIL", userEmail);
        intent.putExtra("USER_NAME", userName);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bannerAdView != null) bannerAdView.resume();
        refreshPosts();
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