package com.auction.service;

import com.auction.exception.AuctionException;

public interface IAuctionService {
    // Đổi kiểu trả về thành void và thêm throws
    void placeManualBid(int auctionId, int bidderId, double bidAmount) throws AuctionException;

    boolean registerAutoBid(int auctionId, int bidderId, double maxBidAmount, double increment);
    void processExpiredAuctions();
}