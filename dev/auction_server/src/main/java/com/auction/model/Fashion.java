package com.auction.model;

public class Fashion extends Item {
    public Fashion(int id, String name, String description, double startingPrice, String imagePath) {
        super(id, name, description, startingPrice, imagePath);
    }

    @Override
    public String getCategory() {
        return "Fashion";
    }
}