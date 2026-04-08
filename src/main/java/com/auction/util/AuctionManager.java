package com.auction.util;
import java.util.Collection;
import com.auction.model.Auction;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class AuctionManager {
    // Biến instance duy nhất, dùng 'volatile' để đồng bộ trên RAM cho đa luồng
    private static volatile AuctionManager instance;

    // Lưu trữ tất cả các phiên đấu giá (ID -> Auction).
    private Map<Integer, Auction> activeAuctions;

    // 1. Private Constructor: Chặn không cho ai dùng lệnh 'new AuctionManager()' ở bên ngoài
    private AuctionManager() {
        activeAuctions = new ConcurrentHashMap<>();
    }

    // 2. Hàm static để lấy ra instance duy nhất (Singleton Pattern)
    public static AuctionManager getInstance() {
        if (instance == null) {
            synchronized (AuctionManager.class) {
                if (instance == null) {
                    instance = new AuctionManager();
                }
            }
        }
        return instance;
    }

    // Các hàm quản lý cơ bản
    public void addAuction(Auction auction) {
        activeAuctions.put(auction.getId(), auction);
    }

    public Auction getAuction(int id) {
        return activeAuctions.get(id);
    }
    // Lấy ra danh sách tất cả các phiên đấu giá đang chạy
    public Collection<Auction> getAllAuctions() {
        return activeAuctions.values();
    }
}