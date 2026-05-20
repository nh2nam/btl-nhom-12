package com.auction.client.model;

public class RealEstate extends Item {
    public RealEstate(int id, String name, String description, double startingPrice, String imagePath) {
        super(id, name, description, startingPrice, imagePath);
    }

    @Override
    public String getCategory() {
        return "Real estate";
    }
}