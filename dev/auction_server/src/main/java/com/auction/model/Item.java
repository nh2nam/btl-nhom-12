package com.auction.model;

public abstract class Item extends Entity {
    private String name;
    private String description;
    private double startingPrice;
    private String imagePath;

    public Item(int id, String name, String description, double startingPrice, String imagePath) {
        super(id);
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.imagePath = imagePath;
    }
    public abstract String getCategory();

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
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

}