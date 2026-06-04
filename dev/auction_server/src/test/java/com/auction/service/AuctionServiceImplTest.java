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
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class AuctionServiceImplTest {

    private AuctionServiceImpl auctionService;
    private Auction testAuction;

    @BeforeEach
    public void setUp() {
        auctionService = new AuctionServiceImpl();

        Item laptop = new Electronics(1, "Laptop Dell", "Moi", 500.0, "/images/laptop.png");

        testAuction = new Auction(100, 1, laptop.getId(), LocalDateTime.now(), LocalDateTime.now().plusDays(1), laptop.getStartingPrice());

        // Đã hoàn trả lại kiểu String nguyên bản của em
        testAuction.setStatus("RUNNING");

        AuctionManager.getInstance().addAuction(testAuction);
    }

    // ==========================================
    // BÀI TEST SỐ 1: CHẶN GIÁ THẤP
    // ==========================================
    @Test
    public void testPlaceBid_TooLow_ShouldFail() {
        assertThrows(BidTooLowException.class, () -> {
            auctionService.placeManualBid(testAuction.getId(), 99, 400.0);
        }, "Hệ thống phải báo lỗi BidTooLowException khi đặt giá thấp!");
    }

    // ==========================================
    // BÀI TEST SỐ 2: ĐẶT GIÁ CHUẨN XÁC
    // ==========================================
    @Test
    public void testPlaceBid_ValidAmount_ShouldSucceed() throws AuctionException {
        auctionService.placeManualBid(testAuction.getId(), 88, 600.0);

        assertEquals(600.0, testAuction.getCurrentHighestBid(), "Giá hiện tại phải được cập nhật lên 600");
        assertEquals(88, testAuction.getCurrentWinnerId(), "Người thắng hiện tại phải là Bidder 88");
    }

    // ==========================================
    // BÀI TEST SỐ 3: HACK THỜI GIAN (ANTI-SNIPING)
    // ==========================================
    @Test
    public void testPlaceBid_AntiSniping_ShouldExtendEndTime() throws AuctionException {
        LocalDateTime nearlyEndTime = LocalDateTime.now().plusSeconds(10);
        testAuction.setEndTime(nearlyEndTime);

        auctionService.placeManualBid(testAuction.getId(), 3, 700.0);

        assertTrue(testAuction.getEndTime().isAfter(nearlyEndTime),
                "Lỗi: Thuật toán Anti-sniping chưa chạy, thời gian không được gia hạn!");
    }

    // ==========================================
    // BÀI TEST SỐ 4: ĐÁNH CHẶN KHI PHIÊN ĐÃ KẾT THÚC
    // ==========================================
    @Test
    public void testPlaceBid_AuctionClosed_ShouldFail() {
        // Cố tình ép phiên đấu giá về trạng thái Đã kết thúc bằng String
        testAuction.setStatus("FINISHED");

        // Đặt giá lúc này bắt buộc phải văng lỗi AuctionException
        assertThrows(AuctionException.class, () -> {
            auctionService.placeManualBid(testAuction.getId(), 77, 800.0);
        }, "Hệ thống phải báo lỗi khi cố tình đặt giá vào phiên đã đóng!");
    }

    // ==========================================
    // BÀI TEST SỐ 5: TEST CHỨC NĂNG AUTO-BID
    // ==========================================
    @Test
    public void testRegisterAutoBid_Valid_ShouldSucceed() {
        // Hành động: Đăng ký Auto-bid cho Bidder 55, max 1000$, mỗi bước 50$
        boolean result = auctionService.registerAutoBid(testAuction.getId(), 55, 1000.0, 50.0);

        // Kiểm tra: Hàm đăng ký phải trả về True
        assertTrue(result, "Hệ thống phải trả về True khi đăng ký Auto-Bid hợp lệ");

        // Thử đăng ký cho 1 phiên không tồn tại (ID 999), hệ thống phải từ chối (trả về False)
        boolean invalidResult = auctionService.registerAutoBid(999, 55, 1000.0, 50.0);
        assertFalse(invalidResult, "Hệ thống phải trả về False khi ID phiên đấu giá không tồn tại");
    }
    // ==========================================
    // BÀI TEST SỐ 6: ĐẤU GIÁ ĐỒNG THỜI (CONCURRENCY)
    // ==========================================
    @Test
    public void testConcurrentBidding_ShouldPreventRaceCondition() throws InterruptedException {
        int numberOfThreads = 10; // Mô phỏng 10 người dùng cùng đánh 1 lúc
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads); // Dùng cái này để đồng bộ các luồng cùng xuất phát

        // Biến đếm số người đặt giá thành công và thất bại an toàn trong môi trường đa luồng
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // Tạo ra 10 người dùng cùng cố gắng đặt chung 1 mức giá là 1000$ vào cùng 1 mili-giây
        for (int i = 0; i < numberOfThreads; i++) {
            final int bidderId = i + 10; // Đặt ID từ 10 đến 19
            executor.execute(() -> {
                try {
                    auctionService.placeManualBid(testAuction.getId(), bidderId, 1000.0);
                    successCount.incrementAndGet(); // Nếu qua được cửa, tăng biến đếm thành công
                } catch (AuctionException e) {
                    failCount.incrementAndGet();    // Nếu bị văng lỗi (do chậm chân hơn), tăng biến đếm thất bại
                } finally {
                    latch.countDown(); // Luồng chạy xong thì báo cáo
                }
            });
        }

        latch.await(); // Chờ cho cả 10 luồng đánh nhau xong xuôi mới chạy tiếp
        executor.shutdown();

        // KIỂM TRA CHÉO (ASSERTIONS)
        // Nhờ có từ khóa 'synchronized' trong code của em, hệ thống sẽ bắt 10 người này phải xếp hàng.
        // Chắc chắn chỉ có ĐÚNG 1 NGƯỜI lọt vào đầu tiên là đặt được 1000$ thành công.
        // 9 người còn lại khi được vào sẽ thấy giá đã là 1000$ rồi -> văng lỗi BidTooLowException.
        assertEquals(1, successCount.get(), "Chỉ có duy nhất 1 người được đặt giá 1000$ thành công!");
        assertEquals(9, failCount.get(), "9 người còn lại phải bị từ chối do Race Condition đã được chặn!");
        assertEquals(1000.0, testAuction.getCurrentHighestBid(), "Giá cuối cùng trên hệ thống phải là 1000$");
    }
}

