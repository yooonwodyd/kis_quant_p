import contextlib
import io
import importlib.util
from pathlib import Path
import unittest


SCRIPT_PATH = Path(__file__).resolve().parents[1] / "scripts" / "five_minute_ping_pong_strategy.py"


def load_strategy_module():
    spec = importlib.util.spec_from_file_location("five_minute_ping_pong_strategy", SCRIPT_PATH)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class FakeClient:
    def __init__(self, positions):
        self.positions = list(positions)
        self.orders = []
        self.logs = []

    def post_log(self, strategy_id, level, message, payload=None):
        self.logs.append({
            "strategyId": strategy_id,
            "level": level,
            "message": message,
            "payload": payload or {},
        })

    def get_position(self, strategy_id, symbol):
        return self.positions.pop(0)

    def place_order(self, strategy_id, symbol, side, order_type, quantity, order_price):
        order = {
            "strategyId": strategy_id,
            "symbol": symbol,
            "side": side,
            "orderType": order_type,
            "quantity": quantity,
            "orderPrice": order_price,
            "status": "FILLED",
        }
        self.orders.append(order)
        return order


class FailingOrderClient(FakeClient):
    def place_order(self, strategy_id, symbol, side, order_type, quantity, order_price):
        raise RuntimeError("x" * 700)


class OneMinutePingPongStrategyTest(unittest.TestCase):
    def test_default_interval_is_one_minute(self):
        strategy = load_strategy_module()

        args = strategy.parse_args([])

        self.assertEqual(args.interval_seconds, 60)

    def test_run_iteration_buys_one_share_when_position_is_empty(self):
        strategy = load_strategy_module()
        client = FakeClient([{"quantity": 0}])

        strategy.run_iteration(client, strategy_id=1, symbol="001510", quantity=1)

        self.assertEqual(client.orders[0]["side"], "BUY")
        self.assertEqual(client.orders[0]["quantity"], 1)
        self.assertEqual(len(client.logs), 1)
        self.assertEqual(client.logs[0]["message"], "전략 판단: 001510 포지션 0주 -> BUY 1주 요청")
        self.assertEqual(client.logs[0]["payload"]["side"], "BUY")

    def test_run_iteration_sells_one_share_when_position_exists(self):
        strategy = load_strategy_module()
        client = FakeClient([{"quantity": 3}])

        strategy.run_iteration(client, strategy_id=1, symbol="001510", quantity=1)

        self.assertEqual(client.orders[0]["side"], "SELL")
        self.assertEqual(client.orders[0]["quantity"], 1)
        self.assertEqual(len(client.logs), 1)
        self.assertEqual(client.logs[0]["message"], "전략 판단: 001510 포지션 3주 -> SELL 1주 요청")
        self.assertEqual(client.logs[0]["payload"]["side"], "SELL")

    def test_run_iteration_logs_compact_error_payload_when_order_fails(self):
        strategy = load_strategy_module()
        client = FailingOrderClient([{"quantity": 0}])

        with self.assertRaises(RuntimeError):
            strategy.run_iteration(client, strategy_id=1, symbol="001510", quantity=1)

        error_log = client.logs[-1]
        self.assertEqual(error_log["level"], "ERROR")
        self.assertEqual(error_log["message"], "전략 실행 실패: 001510 BUY 1주")
        self.assertEqual(error_log["payload"]["error"]["type"], "RuntimeError")
        self.assertEqual(len(error_log["payload"]["error"]["message"]), 500)
        self.assertTrue(error_log["payload"]["error"]["truncated"])

    def test_run_loop_sleeps_between_iterations_only(self):
        strategy = load_strategy_module()
        client = FakeClient([{"quantity": 0}, {"quantity": 1}])
        sleeps = []

        with contextlib.redirect_stdout(io.StringIO()):
            strategy.run_loop(
                client,
                strategy_id=1,
                symbol="001510",
                quantity=1,
                interval_seconds=60,
                iterations=2,
                sleep=sleeps.append,
            )

        self.assertEqual([order["side"] for order in client.orders], ["BUY", "SELL"])
        self.assertEqual(sleeps, [60])


if __name__ == "__main__":
    unittest.main()
