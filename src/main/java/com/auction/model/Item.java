package com.auction.model;

public abstract class Item extends Entity {
    private String name;
    private String description;
    private double startingPrice;

    public Item(int id, String name, String description, double startingPrice) {
        super(id); // Gọi id từ Entity
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }
    // Nhấn Alt + Insert -> chọn Getter and Setter để IntelliJ tự tạo code cho name, description, startingPrice nhé!
}