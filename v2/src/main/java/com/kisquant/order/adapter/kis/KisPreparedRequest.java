package com.kisquant.order.adapter.kis;

import java.util.Map;

record KisPreparedRequest(String path, String trId, Map<String, Object> body) {
}
