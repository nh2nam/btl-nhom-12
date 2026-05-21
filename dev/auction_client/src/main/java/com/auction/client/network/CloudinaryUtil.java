package com.auction.client.network;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.File;
import java.util.Map;

public class CloudinaryUtil {

    // Cấu hình chìa khóa bảo mật
    private static final Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
            "cloud_name", "dph4jehed",
            "api_key", "961174814425483",
            "api_secret", "yjk3X7-21rFvtV5fw8yk6EDH10E"
    ));

    // Hàm gọi ảnh bay lên cloud, trả về cái Link
    public static String uploadImage(File file) {
        try {
            System.out.println("Đang tải ảnh lên Cloudinary...");
            Map uploadResult = cloudinary.uploader().upload(file, ObjectUtils.emptyMap());
            String imageUrl = uploadResult.get("url").toString();
            System.out.println("Tải xong! Link ảnh: " + imageUrl);
            return imageUrl;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Lỗi tải ảnh lên Cloud!");
            return null;
        }
    }
}