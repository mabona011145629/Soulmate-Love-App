package com.mabona.mobilesystems.soulmate;

public class SearchItem {
    private int userId;
    private String fullName;
    private String firstName;
    private String lastName;
    private String gender;
    private int age;
    private String bio;
    private String location;
    private String profileImage; // Base64 for list display
    private String profileImagePath; // Raw path for RequestLoveActivity
    private boolean isOnline;
    private String profileType;
    private boolean hasPendingRequest;

    // Constructor
    public SearchItem(int userId, String fullName, String firstName, String lastName,
                      String gender, int age, String bio, String location,
                      String profileImage, String profileImagePath, boolean isOnline,
                      String profileType, boolean hasPendingRequest) {
        this.userId = userId;
        this.fullName = fullName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.gender = gender;
        this.age = age;
        this.bio = bio;
        this.location = location;
        this.profileImage = profileImage;
        this.profileImagePath = profileImagePath;
        this.isOnline = isOnline;
        this.profileType = profileType;
        this.hasPendingRequest = hasPendingRequest;
    }

    // Getters
    public int getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getGender() { return gender; }
    public int getAge() { return age; }
    public String getBio() { return bio; }
    public String getLocation() { return location; }
    public String getProfileImage() { return profileImage; }
    public String getProfileImagePath() { return profileImagePath; }
    public boolean isOnline() { return isOnline; }
    public String getProfileType() { return profileType; }
    public boolean hasPendingRequest() { return hasPendingRequest; }
}
/*package com.mabona.mobilesystems.soulmate;

public class SearchItem {
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
    private boolean hasPendingRequest;

    // Constructor
    public SearchItem(int userId, String fullName, String firstName, String lastName,
                      String gender, int age, String bio, String location,
                      String profileImage, boolean isOnline, String profileType,
                      boolean hasPendingRequest) {
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
        this.hasPendingRequest = hasPendingRequest;
    }

    // Getters
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
    public boolean hasPendingRequest() { return hasPendingRequest; }
}*/