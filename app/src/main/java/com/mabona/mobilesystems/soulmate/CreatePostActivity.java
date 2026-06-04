package com.mabona.mobilesystems.soulmate;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.yalantis.ucrop.UCrop;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CreatePostActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 100;
    private static final int PICK_VIDEO_REQUEST = 101;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final int REQUEST_IMAGE_CROP = 300;

    // UI Components
    private ImageView backButton;
    private TextView postButton;
    private EditText postContentEditText;
    private TextView hintText;
    private CardView linkPreviewCard;
    private TextView linkTitle;
    private TextView linkUrl;
    private CardView addImageCard;
    private CardView addVideoCard;
    private LinearLayout imagesContainer;
    private LinearLayout imagesGrid;
    private CardView videoPreviewCard;
    private ImageView videoPreviewImage;
    private TextView videoFileName;
    private TextView videoFileSize;
    private ImageView editVideoButton;
    private ImageView removeVideoButton;
    private ProgressBar progressBar;
    private TextView statusText;
    private AdView bannerAdView;
    private CardView writeAnotherCard;
    private ImageView heartImage1;
    private ImageView heartImage2;
    private ImageView heartImage3;

    // Data
    private int userId;
    private String authToken;

    private List<String> selectedImagePaths = new ArrayList<>();
    private String selectedVideoPath = null;
    private String detectedLink = null;

    // Edit history stacks for image editing
    private Stack<Bitmap> undoStack = new Stack<>();
    private Stack<Bitmap> redoStack = new Stack<>();
    private Bitmap currentEditingBitmap = null;

    // Video state
    private boolean isVideoMuted = false;
    private int videoTrimStart = 0;
    private int videoTrimEnd = 0;
    private int videoDuration = 0;
    private boolean isVideoPlaying = false;

    private Handler hintHandler = new Handler();
    private String[] hintMessages = {
            "💭 Write your love story...",
            "❤️ Share how you met your soulmate...",
            "📝 Your story could change other couples...",
            "🔍 Find a lover by posting your preferences...",
            "💕 What does love mean to you?",
            "✨ Share a romantic moment...",
            "🌹 Write a love letter to your future partner..."
    };
    private int hintIndex = 0;

    private static final String BASE_URL = "https://mabona.firstsuninvestment.com/soulmate/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        userId = getIntent().getIntExtra("USER_ID", -1);
        authToken = getIntent().getStringExtra("AUTH_TOKEN");

        if (userId == -1 || authToken == null || authToken.isEmpty()) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initializeViews();
        setupAnimations();
        setupTextWatcher();
        setupClickListeners();
        startHintRotation();
        loadAd();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        postButton = findViewById(R.id.postButton);
        postContentEditText = findViewById(R.id.postContentEditText);
        hintText = findViewById(R.id.hintText);
        linkPreviewCard = findViewById(R.id.linkPreviewCard);
        linkTitle = findViewById(R.id.linkTitle);
        linkUrl = findViewById(R.id.linkUrl);
        addImageCard = findViewById(R.id.addImageCard);
        addVideoCard = findViewById(R.id.addVideoCard);
        imagesContainer = findViewById(R.id.imagesContainer);
        imagesGrid = findViewById(R.id.imagesGrid);
        videoPreviewCard = findViewById(R.id.videoPreviewCard);
        videoPreviewImage = findViewById(R.id.videoPreviewImage);
        videoFileName = findViewById(R.id.videoFileName);
        videoFileSize = findViewById(R.id.videoFileSize);
        editVideoButton = findViewById(R.id.editVideoButton);
        removeVideoButton = findViewById(R.id.removeVideoButton);
        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusText);
        bannerAdView = findViewById(R.id.bannerAdView);
        writeAnotherCard = findViewById(R.id.writeAnotherCard);
        heartImage1 = findViewById(R.id.heartImage1);
        heartImage2 = findViewById(R.id.heartImage2);
        heartImage3 = findViewById(R.id.heartImage3);

        backButton.setOnClickListener(v -> finish());
        postButton.setOnClickListener(v -> createPost());
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

    private void setupTextWatcher() {
        postContentEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                detectLinks(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();
                Pattern urlPattern = Pattern.compile("(https?://[\\w\\d\\-\\._~:/?#\\[\\]@!$&'()*+,;=%]+)");
                Matcher matcher = urlPattern.matcher(text);
                while (matcher.find()) {
                    int start = matcher.start();
                    int end = matcher.end();
                    if (start >= 0 && end <= s.length()) {
                        s.setSpan(new ForegroundColorSpan(getColor(R.color.pink)), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }
        });
    }

    private void detectLinks(String text) {
        Pattern urlPattern = Pattern.compile("(https?://[\\w\\d\\-\\._~:/?#\\[\\]@!$&'()*+,;=%]+)");
        Matcher matcher = urlPattern.matcher(text);

        if (matcher.find()) {
            detectedLink = matcher.group();
            linkUrl.setText(detectedLink);
            linkTitle.setText("Link detected");
            linkPreviewCard.setVisibility(View.VISIBLE);
        } else {
            detectedLink = null;
            linkPreviewCard.setVisibility(View.GONE);
        }
    }

    private void startHintRotation() {
        hintHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                hintIndex = (hintIndex + 1) % hintMessages.length;
                hintText.startAnimation(android.view.animation.AnimationUtils.loadAnimation(CreatePostActivity.this, android.R.anim.fade_in));
                hintText.setText(hintMessages[hintIndex]);
                hintHandler.postDelayed(this, 4000);
            }
        }, 4000);
    }

    private void setupClickListeners() {
        addImageCard.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                openImagePicker();
            }
        });

        addVideoCard.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                if (selectedVideoPath != null) {
                    Toast.makeText(this, "You can only add one video per post", Toast.LENGTH_SHORT).show();
                    return;
                }
                openVideoPicker();
            }
        });

        editVideoButton.setOnClickListener(v -> {
            if (selectedVideoPath != null) {
                showVideoEditDialog();
            }
        });

        removeVideoButton.setOnClickListener(v -> {
            selectedVideoPath = null;
            videoPreviewCard.setVisibility(View.GONE);
        });

        writeAnotherCard.setOnClickListener(v -> {
            resetForm();
            writeAnotherCard.setVisibility(View.GONE);
            postButton.setEnabled(true);
            postButton.setAlpha(1f);
        });
    }

    private void resetForm() {
        postContentEditText.setText("");
        selectedImagePaths.clear();
        selectedVideoPath = null;
        detectedLink = null;
        updateImagesPreview();
        videoPreviewCard.setVisibility(View.GONE);
        imagesContainer.setVisibility(View.GONE);
        linkPreviewCard.setVisibility(View.GONE);
        undoStack.clear();
        redoStack.clear();
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_REQUEST_CODE);
                return false;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                return false;
            }
        }
        return true;
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void openVideoPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_VIDEO_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null) {
                Uri selectedUri = data.getData();
                String imagePath = getRealPathFromUri(selectedUri);
                if (imagePath != null) {
                    selectedImagePaths.add(imagePath);
                    updateImagesPreview();
                }
            } else if (requestCode == PICK_VIDEO_REQUEST && data != null) {
                Uri selectedUri = data.getData();
                selectedVideoPath = getRealPathFromUri(selectedUri);
                updateVideoPreview();
                loadVideoMetadata();
            } else if (requestCode == REQUEST_IMAGE_CROP && data != null) {
                handleCropResult(data);
            }
        }
    }

    private void handleCropResult(Intent data) {
        Uri resultUri = UCrop.getOutput(data);
        if (resultUri != null) {
            String croppedPath = getRealPathFromUri(resultUri);
            if (croppedPath != null) {
                int currentIndex = selectedImagePaths.size() - 1;
                if (currentIndex >= 0) {
                    selectedImagePaths.set(currentIndex, croppedPath);
                    updateImagesPreview();
                }
            }
        }
    }

    // ==================== IMAGE EDITING METHODS ====================

    private void showImageEditDialog(final String imagePath, final int position) {
        Dialog editDialog = new Dialog(this);
        editDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        editDialog.setContentView(R.layout.dialog_image_edit);
        editDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        editDialog.setCancelable(true);

        ImageView editImageView = editDialog.findViewById(R.id.editImageView);
        CardView cropButton = editDialog.findViewById(R.id.cropButton);
        CardView addTextButton = editDialog.findViewById(R.id.addTextButton);
        CardView colorButton = editDialog.findViewById(R.id.colorButton);
        CardView undoButton = editDialog.findViewById(R.id.undoButton);
        CardView redoButton = editDialog.findViewById(R.id.redoButton);
        CardView cancelButton = editDialog.findViewById(R.id.cancelEditButton);
        CardView saveButton = editDialog.findViewById(R.id.saveEditButton);

        currentEditingBitmap = BitmapFactory.decodeFile(imagePath);
        editImageView.setImageBitmap(currentEditingBitmap);

        undoStack.clear();
        redoStack.clear();
        undoStack.push(currentEditingBitmap.copy(currentEditingBitmap.getConfig(), true));

        cropButton.setOnClickListener(v -> {
            Uri sourceUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", new File(imagePath));
            UCrop.of(sourceUri, sourceUri)
                    .withAspectRatio(1, 1)
                    .withMaxResultSize(1080, 1080)
                    .start(this, REQUEST_IMAGE_CROP);
            editDialog.dismiss();
        });

        addTextButton.setOnClickListener(v -> {
            showAddTextDialog(editImageView, new Runnable() {
                @Override
                public void run() {
                    // After text is added, update currentEditingBitmap
                    editImageView.buildDrawingCache();
                    currentEditingBitmap = editImageView.getDrawingCache();
                    undoStack.push(currentEditingBitmap.copy(currentEditingBitmap.getConfig(), true));
                }
            });
        });

        colorButton.setOnClickListener(v -> {
            Toast.makeText(this, "Color filter coming soon", Toast.LENGTH_SHORT).show();
        });

        undoButton.setOnClickListener(v -> {
            if (undoStack.size() > 1) {
                redoStack.push(undoStack.pop());
                currentEditingBitmap = undoStack.peek();
                editImageView.setImageBitmap(currentEditingBitmap);
            } else {
                Toast.makeText(this, "Nothing to undo", Toast.LENGTH_SHORT).show();
            }
        });

        redoButton.setOnClickListener(v -> {
            if (!redoStack.isEmpty()) {
                currentEditingBitmap = redoStack.pop();
                undoStack.push(currentEditingBitmap);
                editImageView.setImageBitmap(currentEditingBitmap);
            } else {
                Toast.makeText(this, "Nothing to redo", Toast.LENGTH_SHORT).show();
            }
        });

        cancelButton.setOnClickListener(v -> editDialog.dismiss());

        saveButton.setOnClickListener(v -> {
            try {
                File outputFile = new File(getCacheDir(), "edited_image_" + System.currentTimeMillis() + ".jpg");
                FileOutputStream fos = new FileOutputStream(outputFile);
                currentEditingBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                fos.close();

                selectedImagePaths.set(position, outputFile.getAbsolutePath());
                updateImagesPreview();
                editDialog.dismiss();
                Toast.makeText(CreatePostActivity.this, "Image saved", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(CreatePostActivity.this, "Failed to save image", Toast.LENGTH_SHORT).show();
            }
        });

        editDialog.show();
    }

    private void showAddTextDialog(ImageView targetImageView, Runnable onTextAdded) {
        Dialog textDialog = new Dialog(this);
        textDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        textDialog.setContentView(R.layout.dialog_add_text);
        textDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        textDialog.setCancelable(true);

        EditText textInput = textDialog.findViewById(R.id.textInput);
        SeekBar textSizeSeekBar = textDialog.findViewById(R.id.textSizeSeekBar);
        CardView cancelButton = textDialog.findViewById(R.id.cancelTextButton);
        CardView saveButton = textDialog.findViewById(R.id.saveTextButton);

        final int[] selectedColor = {Color.WHITE};
        final int[] textSize = {24};

        View colorWhite = textDialog.findViewById(R.id.colorWhite);
        View colorPink = textDialog.findViewById(R.id.colorPink);
        View colorPurple = textDialog.findViewById(R.id.colorPurple);
        View colorRed = textDialog.findViewById(R.id.colorRed);

        colorWhite.setOnClickListener(v -> selectedColor[0] = Color.WHITE);
        colorPink.setOnClickListener(v -> selectedColor[0] = Color.parseColor("#FF69B4"));
        colorPurple.setOnClickListener(v -> selectedColor[0] = Color.parseColor("#9370DB"));
        colorRed.setOnClickListener(v -> selectedColor[0] = Color.parseColor("#FF4500"));

        textSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textSize[0] = progress;
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        saveButton.setOnClickListener(v -> {
            String text = textInput.getText().toString();
            if (!text.isEmpty()) {
                Toast.makeText(this, "Text added: " + text, Toast.LENGTH_SHORT).show();
                // Here you would actually add text to the image
                textDialog.dismiss();
                if (onTextAdded != null) {
                    onTextAdded.run();
                }
            } else {
                Toast.makeText(this, "Please enter text", Toast.LENGTH_SHORT).show();
            }
        });

        cancelButton.setOnClickListener(v -> textDialog.dismiss());
        textDialog.show();
    }

    // ==================== VIDEO EDITING METHODS ====================

    private void showVideoEditDialog() {
        Dialog videoDialog = new Dialog(this);
        videoDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        videoDialog.setContentView(R.layout.dialog_video_edit);
        videoDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        videoDialog.setCancelable(true);

        VideoView editVideoView = videoDialog.findViewById(R.id.editVideoView);
        CardView playPauseButton = videoDialog.findViewById(R.id.playPauseButton);
        ImageView playPauseIcon = videoDialog.findViewById(R.id.playPauseIcon);
        TextView playPauseText = videoDialog.findViewById(R.id.playPauseText);
        CardView muteButton = videoDialog.findViewById(R.id.muteButton);
        ImageView muteIcon = videoDialog.findViewById(R.id.muteIcon);
        TextView muteText = videoDialog.findViewById(R.id.muteText);
        CardView addTextButton = videoDialog.findViewById(R.id.videoAddTextButton);
        SeekBar trimSeekBar = videoDialog.findViewById(R.id.trimSeekBar);
        TextView startTimeText = videoDialog.findViewById(R.id.startTimeText);
        TextView endTimeText = videoDialog.findViewById(R.id.endTimeText);
        CardView cancelButton = videoDialog.findViewById(R.id.cancelVideoEditButton);
        CardView saveButton = videoDialog.findViewById(R.id.saveVideoEditButton);

        Uri videoUri = Uri.parse(selectedVideoPath);
        editVideoView.setVideoURI(videoUri);
        editVideoView.setOnPreparedListener(mp -> {
            videoDuration = mp.getDuration();
            videoTrimEnd = videoDuration;
            trimSeekBar.setMax(videoDuration);
            trimSeekBar.setProgress(videoDuration);
            updateTimeTexts(startTimeText, endTimeText, 0, videoDuration);
            mp.start();
            isVideoPlaying = true;
            playPauseIcon.setImageResource(R.drawable.ic_pause);
            playPauseText.setText("Pause");
        });

        playPauseButton.setOnClickListener(v -> {
            if (isVideoPlaying) {
                editVideoView.pause();
                playPauseIcon.setImageResource(R.drawable.ic_play);
                playPauseText.setText("Play");
                isVideoPlaying = false;
            } else {
                editVideoView.start();
                playPauseIcon.setImageResource(R.drawable.ic_pause);
                playPauseText.setText("Pause");
                isVideoPlaying = true;
            }
        });

        muteButton.setOnClickListener(v -> {
            isVideoMuted = !isVideoMuted;
            Toast.makeText(this, isVideoMuted ? "Video muted" : "Video unmuted", Toast.LENGTH_SHORT).show();
            muteIcon.setImageResource(isVideoMuted ? R.drawable.ic_mute : R.drawable.ic_unmute);
            muteText.setText(isVideoMuted ? "Unmute" : "Mute");
        });

        trimSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    videoTrimEnd = progress;
                    updateTimeTexts(startTimeText, endTimeText, videoTrimStart, videoTrimEnd);
                    editVideoView.seekTo(videoTrimStart);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        addTextButton.setOnClickListener(v -> {
            showVideoTextDialog();
        });

        editVideoView.setOnCompletionListener(mp -> {
            playPauseIcon.setImageResource(R.drawable.ic_play);
            playPauseText.setText("Play");
            isVideoPlaying = false;
        });

        cancelButton.setOnClickListener(v -> {
            if (editVideoView.isPlaying()) {
                editVideoView.stopPlayback();
            }
            videoDialog.dismiss();
        });

        saveButton.setOnClickListener(v -> {
            Toast.makeText(this, "Video changes saved", Toast.LENGTH_SHORT).show();
            if (editVideoView.isPlaying()) {
                editVideoView.stopPlayback();
            }
            videoDialog.dismiss();
        });

        videoDialog.setOnDismissListener(dialog -> {
            if (editVideoView.isPlaying()) {
                editVideoView.stopPlayback();
            }
        });

        videoDialog.show();
    }

    private void showVideoTextDialog() {
        Dialog textDialog = new Dialog(this);
        textDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        textDialog.setContentView(R.layout.dialog_add_text);
        textDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        textDialog.setCancelable(true);

        EditText textInput = textDialog.findViewById(R.id.textInput);
        CardView cancelButton = textDialog.findViewById(R.id.cancelTextButton);
        CardView saveButton = textDialog.findViewById(R.id.saveTextButton);

        saveButton.setOnClickListener(v -> {
            String text = textInput.getText().toString();
            if (!text.isEmpty()) {
                Toast.makeText(this, "Text will be added to video: " + text, Toast.LENGTH_SHORT).show();
                textDialog.dismiss();
            } else {
                Toast.makeText(this, "Please enter text", Toast.LENGTH_SHORT).show();
            }
        });

        cancelButton.setOnClickListener(v -> textDialog.dismiss());
        textDialog.show();
    }

    private void updateTimeTexts(TextView startText, TextView endText, int startMs, int endMs) {
        startText.setText(formatTime(startMs));
        endText.setText(formatTime(endMs));
    }

    private String formatTime(int ms) {
        int seconds = ms / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private void loadVideoMetadata() {
        if (selectedVideoPath != null) {
            File file = new File(selectedVideoPath);
            videoFileName.setText(file.getName());
            videoFileSize.setText(formatFileSize(file.length()));

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(selectedVideoPath);
                String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                videoDuration = Integer.parseInt(durationStr);
                videoTrimEnd = videoDuration;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    retriever.release();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    // ==================== HELPER METHODS ====================

    private String getRealPathFromUri(Uri uri) {
        try {
            String fileName = "post_" + System.currentTimeMillis() + ".jpg";
            File cacheFile = new File(getCacheDir(), fileName);
            InputStream inputStream = getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(cacheFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();
            return cacheFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void updateImagesPreview() {
        if (selectedImagePaths.isEmpty()) {
            imagesContainer.setVisibility(View.GONE);
            return;
        }

        imagesContainer.setVisibility(View.VISIBLE);
        imagesGrid.removeAllViews();

        for (int i = 0; i < selectedImagePaths.size(); i++) {
            String imagePath = selectedImagePaths.get(i);
            View imageItem = getLayoutInflater().inflate(R.layout.item_post_image, null);
            ImageView imageView = imageItem.findViewById(R.id.postImageView);
            ImageView removeIcon = imageItem.findViewById(R.id.removeImageIcon);
            ImageView editIcon = imageItem.findViewById(R.id.editImageIcon);
            TextView indexText = imageItem.findViewById(R.id.imageIndexText);

            Glide.with(this).load(imagePath).into(imageView);
            indexText.setText(String.valueOf(i + 1));

            final int position = i;
            removeIcon.setOnClickListener(v -> {
                selectedImagePaths.remove(position);
                updateImagesPreview();
            });

            editIcon.setOnClickListener(v -> {
                showImageEditDialog(imagePath, position);
            });

            imagesGrid.addView(imageItem);
        }
    }

    private void updateVideoPreview() {
        if (selectedVideoPath != null) {
            File file = new File(selectedVideoPath);
            videoFileName.setText(file.getName());
            videoFileSize.setText(formatFileSize(file.length()));

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(selectedVideoPath);
                Bitmap thumbnail = retriever.getFrameAtTime(0);
                if (thumbnail != null) {
                    videoPreviewImage.setImageBitmap(thumbnail);
                } else {
                    videoPreviewImage.setImageResource(R.drawable.ic_video);
                }
            } catch (Exception e) {
                e.printStackTrace();
                videoPreviewImage.setImageResource(R.drawable.ic_video);
            } finally {
                try {
                    retriever.release();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            videoPreviewCard.setVisibility(View.VISIBLE);
        }
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024.0));
    }

    private void createPost() {
        String content = postContentEditText.getText().toString().trim();

        if (content.isEmpty() && selectedImagePaths.isEmpty() && selectedVideoPath == null && detectedLink == null) {
            Toast.makeText(this, "Please write something or add media", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress(true);

        OkHttpClient client = new OkHttpClient();
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("user_id", String.valueOf(userId))
                .addFormDataPart("token", authToken);

        if (!content.isEmpty()) {
            builder.addFormDataPart("content", content);
        }

        if (detectedLink != null) {
            builder.addFormDataPart("link", detectedLink);
        }

        for (int i = 0; i < selectedImagePaths.size(); i++) {
            File imageFile = new File(selectedImagePaths.get(i));
            builder.addFormDataPart("images[]", imageFile.getName(),
                    RequestBody.create(MediaType.parse("image/jpeg"), imageFile));
        }

        if (selectedVideoPath != null) {
            File videoFile = new File(selectedVideoPath);
            builder.addFormDataPart("video", videoFile.getName(),
                    RequestBody.create(MediaType.parse("video/mp4"), videoFile));

            if (isVideoMuted) {
                builder.addFormDataPart("video_muted", "true");
            }
            if (videoTrimEnd < videoDuration) {
                builder.addFormDataPart("video_trim_start", String.valueOf(videoTrimStart));
                builder.addFormDataPart("video_trim_end", String.valueOf(videoTrimEnd));
            }
        }

        RequestBody requestBody = builder.build();

        Request request = new Request.Builder()
                .url(BASE_URL + "post.php")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(CreatePostActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseBody = response.body().string();
                runOnUiThread(() -> {
                    showProgress(false);
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        boolean success = jsonResponse.optBoolean("success", false);
                        String message = jsonResponse.optString("message", "Unknown error");

                        Toast.makeText(CreatePostActivity.this, message, Toast.LENGTH_LONG).show();

                        if (success) {
                            postButton.setEnabled(false);
                            postButton.setAlpha(0.6f);
                            writeAnotherCard.setVisibility(View.VISIBLE);
                            resetForm();
                        } else if (message.contains("expired") || message.contains("Invalid token")) {
                            goToLogin();
                        }
                    } catch (Exception e) {
                        Toast.makeText(CreatePostActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void goToLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        statusText.setVisibility(show ? View.VISIBLE : View.GONE);
        postButton.setEnabled(!show);
        addImageCard.setEnabled(!show);
        addVideoCard.setEnabled(!show);
    }

    private void loadAd() {
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
        hintHandler.removeCallbacksAndMessages(null);
    }
}