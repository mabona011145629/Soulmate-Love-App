package com.mabona.mobilesystems.soulmate;

import android.content.Context;
import android.content.SharedPreferences;

public class LoginManager {

    private static final String PREF_NAME = "SoulmatePrefs";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";

    // Save login after successful login
    public static void saveLogin(Context context, String email, int userId, String userName, String token) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putString(KEY_EMAIL, email);
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, userName);
        editor.putString(KEY_TOKEN, token);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    // Check if user is logged in
    public static boolean isLoggedIn(Context context) {
        return getPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false);
    }

    // Get saved email
    public static String getSavedEmail(Context context) {
        return getPrefs(context).getString(KEY_EMAIL, "");
    }

    // Get saved user ID
    public static int getSavedUserId(Context context) {
        return getPrefs(context).getInt(KEY_USER_ID, -1);
    }

    // Get saved user name
    public static String getSavedUserName(Context context) {
        return getPrefs(context).getString(KEY_USER_NAME, "");
    }

    // Get saved token
    public static String getSavedToken(Context context) {
        return getPrefs(context).getString(KEY_TOKEN, "");
    }

    // Force logout
    public static void forceLogout(Context context) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.clear();
        editor.apply();
    }

    // Clear all login data
    public static void clearAllData(Context context) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.clear();
        editor.apply();
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}