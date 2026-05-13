package com.auction.exception;

public class BidTooLowException extends AuctionException {
    public BidTooLowException(String message) {
        super(message);
    }
}