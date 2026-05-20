package com.auction.client.model;

public class OtherItem extends Item {
    public OtherItem(int id, String name, String description, double startingPrice, String imagePath) {
        super(id, name, description, startingPrice, imagePath);
    }

    @Override
    public String getCategory() {
        return "Other";
    }
}