package com.auction.client.session;

public class UserSession {
    private static UserSession instance;

    // Các thông tin cơ bản cần lưu khi User đăng nhập
    private int userId;
    private String username;
    private String displayName;
    private String role;

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

    public void login(int id, String username, String displayName, String role) {
        this.userId = id;
        this.username = username;
        this.displayName = displayName;
        this.role = role;
    }

    // Hàm kiểm tra trạng thái đăng nhập
    public boolean isLoggedIn() {
        // Nếu username khác null nghĩa là người dùng đã đăng nhập thành công
        return this.username != null && !this.username.isEmpty();
    }

    // Hàm dùng để xóa dữ liệu khi người dùng bấm Đăng xuất
    public void logout() {
        this.userId = 0;
        this.username = null;
        this.displayName = null;
        this.role = null;
    }
}