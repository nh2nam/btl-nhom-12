package com.auction;

import com.auction.network.ClientHandler;
import com.auction.service.AuctionServiceImpl;
import com.auction.service.IAuctionService;
import com.auction.util.AuctionManager;
import com.auction.util.ItemManager;
import com.auction.util.UserManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        System.out.println("--- KHỞI ĐỘNG SERVER ĐẤU GIÁ ---");

        // 1. Đọc port từ config
        int port = readPort();

        // 2. Đồng bộ dữ liệu từ MySQL lên RAM
        System.out.println("Đang kết nối Database và tải dữ liệu...");
        try {
            UserManager.getInstance();
            ItemManager.getInstance();
            AuctionManager.getInstance();
            System.out.println("✅ Khởi tạo hệ thống thành công. Sẵn sàng hoạt động!");
        } catch (Exception e) {
            System.err.println("❌ Lỗi nghiêm trọng khi khởi động Server: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // 3. Scheduler tự động đóng phiên hết hạn — chạy mỗi 30 giây
        IAuctionService auctionService = new AuctionServiceImpl();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                auctionService.processExpiredAuctions();
            } catch (Exception e) {
                LOGGER.warning("⚠️ Lỗi khi kiểm tra phiên hết hạn: " + e.getMessage());
            }
        }, 30, 30, TimeUnit.SECONDS);
        System.out.println("⏰ Scheduler kiểm tra phiên hết hạn đã khởi động (mỗi 30 giây).");

        // 4. Mở ServerSocket và chấp nhận kết nối từ Client
        ExecutorService threadPool = Executors.newFixedThreadPool(50);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("🌐 Server đang lắng nghe tại port " + port + " ...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(new ClientHandler(clientSocket));
            }

        } catch (IOException e) {
            LOGGER.severe("❌ Không thể mở ServerSocket tại port " + port + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
            scheduler.shutdown();
        }
    }

    private static int readPort() {
        Properties props = new Properties();
        try (InputStream is = Main.class.getClassLoader().getResourceAsStream("server.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            LOGGER.warning("Không đọc được server.properties, dùng port mặc định 9999.");
        }
        return Integer.parseInt(props.getProperty("server.port", "9999"));
    }
}
