package com.kisquant.order.application;

public record CancelOrderResult(boolean successful, String message) {

    public static CancelOrderResult succeeded() {
        return new CancelOrderResult(true, "canceled");
    }

    public static CancelOrderResult failed(String message) {
        return new CancelOrderResult(false, message);
    }
}
