# 도메인 로직 설계

- [x] KIS 실계좌 연결 테스트 완료

polling을 고려할 것.
테스트시 주문과 체결이 제대로 이루어지지 않음.
KIS에 주기적으로 주문/체결 상태를 다시 물어봐야만 함. 주문 완료와 체결을 구분할 것.

## 2. 중심 아키텍처

`DDD-lite + Hexagonal`로 진행.

### 2-1. 헥사고날이 오버엔지니어링은 아닐까
도메인 로직이 완벽하게 설계 되지 않은 만큼 구현 도중 계속 바뀔 수 있다. 
다만 주문 상태 전이, 체결 반영, 포지션 계산, SIMULATION/LIVE 분기 같은 규칙이 Service 메서드 안에 길게 쌓일 가능성이 큼.
규칙 자체는 도메인 객체가 가지게 하고, 헥사고날로 분리도 해둘 필요성도 있어 보이긴 하다.
KIS Open API 와 도메인 테스트도 분리가 필요하긴 하다.

```text
domain
  - 주문, 체결, 포지션, 전략 규칙
  - Spring, JPA, HTTP, KIS DTO를 모름

application
  - transaction boundary
  - domain 객체 호출
  - 실행되고 있는 전략과의 통신

adapter
  - KIS REST API 호출
  - JPA 저장/조회
  - Web Controller
  - 외부 응답과 내부 모델 변환
```


## 도메인 로직

### 도메인
1. | 여러 도메인에서 같이 쓰는 값 타입 | 
2. | 전략 상태, 투자 모드, 주문 한도 |
3. | 주문 생성, 주문 상태 전이, 주문 가능 여부 |
4. | 체결 생성, 체결 출처, 중복 체결 구분 |
5. | 전략별 보유 수량, 평균 단가, 실현 손익 |

초기에는 이 정도만 둔다.

계좌 도메인은 추후 생각. 여러개의 전략이 매수와 매도 의견을 내면? 등등...


### 생각해볼 점
실제 계좌는 전략별 보유 수량을 알려주지 않는다.
예를 들어 한 계좌에 `001510`을 3주 보유하고 있어도, 그 3주가 어떤 전략에서 산 것인지 KIS는 모른다.
그래서 내부 시스템은 전략별 포지션을 따로 관리해야 한다.


- 매수 체결이 들어오면 수량이 증가하고 평균 단가를 다시 계산한다.
- 매도 체결이 들어오면 수량이 감소하고 실현 손익을 계산한다.
- 보유 수량보다 많이 팔 수 없다.
- 수량이 0이 되면 평균 단가는 0으로 돌아간다.

- KIS 외부 호출을 긴 DB transaction 안에 넣지 않는다. but 당장은 상관 없을 수 도 있음.
- 주문 요청은 KIS 호출 전에 `REQUESTED` 상태로 남기기 test시 다양한 이유로 거절됨을 기억.
- KIS timeout은 `UNKNOWN`으로 남기고 자동 재주문하지 않는다.



### Application Service가 할 일

```text
PlaceOrderApplicationService
  -> Strategy 조회
  -> StrategyPosition 조회
  -> 미완료 주문 조회
  -> OrderEligibilityService 검증
  -> Order REQUESTED 저장
  -> tradeMode 확인
      -> SIMULATION: 내부 체결 생성, position 갱신, log 저장
      -> LIVE: KIS adapter 호출, 주문 상태 반영?
```

