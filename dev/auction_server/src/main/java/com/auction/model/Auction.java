package com.auction.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Auction extends Entity {
    private int sellerId;
    private int itemId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;

    // Biến phụ trợ cho logic đấu giá
    private double currentHighestBid;
    private int currentWinnerId;
    private int version;

    // Thời hạn thanh toán (chỉ có giá trị khi status = PENDING_PAYMENT)
    private LocalDateTime paymentDeadline;

    // Danh sách lưu lại lịch sử đặt giá
    private List<BidTransaction> bidHistory;
    // Danh sách lưu những người dùng đang bật Auto-Bid
    private List<Bidder> autoBidders;



    public Auction(int id, int sellerId, int itemId, LocalDateTime startTime, LocalDateTime endTime, double startingPrice) {
        super(id);
        this.sellerId = sellerId;
        this.itemId = itemId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = "OPEN";

        this.currentHighestBid = startingPrice;
        this.currentWinnerId = -1;
        this.version = 1;


        this.bidHistory = new ArrayList<>();
        this.autoBidders = new ArrayList<>();
    }


    public boolean isOpen() {
        return "RUNNING".equals(status) && LocalDateTime.now().isBefore(endTime);
    }


    public List<BidTransaction> getBidHistory() {
        return bidHistory;
    }

    public int getSellerId() { return sellerId; }
    public void setSellerId(int sellerId) { this.sellerId = sellerId; }
    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(double currentHighestBid) { this.currentHighestBid = currentHighestBid; }
    public int getCurrentWinnerId() { return currentWinnerId; }
    public void setCurrentWinnerId(int currentWinnerId) { this.currentWinnerId = currentWinnerId; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public List<Bidder> getAutoBidders() {
        return autoBidders;
    }

    public LocalDateTime getPaymentDeadline() { return paymentDeadline; }
    public void setPaymentDeadline(LocalDateTime paymentDeadline) { this.paymentDeadline = paymentDeadline; }
}
