package com.auction.model;

public class Art extends Item {
    private String artist;
    private int yearCreated;
    private String medium; // Chất liệu (ví dụ: Sơn dầu, màu nước...)

    public Art(int id, String name, String description, double startingPrice,
               String artist, int yearCreated, String medium) {
        super(id, name, description, startingPrice);
        this.artist = artist;
        this.yearCreated = yearCreated;
        this.medium = medium;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public int getYearCreated() {
        return yearCreated;
    }

    public void setYearCreated(int yearCreated) {
        this.yearCreated = yearCreated;
    }

    public String getMedium() {
        return medium;
    }

    public void setMedium(String medium) {
        this.medium = medium;
    }
// Nhớ dùng chuột phải -> Generate -> Getter and Setter nhé
}