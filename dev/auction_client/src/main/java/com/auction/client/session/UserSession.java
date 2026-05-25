package com.auction.client.session;

public class UserSession {
    private static UserSession instance;

    private int userId;
    private String username;
    private String displayName;
    private String role;
    private String email;
    private String phone;

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    // --- Getters & Setters ---
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDisplayName() { return displayName != null ? displayName : username; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getEmail() { return email != null ? email : ""; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone != null ? phone : ""; }
    public void setPhone(String phone) { this.phone = phone; }

    public void login(int id, String username, String displayName, String role) {
        this.userId = id;
        this.username = username;
        this.displayName = displayName;
        this.role = role;
    }

    public boolean isLoggedIn() {
        return this.username != null && !this.username.isEmpty();
    }

    public void logout() {
        this.userId = 0;
        this.username = null;
        this.displayName = null;
        this.role = null;
        this.email = null;
        this.phone = null;
    }
}