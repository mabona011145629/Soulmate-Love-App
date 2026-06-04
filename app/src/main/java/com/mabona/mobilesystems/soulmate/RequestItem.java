package com.mabona.mobilesystems.soulmate;

public class RequestItem {
    private int requestId;
    private String requestType;
    private int userId;
    private String fullName;
    private String firstName;
    private String lastName;
    private String gender;
    private int age;
    private String bio;
    private String location;
    private String profileImage;  // This is the PATH, not full URL
    private boolean isOnline;
    private String profileType;
    private String requestedAt;

    public RequestItem(int requestId, String requestType, int userId, String fullName,
                       String firstName, String lastName, String gender, int age,
                       String bio, String location, String profileImage,
                       boolean isOnline, String profileType, String requestedAt) {
        this.requestId = requestId;
        this.requestType = requestType;
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
        this.requestedAt = requestedAt;
    }

    // Getters
    public int getRequestId() { return requestId; }
    public String getRequestType() { return requestType; }
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
    public String getRequestedAt() { return requestedAt; }
}