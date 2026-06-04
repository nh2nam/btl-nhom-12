package com.auction.model;

public class Seller extends User {
    private double rating;

    public Seller(int id, String username, String email, String passwordHash) {
        super(id, username, email, passwordHash);
        this.rating = 5.0;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

}