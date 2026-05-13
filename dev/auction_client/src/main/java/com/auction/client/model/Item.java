package com.auction.client.model;

import java.util.Map;

public class Item {
    private String name;
    private double startingPrice;
    private String imagePath;
    private String description;

    public Item(String name, double price, String imagePath, String descrpition) {
        this.name = name;
        this.startingPrice = price;
        this.imagePath = imagePath;
        this.description = descrpition;
    }

    // Getters
    public String getName() { return name; }
    public double getPrice() { return startingPrice; }
    public String getImagePath() { return imagePath; }
    public String getDescription() { return description; }

    //Setter
    public void setName(String name) { this.name = name; }
    public void setPrice(double price) { this.startingPrice = price; }
    public void setImagePath(String imagePath) {this.imagePath = imagePath; }
    public void setDescrpition (String descrpition) { this.description = descrpition; }
}