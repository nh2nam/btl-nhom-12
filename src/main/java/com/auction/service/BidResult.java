package com.auction.service;

public enum BidResult {
    SUCCESS,                // Đặt giá thành công
    BID_TOO_LOW,            // Lỗi: Giá đặt thấp hơn hoặc bằng giá hiện tại
    AUCTION_CLOSED,         // Lỗi: Phiên đấu giá đã kết thúc
    CONCURRENCY_ERROR,      // Lỗi: Có người khác vừa nhanh tay đặt giá trước (Cần load lại)
    INVALID_AUCTION         // Lỗi: Không tìm thấy phiên đấu giá
}