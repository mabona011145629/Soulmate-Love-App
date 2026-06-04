package com.mabona.mobilesystems.soulmate;

import android.content.Context;
import android.content.SharedPreferences;

public class LoginManager {

    private static final String PREF_NAME = "SoulmatePrefs";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_TOKEN = "auth_token";

    // Save login after successful login
    public static void saveLogin(Context context, String email, String token) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putString(KEY_EMAIL, email);
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

    // Get saved token
    public static String getSavedToken(Context context) {
        return getPrefs(context).getString(KEY_TOKEN, "");
    }

    // Update only email (when password changes)
    public static void updateEmail(Context context, String email) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putString(KEY_EMAIL, email);
        editor.apply();
    }

    // Force logout (when password changes or user logs out)
    public static void forceLogout(Context context) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.remove(KEY_TOKEN);
        editor.apply();
    }

    // Clear all login data (when app is uninstalled or user clears data)
    public static void clearAllData(Context context) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.clear();
        editor.apply();
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}