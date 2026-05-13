package com.auction.util;

import com.auction.model.*;

public class ItemFactory {
    // Nhà máy nhận yêu cầu (category) và trả về đúng loại sản phẩm
    public static Item createItem(String category, int id, String name, String description, double price, String imagePath) {

        // Đã sửa GeneralItem -> OtherItem
        if (category == null) return new OtherItem(id, name, description, price, imagePath);

        switch (category.toUpperCase()) {
            case "ELECTRONICS":
                return new Electronics(id, name, description, price, imagePath);
            case "ARTS":
                return new Arts(id, name, description, price, imagePath);
            case "FASHION": // Thầy thấy em có tạo file Fashion.java trên tab nên thêm luôn vào đây
                return new Fashion(id, name, description, price, imagePath);
            case "REALESTATE": // Tương tự với RealEstate.java
                return new RealEstate(id, name, description, price, imagePath);
            case "OTHER":
                return new OtherItem(id, name, description, price, imagePath);
            default:
                // Đã sửa GeneralItem -> OtherItem
                return new OtherItem(id, name, description, price, imagePath);
        }
    }
}