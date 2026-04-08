package com.auction.model;

public class Vehicle extends Item {
    private String make;  // Hãng xe (ví dụ: Toyota)
    private String model; // Dòng xe (ví dụ: Camry)
    private int year;     // Năm sản xuất
    private String vin;   // Số khung (Vehicle Identification Number)

    public Vehicle(int id, String name, String description, double startingPrice,
                   String make, String model, int year, String vin) {
        super(id, name, description, startingPrice);
        this.make = make;
        this.model = model;
        this.year = year;
        this.vin = vin;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }
// Nhớ Generate Getter và Setter
}