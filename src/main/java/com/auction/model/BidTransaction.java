package com.auction.model;

import java.time.LocalDateTime;

public class BidTransaction extends Entity {
    private int auctionId;
    private int bidderId;
    private double amount;
    private LocalDateTime bidTime;

    public BidTransaction(int id, int auctionId, int bidderId, double amount, LocalDateTime bidTime) {
        super(id);
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.bidTime = bidTime;
    }

    public int getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(int auctionId) {
        this.auctionId = auctionId;
    }

    public int getBidderId() {
        return bidderId;
    }

    public void setBidderId(int bidderId) {
        this.bidderId = bidderId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    public void setBidTime(LocalDateTime bidTime) {
        this.bidTime = bidTime;
    }
// Generate Getter và Setter
}