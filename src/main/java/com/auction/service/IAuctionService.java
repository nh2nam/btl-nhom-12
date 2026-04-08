package com.auction.service;

public interface IAuctionService {

    // 1. Chức năng đặt giá thủ công
    BidResult placeManualBid(int auctionId, int bidderId, double bidAmount);

    // 2. Chức năng đăng ký đấu giá tự động (Nâng cao)
    boolean registerAutoBid(int auctionId, int bidderId, double maxBidAmount, double increment);

    // 3. Chức năng quét và đóng các phiên đã hết giờ (Server sẽ gọi hàm này liên tục)
    void processExpiredAuctions();
}