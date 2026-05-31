package com.auction.service;

import com.auction.model.Auction;
import com.auction.util.AuctionManager;
import com.auction.model.Electronics;
import com.auction.model.Item;

// Import thêm các Exception mới tạo
import com.auction.exception.AuctionException;
import com.auction.exception.BidTooLowException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

// Import các hàm kiểm tra của JUnit
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AuctionServiceImplTest {

    private AuctionServiceImpl auctionService;
    private Auction testAuction;

    // Hàm này sẽ tự động chạy TRƯỚC mỗi bài test để dọn dẹp và chuẩn bị dữ liệu mới
    @BeforeEach
    public void setUp() {
        auctionService = new AuctionServiceImpl();

        Item laptop = new Electronics(1, "Laptop Dell", "Moi", 500.0, "/images/laptop.png");

        testAuction = new Auction(100, 1, laptop.getId(), LocalDateTime.now(), LocalDateTime.now().plusDays(1), laptop.getStartingPrice());
        testAuction.setStatus("RUNNING");
        AuctionManager.getInstance().addAuction(testAuction);
    }
    // ==========================================
    // BÀI TEST SỐ 1: CHẶN GIÁ THẤP
    // ==========================================
    @Test
    public void testPlaceBid_TooLow_ShouldFail() {
        // Dùng assertThrows để bắt lỗi: Nếu code bên trong { ... } ném ra lỗi BidTooLowException thì bài test Pass
        assertThrows(BidTooLowException.class, () -> {
            // Hành động: Bidder số 99 đặt giá 400$ (trong khi giá khởi điểm đang là 500$)
            auctionService.placeManualBid(testAuction.getId(), 99, 400.0);
        }, "Hệ thống phải báo lỗi BidTooLowException khi đặt giá thấp!");
    }

    // ==========================================
    // BÀI TEST SỐ 2: ĐẶT GIÁ CHUẨN XÁC
    // ==========================================
    @Test
    public void testPlaceBid_ValidAmount_ShouldSucceed() throws AuctionException {
        // Thêm "throws AuctionException" ở dòng trên để báo cho JUnit biết hàm này có thể văng lỗi

        // Hành động: Bidder số 88 đặt giá 600$ (hợp lệ vì giá khởi điểm là 500$)
        // Không gán vào BidResult nữa, nếu code chạy trơn tru không ném ra Exception nào tức là đã đặt giá thành công
        auctionService.placeManualBid(testAuction.getId(), 88, 600.0);

        // Kiểm tra Giá mới và ID người thắng
        assertEquals(600.0, testAuction.getCurrentHighestBid(), "Giá hiện tại phải được cập nhật lên 600");
        assertEquals(88, testAuction.getCurrentWinnerId(), "Người thắng hiện tại phải là Bidder 88");
    }

    // ==========================================
    // BÀI TEST SỐ 3: HACK THỜI GIAN (ANTI-SNIPING)
    // ==========================================
    @Test
    public void testPlaceBid_AntiSniping_ShouldExtendEndTime() throws AuctionException {
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