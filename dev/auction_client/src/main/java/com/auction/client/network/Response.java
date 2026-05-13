package com.auction.client.network;

import com.google.gson.Gson;

public class Response {
    private boolean success;
    private String message;

    // SỬA Ở ĐÂY: Đổi từ String sang Object để hứng được mọi loại dữ liệu từ Server
    private Object data;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    // Tự động chuyển đổi thành chuỗi JSON để các Controller (như LoginController) vẫn hoạt động bình thường
    public String getData() {
        if (data == null) {
            return null;
        }
        // Nếu đã là chuỗi thì trả về luôn
        if (data instanceof String) {
            return (String) data;
        }
        // Nếu là Object (như thông tin user) thì dịch ra chuỗi JSON
        return new Gson().toJson(data);
    }
}