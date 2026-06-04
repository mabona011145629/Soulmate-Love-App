package com.mabona.mobilesystems.soulmate;

public class ReplyItem {
    private int replyId;
    private String requestType;
    private String requestStatus;
    private String respondedAt;
    private int userId;
    private String fullName;
    private String firstName;
    private String lastName;
    private String gender;
    private int age;
    private String bio;
    private String location;
    private String profileImage;
    private boolean isOnline;
    private String profileType;
    private boolean canViewProfile;

    public ReplyItem(int replyId, String requestType, String requestStatus, String respondedAt,
                     int userId, String fullName, String firstName, String lastName,
                     String gender, int age, String bio, String location,
                     String profileImage, boolean isOnline, String profileType, boolean canViewProfile) {
        this.replyId = replyId;
        this.requestType = requestType;
        this.requestStatus = requestStatus;
        this.respondedAt = respondedAt;
        this.userId = userId;
        this.fullName = fullName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.gender = gender;
        this.age = age;
        this.bio = bio;
        this.location = location;
        this.profileImage = profileImage;
        this.isOnline = isOnline;
        this.profileType = profileType;
        this.canViewProfile = canViewProfile;
    }

    // Getters
    public int getReplyId() { return replyId; }
    public String getRequestType() { return requestType; }
    public String getRequestStatus() { return requestStatus; }
    public String getRespondedAt() { return respondedAt; }
    public int getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getGender() { return gender; }
    public int getAge() { return age; }
    public String getBio() { return bio; }
    public String getLocation() { return location; }
    public String getProfileImage() { return profileImage; }
    public boolean isOnline() { return isOnline; }
    public String getProfileType() { return profileType; }
    public boolean canViewProfile() { return canViewProfile; }
}