package com.auction.model;

public class Electronics extends Item {
    public Electronics(int id, String name, String description, double startingPrice, String imagePath) {
        super(id, name, description, startingPrice, imagePath);
    }

    @Override
    public String getCategory() {
        return "Electronics";
    }
}