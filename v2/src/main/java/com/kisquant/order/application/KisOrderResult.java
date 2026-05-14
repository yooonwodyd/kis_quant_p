package com.kisquant.order.application;

import com.kisquant.order.domain.KisOrderNumber;
import com.kisquant.order.domain.KisOrderOrgNumber;

public record KisOrderResult(
        KisOrderResultStatus status,
        KisOrderNumber kisOrderNumber,
        KisOrderOrgNumber kisOrderOrgNumber,
        String rejectCode,
        String rejectMessage
) {

    public static KisOrderResult accepted(KisOrderNumber orderNumber, KisOrderOrgNumber orderOrgNumber) {
        return new KisOrderResult(KisOrderResultStatus.ACCEPTED, orderNumber, orderOrgNumber, null, null);
    }

    public static KisOrderResult rejected(String code, String message) {
        return new KisOrderResult(KisOrderResultStatus.REJECTED, null, null, code, message);
    }
}
