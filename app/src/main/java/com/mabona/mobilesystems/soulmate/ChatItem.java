package com.mabona.mobilesystems.soulmate;

public class ChatItem {
    private int userId;
    private String name;
    private String lastMessage;
    private String time;
    private String profileImage;
    private boolean isOnline;
    private boolean isAd;
    private boolean isEmptyState;  // NEW: For empty state messages

    // Constructor for regular chat items
    public ChatItem(int userId, String name, String lastMessage, String time,
                    String profileImage, boolean isOnline) {
        this.userId = userId;
        this.name = name;
        this.lastMessage = lastMessage;
        this.time = time;
        this.profileImage = profileImage;
        this.isOnline = isOnline;
        this.isAd = false;
        this.isEmptyState = false;  // NEW: Default false
    }

    // Constructor for ad items
    public ChatItem(boolean isAd) {
        this.isAd = isAd;
        this.isEmptyState = false;
    }

    // NEW: Constructor for empty state message
    public ChatItem(int userId, String name, String lastMessage, String time,
                    String profileImage, boolean isOnline, boolean isEmptyState) {
        this.userId = userId;
        this.name = name;
        this.lastMessage = lastMessage;
        this.time = time;
        this.profileImage = profileImage;
        this.isOnline = isOnline;
        this.isAd = false;
        this.isEmptyState = isEmptyState;
    }

    // NEW: Set as empty state
    public void setAsEmptyState(boolean isEmpty) {
        this.isEmptyState = isEmpty;
    }

    // NEW: Check if empty state
    public boolean isEmptyState() {
        return isEmptyState;
    }

    // Getters
    public int getUserId() { return userId; }
    public String getName() { return name; }
    public String getLastMessage() { return lastMessage; }
    public String getTime() { return time; }
    public String getProfileImage() { return profileImage; }
    public boolean isOnline() { return isOnline; }
    public boolean isAd() { return isAd; }
}