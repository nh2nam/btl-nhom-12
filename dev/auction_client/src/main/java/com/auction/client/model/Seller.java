package com.auction.client.model;

public class Seller extends User {
    // Seller có thể có thêm điểm uy tín (rating)
    private double rating;

    public Seller(int id, String username, String email, String passwordHash) {
        super(id, username, email, passwordHash);
        this.rating = 5.0; // Khởi tạo điểm uy tín mặc định
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }
// Generate Getter và Setter cho rating
}