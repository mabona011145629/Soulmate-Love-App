package com.mabona.mobilesystems.soulmate;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegistrationActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 100;
    private static final int CAMERA_REQUEST = 103;
    private static final int PERMISSION_REQUEST_CODE = 200;

    private EditText emailEditText, passwordEditText, confirmPasswordEditText;
    private EditText firstNameEditText, lastNameEditText, bioEditText;
    private TextView dobTextView;
    private Spinner genderSpinner;
    private RadioGroup hideProfileRadioGroup;
    private RadioButton hideYesRadio, hideNoRadio;
    private CheckBox termsCheckBox;
    private Button registerButton, selectImageButton, takePhotoButton, pickDobButton;
    private ImageView profileImageView, heartImage1, heartImage2, heartImage3;
    private ProgressBar progressBar;
    private TextView progressText;
    private AdView adView;

    private String selectedImagePath = null;
    private Bitmap selectedBitmap = null;
    private String selectedGender = "male";
    private boolean hideProfile = false;
    private Calendar dobCalendar = Calendar.getInstance();
    private Uri cameraPhotoUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        initializeViews();
        setupAnimations();
        setupSpinner();
        setupClickListeners();
        loadAd();
    }

    private void initializeViews() {
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        firstNameEditText = findViewById(R.id.firstNameEditText);
        lastNameEditText = findViewById(R.id.lastNameEditText);
        bioEditText = findViewById(R.id.bioEditText);
        dobTextView = findViewById(R.id.dobTextView);
        genderSpinner = findViewById(R.id.genderSpinner);
        hideProfileRadioGroup = findViewById(R.id.hideProfileRadioGroup);
        hideYesRadio = findViewById(R.id.hideYesRadio);
        hideNoRadio = findViewById(R.id.hideNoRadio);
        termsCheckBox = findViewById(R.id.termsCheckBox);
        registerButton = findViewById(R.id.registerButton);
        selectImageButton = findViewById(R.id.selectImageButton);
        takePhotoButton = findViewById(R.id.takePhotoButton);
        pickDobButton = findViewById(R.id.pickDobButton);
        profileImageView = findViewById(R.id.profileImageView);
        progressBar = findViewById(R.id.progressBar);
        progressText = findViewById(R.id.progressText);
        adView = findViewById(R.id.adView);
        heartImage1 = findViewById(R.id.heartImage1);
        heartImage2 = findViewById(R.id.heartImage2);
        heartImage3 = findViewById(R.id.heartImage3);

        profileImageView.setImageResource(R.drawable.default_profile);
        dobCalendar.add(Calendar.YEAR, -18);
        updateDobTextView();
    }

    private void setupAnimations() {
        android.animation.ObjectAnimator floatAnimation1 = android.animation.ObjectAnimator.ofFloat(heartImage1, "translationY", -30f, 30f);
        floatAnimation1.setDuration(2000);
        floatAnimation1.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        floatAnimation1.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        floatAnimation1.start();

        android.animation.ObjectAnimator floatAnimation2 = android.animation.ObjectAnimator.ofFloat(heartImage2, "translationY", 30f, -30f);
        floatAnimation2.setDuration(2200);
        floatAnimation2.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        floatAnimation2.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        floatAnimation2.start();

        android.animation.ObjectAnimator floatAnimation3 = android.animation.ObjectAnimator.ofFloat(heartImage3, "translationX", -20f, 20f);
        floatAnimation3.setDuration(1800);
        floatAnimation3.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        floatAnimation3.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        floatAnimation3.start();
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.gender_options_full, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(adapter);

        genderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] genderValues = {"male", "female", "lesbian", "gay", "other"};
                if (position < genderValues.length) {
                    selectedGender = genderValues[position];
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedGender = "male";
            }
        });
    }

    private void setupClickListeners() {
        selectImageButton.setOnClickListener(v -> {
            // Request storage permission first
            if (checkAndRequestPermissions()) {
                openGallery();
            }
        });

        takePhotoButton.setOnClickListener(v -> {
            // Request camera and storage permissions first
            if (checkAndRequestPermissions()) {
                openCamera();
            }
        });

        pickDobButton.setOnClickListener(v -> showDatePickerDialog());

        hideProfileRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            hideProfile = checkedId == R.id.hideYesRadio;
        });

        registerButton.setOnClickListener(v -> {
            Animation shake = AnimationUtils.loadAnimation(RegistrationActivity.this, R.anim.shake);
            registerButton.startAnimation(shake);
            attemptRegistration();
        });

        TextView loginText = findViewById(R.id.loginText);
        loginText.setOnClickListener(v -> {
            Intent intent = new Intent(RegistrationActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            overridePendingTransition(R.anim.slide_out_left, R.anim.slide_in_right);
        });
    }

    private boolean checkAndRequestPermissions() {
        String[] permissions;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ - different permission structure
            permissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_MEDIA_IMAGES
            };
        } else {
            // Android 12 and below
            permissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };
        }

        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void openCamera() {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, "Soulmate_Profile_" + System.currentTimeMillis());
            values.put(MediaStore.Images.Media.DESCRIPTION, "Profile picture for Soulmate app");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cameraPhotoUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                File photoFile = createImageFile();
                if (photoFile != null) {
                    cameraPhotoUri = FileProvider.getUriForFile(this,
                            getPackageName() + ".fileprovider", photoFile);
                }
            }

            if (cameraPhotoUri != null) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoUri);
                startActivityForResult(intent, CAMERA_REQUEST);
            } else {
                Toast.makeText(this, "Error creating photo file", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Camera error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new java.util.Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private Bitmap fixImageRotation(String imagePath) {
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        if (bitmap == null) return null;

        try {
            ExifInterface exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                default:
                    return bitmap;
            }

            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (Exception e) {
            e.printStackTrace();
            return bitmap;
        }
    }

    private String saveBitmapToFile(Bitmap bitmap) {
        try {
            String fileName = "profile_" + System.currentTimeMillis() + ".jpg";
            File cacheFile = new File(getCacheDir(), fileName);
            FileOutputStream fos = new FileOutputStream(cacheFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            return cacheFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getRealPathFromUri(Uri uri) {
        try {
            String fileName = "profile_" + System.currentTimeMillis() + ".jpg";
            File cacheFile = new File(getCacheDir(), fileName);

            ContentResolver resolver = getContentResolver();
            InputStream inputStream = resolver.openInputStream(uri);
            OutputStream outputStream = new FileOutputStream(cacheFile);

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
                Uri selectedImageUri = data.getData();
                selectedImagePath = getRealPathFromUri(selectedImageUri);

                if (selectedImagePath != null) {
                    // Fix rotation for gallery images too
                    selectedBitmap = fixImageRotation(selectedImagePath);
                    if (selectedBitmap == null) {
                        selectedBitmap = BitmapFactory.decodeFile(selectedImagePath);
                    }
                    profileImageView.setImageBitmap(selectedBitmap);

                    // Re-save fixed rotation image
                    String fixedPath = saveBitmapToFile(selectedBitmap);
                    if (fixedPath != null) {
                        selectedImagePath = fixedPath;
                    }

                    Animation bounce = AnimationUtils.loadAnimation(this, R.anim.bounce);
                    profileImageView.startAnimation(bounce);
                } else {
                    Toast.makeText(this, "Failed to get image path", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == CAMERA_REQUEST && cameraPhotoUri != null) {
                selectedImagePath = getRealPathFromUri(cameraPhotoUri);

                if (selectedImagePath != null) {
                    // Fix rotation for camera images (important for vertical orientation)
                    selectedBitmap = fixImageRotation(selectedImagePath);
                    if (selectedBitmap == null) {
                        selectedBitmap = BitmapFactory.decodeFile(selectedImagePath);
                    }
                    profileImageView.setImageBitmap(selectedBitmap);

                    // Re-save fixed rotation image
                    String fixedPath = saveBitmapToFile(selectedBitmap);
                    if (fixedPath != null) {
                        selectedImagePath = fixedPath;
                    }

                    Animation bounce = AnimationUtils.loadAnimation(this, R.anim.bounce);
                    profileImageView.startAnimation(bounce);
                } else {
                    Toast.makeText(this, "Failed to get photo", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                // Permissions granted, now we need to know which button was clicked
                // For simplicity, we'll let the button click handler open the picker again
                Toast.makeText(this, "Permissions granted. Click button again.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permissions required to select images", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    dobCalendar.set(Calendar.YEAR, year);
                    dobCalendar.set(Calendar.MONTH, month);
                    dobCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDobTextView();

                    Calendar today = Calendar.getInstance();
                    int age = today.get(Calendar.YEAR) - dobCalendar.get(Calendar.YEAR);
                    if (today.get(Calendar.DAY_OF_YEAR) < dobCalendar.get(Calendar.DAY_OF_YEAR)) {
                        age--;
                    }

                    if (age < 18) {
                        Toast.makeText(RegistrationActivity.this,
                                "You must be at least 18 years old", Toast.LENGTH_SHORT).show();
                        dobCalendar.add(Calendar.YEAR, 18 - age);
                        updateDobTextView();
                    }
                },
                dobCalendar.get(Calendar.YEAR),
                dobCalendar.get(Calendar.MONTH),
                dobCalendar.get(Calendar.DAY_OF_MONTH)
        );

        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.YEAR, -18);
        datePickerDialog.getDatePicker().setMaxDate(maxDate.getTimeInMillis());

        Calendar minDate = Calendar.getInstance();
        minDate.add(Calendar.YEAR, -100);
        datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());

        datePickerDialog.show();
    }

    private void updateDobTextView() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        dobTextView.setText(sdf.format(dobCalendar.getTime()));
    }

    private void attemptRegistration() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String bio = bioEditText.getText().toString().trim();
        String dob = dobTextView.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            showError(emailEditText, "Email is required");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(emailEditText, "Enter a valid email");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            showError(passwordEditText, "Password is required");
            return;
        }

        if (password.length() < 6) {
            showError(passwordEditText, "Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError(confirmPasswordEditText, "Passwords do not match");
            return;
        }

        if (TextUtils.isEmpty(firstName)) {
            showError(firstNameEditText, "First name is required");
            return;
        }

        if (TextUtils.isEmpty(lastName)) {
            showError(lastNameEditText, "Last name is required");
            return;
        }

        if (selectedImagePath == null || !new File(selectedImagePath).exists()) {
            Toast.makeText(this, "Profile image is required", Toast.LENGTH_SHORT).show();
            Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
            profileImageView.startAnimation(shake);
            return;
        }

        if (!termsCheckBox.isChecked()) {
            Toast.makeText(this, "You must accept terms and conditions", Toast.LENGTH_SHORT).show();
            Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
            termsCheckBox.startAnimation(shake);
            return;
        }

        showProgress(true);

        OkHttpClient client = new OkHttpClient();

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("email", email)
                .addFormDataPart("password", password)
                .addFormDataPart("first_name", firstName)
                .addFormDataPart("last_name", lastName)
                .addFormDataPart("gender", selectedGender)
                .addFormDataPart("date_of_birth", dob)
                .addFormDataPart("hide_profile", hideProfile ? "1" : "0");

        if (!TextUtils.isEmpty(bio)) {
            builder.addFormDataPart("bio", bio);
        }

        File imageFile = new File(selectedImagePath);
        if (imageFile.exists()) {
            builder.addFormDataPart("profile_image", imageFile.getName(),
                    RequestBody.create(MediaType.parse("image/jpeg"), imageFile));
        }

        RequestBody requestBody = builder.build();

        Request request = new Request.Builder()
                .url("https://mabona.firstsuninvestment.com/soulmate/register.php")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(RegistrationActivity.this,
                            "Network error. Please try again.", Toast.LENGTH_SHORT).show();
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

                        if (success) {
                            String message = jsonResponse.getString("message");
                            Toast.makeText(RegistrationActivity.this, message, Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(RegistrationActivity.this, MainActivity.class);
                            intent.putExtra("REGISTERED_EMAIL", email);
                            startActivity(intent);
                            finish();
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        } else {
                            String errorMsg = jsonResponse.getString("message");
                            Toast.makeText(RegistrationActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(RegistrationActivity.this,
                                "Registration failed. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void showError(EditText editText, String error) {
        editText.setError(error);
        editText.requestFocus();
        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
        editText.startAnimation(shake);
    }

    private void showProgress(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            progressText.setVisibility(View.VISIBLE);
            registerButton.setEnabled(false);
            selectImageButton.setEnabled(false);
            takePhotoButton.setEnabled(false);
            pickDobButton.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            progressText.setVisibility(View.GONE);
            registerButton.setEnabled(true);
            selectImageButton.setEnabled(true);
            takePhotoButton.setEnabled(true);
            pickDobButton.setEnabled(true);
        }
    }

    private void loadAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (adView != null) {
            adView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adView != null) {
            adView.destroy();
        }
    }
}

