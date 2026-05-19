#!/usr/bin/env python3
"""Run a simple 1-minute buy/sell API smoke strategy.

This strategy is for API verification, not profit. It buys 1 share when the
Spring strategy position is empty and sells 1 share when a position exists.
"""

import argparse
import json
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from typing import Any


class StrategyApiClient:
    def __init__(self, base_url: str, timeout_seconds: float = 5.0):
        self.base_url = base_url.rstrip("/")
        self.timeout_seconds = timeout_seconds

    def post_log(
            self,
            strategy_id: int,
            level: str,
            message: str,
            payload: dict[str, Any] | None = None
    ) -> dict[str, Any]:
        return self._request(
            "POST",
            "/api/strategy/logs",
            body={
                "strategyId": strategy_id,
                "level": level,
                "message": message,
                "payloadJson": json.dumps(payload or {}, ensure_ascii=False),
            },
        )

    def get_position(self, strategy_id: int, symbol: str) -> dict[str, Any]:
        return self._request(
            "GET",
            "/api/strategy/positions",
            query={"strategyId": strategy_id, "symbol": symbol},
        )

    def place_order(
            self,
            strategy_id: int,
            symbol: str,
            side: str,
            order_type: str,
            quantity: int,
            order_price: int
    ) -> dict[str, Any]:
        return self._request(
            "POST",
            "/api/strategy/orders",
            body={
                "strategyId": strategy_id,
                "symbol": symbol,
                "side": side,
                "orderType": order_type,
                "quantity": quantity,
                "orderPrice": order_price,
            },
        )

    def _request(
            self,
            method: str,
            path: str,
            body: dict[str, Any] | None = None,
            query: dict[str, Any] | None = None
    ) -> dict[str, Any]:
        url = self.base_url + path
        if query:
            url += "?" + urllib.parse.urlencode(query)

        data = None
        headers = {"Accept": "application/json"}
        if body is not None:
            data = json.dumps(body, ensure_ascii=False).encode("utf-8")
            headers["Content-Type"] = "application/json"

        request = urllib.request.Request(url, data=data, headers=headers, method=method)
        try:
            with urllib.request.urlopen(request, timeout=self.timeout_seconds) as response:
                response_body = response.read().decode("utf-8")
        except urllib.error.HTTPError as error:
            message = error.read().decode("utf-8", errors="replace")
            raise RuntimeError(f"{method} {path} failed: HTTP {error.code} {message}") from error
        except urllib.error.URLError as error:
            raise RuntimeError(f"{method} {path} failed: {error.reason}") from error

        if not response_body:
            return {}
        return json.loads(response_body)


def run_iteration(
        client,
        strategy_id: int,
        symbol: str,
        quantity: int,
        order_type: str = "MARKET",
        order_price: int = 0
) -> dict[str, Any]:
    side = "UNKNOWN"
    position_quantity = None
    try:
        position = client.get_position(strategy_id, symbol)
        position_quantity = int(position.get("quantity", 0))
        side = "BUY" if position_quantity <= 0 else "SELL"
        client.post_log(strategy_id, "INFO", f"전략 판단: {symbol} 포지션 {position_quantity}주 -> {side} {quantity}주 요청", {
            "event": "STRATEGY_DECISION",
            "strategyId": strategy_id,
            "symbol": symbol,
            "side": side,
            "quantity": quantity,
            "positionQuantityBeforeOrder": position_quantity,
        })
        effective_order_price = 0 if order_type == "MARKET" else order_price
        order = client.place_order(
            strategy_id,
            symbol,
            side,
            order_type,
            quantity,
            effective_order_price,
        )
        return {"side": side, **order}
    except Exception as error:
        client.post_log(strategy_id, "ERROR", f"전략 실행 실패: {symbol} {side} {quantity}주", {
            "event": "STRATEGY_ERROR",
            "strategyId": strategy_id,
            "symbol": symbol,
            "side": side,
            "quantity": quantity,
            "positionQuantityBeforeOrder": position_quantity,
            "error": compact_error(error),
        })
        raise


def compact_error(error: Exception, limit: int = 500) -> dict[str, Any]:
    message = str(error)
    return {
        "type": error.__class__.__name__,
        "message": message[:limit],
        "truncated": len(message) > limit,
    }


def run_loop(
        client,
        strategy_id: int,
        symbol: str,
        quantity: int,
        interval_seconds: int,
        iterations: int,
        sleep=time.sleep,
        order_type: str = "MARKET",
        order_price: int = 0
) -> None:
    completed = 0
    while iterations == 0 or completed < iterations:
        try:
            order = run_iteration(client, strategy_id, symbol, quantity, order_type, order_price)
            print(f"iteration={completed + 1} side={order.get('side')} status={order.get('status')}", flush=True)
        except Exception as error:
            print(f"iteration={completed + 1} failed: {error}", file=sys.stderr, flush=True)

        completed += 1
        if iterations != 0 and completed >= iterations:
            break
        sleep(interval_seconds)


def positive_int(value: str) -> int:
    parsed = int(value)
    if parsed <= 0:
        raise argparse.ArgumentTypeError("must be greater than 0")
    return parsed


def non_negative_int(value: str) -> int:
    parsed = int(value)
    if parsed < 0:
        raise argparse.ArgumentTypeError("must be greater than or equal to 0")
    return parsed


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Run the 1-minute API ping-pong strategy.")
    parser.add_argument("--base-url", default="http://localhost:8090")
    parser.add_argument("--strategy-id", type=positive_int, default=1)
    parser.add_argument("--symbol", default="001510")
    parser.add_argument("--quantity", type=positive_int, default=1)
    parser.add_argument("--interval-seconds", type=positive_int, default=60)
    parser.add_argument("--iterations", type=non_negative_int, default=0, help="0 means run forever")
    parser.add_argument("--order-type", choices=["MARKET", "LIMIT"], default="MARKET")
    parser.add_argument("--order-price", type=non_negative_int, default=0)
    parser.add_argument("--timeout-seconds", type=float, default=5.0)
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(sys.argv[1:] if argv is None else argv)
    client = StrategyApiClient(args.base_url, args.timeout_seconds)
    try:
        run_loop(
            client,
            strategy_id=args.strategy_id,
            symbol=args.symbol,
            quantity=args.quantity,
            interval_seconds=args.interval_seconds,
            iterations=args.iterations,
            order_type=args.order_type,
            order_price=args.order_price,
        )
    except KeyboardInterrupt:
        print("stopped by user", file=sys.stderr)
        return 130
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
