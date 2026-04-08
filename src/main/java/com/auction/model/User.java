package com.auction.model;

public abstract class User extends Entity {
    private String username;
    private String email;
    private String passwordHash;

    public User(int id, String username, String email, String passwordHash) {
        super(id); // Gọi hàm khởi tạo id từ Entity
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
// Bạn dùng chuột phải -> Generate -> Getter and Setter để tạo cho 3 biến này nhé!
}