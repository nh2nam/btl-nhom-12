package com.auction.client.model;

public class Arts extends Item {
    public Arts(int id, String name, String description, double startingPrice, String imagePath) {
        super(id, name, description, startingPrice, imagePath);
    }

    @Override
    public String getCategory() {
        return "Arts";
    }
}