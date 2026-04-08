package com.auction.model;

public class Electronics extends Item {
    private String brand;
    private String model;
    private int warrantyPeriodMonths;

    public Electronics(int id, String name, String description, double startingPrice,
                       String brand, String model, int warrantyPeriodMonths) {
        // Truyền các thông số cơ bản lên lớp cha (Item)
        super(id, name, description, startingPrice);

        // Gán các thông số riêng của đồ điện tử
        this.brand = brand;
        this.model = model;
        this.warrantyPeriodMonths = warrantyPeriodMonths;
    }

    public int getWarrantyPeriodMonths() {
        return warrantyPeriodMonths;
    }

    public void setWarrantyPeriodMonths(int warrantyPeriodMonths) {
        this.warrantyPeriodMonths = warrantyPeriodMonths;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }
// Generate Getter/Setter cho brand, model, warrantyPeriodMonths
}