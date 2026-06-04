package com.auction.model;

public class Bidder extends User {
    private boolean autoBidEnabled;
    private double maxAutoBidAmount;
    private double autoBidIncrement;

    public Bidder(int id, String username, String email, String passwordHash) {
        super(id, username, email, passwordHash);
        this.autoBidEnabled = false;
    }


    public boolean isAutoBidEnabled() { return autoBidEnabled; }
    public void setAutoBidEnabled(boolean autoBidEnabled) { this.autoBidEnabled = autoBidEnabled; }

    public double getMaxAutoBidAmount() { return maxAutoBidAmount; }
    public void setMaxAutoBidAmount(double maxAutoBidAmount) { this.maxAutoBidAmount = maxAutoBidAmount; }

    public double getAutoBidIncrement() { return autoBidIncrement; }
    public void setAutoBidIncrement(double autoBidIncrement) { this.autoBidIncrement = autoBidIncrement; }
}