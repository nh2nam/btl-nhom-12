package com.auction.network;

import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// Đây chính là cốt lõi của Observer Pattern
public class BroadcastManager {
    // Danh sách chứa các "Cái loa" của tất cả các Client đang kết nối
    // Dùng CopyOnWriteArrayList để an toàn khi nhiều luồng cùng thêm/xóa
    private static final List<PrintWriter> observers = new CopyOnWriteArrayList<>();

    // 1. Thêm một người nghe đài mới (Client gửi lệnh SUBSCRIBE)
    public static void addObserver(PrintWriter out) {
        observers.add(out);
    }

    // 2. Xóa người nghe đài khi họ tắt app
    public static void removeObserver(PrintWriter out) {
        observers.remove(out);
    }

    // 3. Hét lên cho tất cả mọi người cùng nghe! (cập nhật giá + thời gian kết thúc)
    public static void broadcastPriceUpdate(int auctionId, double newPrice, String newEndTime) {
        // Đóng gói tin nhắn dạng JSON thủ công cho nhanh
        String message = String.format(
                "{\"action\":\"UPDATE_PRICE\", \"auctionId\":%d, \"newPrice\":%f, \"newEndTime\":\"%s\"}",
                auctionId, newPrice, newEndTime);

        System.out.println("📢 BROADCAST: " + message);
        for (PrintWriter out : observers) {
            try {
                out.println(message); // Đẩy dữ liệu thẳng về Client
            } catch (Exception e) {
                observers.remove(out); // Nếu Client này đứt mạng thì xóa đi
            }
        }
    }

    // 4. Phát tin nhắn chat đến tất cả client đang theo dõi phiên
    public static void broadcastChatMessage(int auctionId, String senderName, String content) {
        // Escape dấu nháy kép trong nội dung để tránh vỡ JSON thủ công
        String safeContent    = content.replace("\\", "\\\\").replace("\"", "\\\"");
        String safeSenderName = senderName.replace("\\", "\\\\").replace("\"", "\\\"");
        String message = String.format(
                "{\"action\":\"NEW_CHAT_MESSAGE\", \"auctionId\":%d, \"sender\":\"%s\", \"content\":\"%s\"}",
                auctionId, safeSenderName, safeContent);

        System.out.println("💬 CHAT BROADCAST: " + message);
        for (PrintWriter out : observers) {
            try {
                out.println(message);
            } catch (Exception e) {
                observers.remove(out);
            }
        }
    }
}