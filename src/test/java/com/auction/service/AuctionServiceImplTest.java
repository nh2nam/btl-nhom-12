package com.auction.service;

import com.auction.model.Auction;
import com.auction.model.Electronics;
import com.auction.util.AuctionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

// Import các hàm kiểm tra của JUnit
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuctionServiceImplTest {

    private AuctionServiceImpl auctionService;
    private Auction testAuction;

    // Hàm này sẽ tự động chạy TRƯỚC mỗi bài test để dọn dẹp và chuẩn bị dữ liệu mới
    @BeforeEach
    public void setUp() {
        auctionService = new AuctionServiceImpl();

        // 1. Tạo một món đồ giá khởi điểm là 500$
        Electronics laptop = new Electronics(1, "Laptop Dell", "Mới", 500.0, "Dell", "XPS", 12);

        // 2. Tạo phiên đấu giá kéo dài 1 ngày
        testAuction = new Auction(100, 1, laptop.getId(), LocalDateTime.now(), LocalDateTime.now().plusDays(1), laptop.getStartingPrice());
        testAuction.setStatus("RUNNING");

        // 3. Đưa vào Manager
        AuctionManager.getInstance().addAuction(testAuction);
    }

    // ==========================================
    // BÀI TEST SỐ 1: CHẶN GIÁ THẤP (Ở ĐÂY NÀY!)
    // ==========================================
    @Test
    public void testPlaceBid_TooLow_ShouldFail() {
        // Hành động: Bidder số 99 đặt giá 400$ (trong khi giá khởi điểm đang là 500$)
        BidResult result = auctionService.placeManualBid(testAuction.getId(), 99, 400.0);

        // Kiểm tra kết quả: Kỳ vọng kết quả trả về CHÍNH XÁC là BID_TOO_LOW
        assertEquals(BidResult.BID_TOO_LOW, result, "Hệ thống phải báo lỗi BID_TOO_LOW khi đặt giá thấp!");
    }

    // ==========================================
    // BÀI TEST SỐ 2: ĐẶT GIÁ CHUẨN XÁC
    // ==========================================
    @Test
    public void testPlaceBid_ValidAmount_ShouldSucceed() {
        // Hành động: Bidder số 88 đặt giá 600$ (hợp lệ vì giá khởi điểm là 500$)
        BidResult result = auctionService.placeManualBid(testAuction.getId(), 88, 600.0);

        // Kiểm tra 3 thứ: Trạng thái, Giá mới, và ID người thắng
        assertEquals(BidResult.SUCCESS, result, "Hệ thống phải trả về SUCCESS");
        assertEquals(600.0, testAuction.getCurrentHighestBid(), "Giá hiện tại phải được cập nhật lên 600");
        assertEquals(88, testAuction.getCurrentWinnerId(), "Người thắng hiện tại phải là Bidder 88");
    }

    // ==========================================
    // BÀI TEST SỐ 3: HACK THỜI GIAN (ANTI-SNIPING)
    // ==========================================
    @Test
    public void testPlaceBid_AntiSniping_ShouldExtendEndTime() {
        // 1. Dùng code "hack" thời gian: Ép giờ kết thúc chỉ còn 10 giây nữa
        LocalDateTime nearlyEndTime = LocalDateTime.now().plusSeconds(10);
        testAuction.setEndTime(nearlyEndTime);

        // 2. Hành động: Bidder 3 nhảy vào "cắn trộm" giá 700$ ở phút chót
        auctionService.placeManualBid(testAuction.getId(), 3, 700.0);

        // 3. Kiểm tra: Giờ kết thúc MỚI có vượt qua cái mốc 10 giây lúc nãy không?
        assertTrue(testAuction.getEndTime().isAfter(nearlyEndTime),
                "Lỗi: Thuật toán Anti-sniping chưa chạy, thời gian không được gia hạn!");
    }
}