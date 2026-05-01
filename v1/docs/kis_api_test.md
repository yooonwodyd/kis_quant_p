# KIS 실계좌 연결 테스트

실전 계좌 연결을 위한 프로토타입 설계 문서.


# 순서
1. KIS Open API로 내 실전 계좌에서 토큰을 받기
2. 잔고 보기
3. 현재가 보기
4. 매수 가능 금액 확인 하기
5. 주문 넣기
6. 주문 상태와 체결 확인

## 1. 예상 흐름

```text
  -> Controller
  -> OrderGuard
  -> KisRestClient
  -> KIS Open API
  -> CallResult
```

`Controller`에서 브라우저 요청을 받기 
위험한 요청인지 `OrderGuard`로 먼저 검사(허용된 종목인지, 최대 금액을 넘진 않았는지, 등등) 
검사를 통과하면 `KisRestClient`가 KIS Open API를 직접 호출
응답은 `CallResult`에 담기


## 2.사용할 KIS API 목록
### 비고
TR ID = Transaction ID / 거래 ID
일반 웹 API와 다르게 URL + TR ID로 api 요청 보냄
단순히 crud로 담을 수 없는 업무가 많다보니 사용하는 것으로 보인다. 추후 프로젝트에 적용할 지 고려해보기
관리해야하는 url의 수를 줄이면서 밀집도를 높히는 재밌는 방법.  

### api 목록
| API | Method | Endpoint | TR ID | Request | Response | 비고                                                                             |
| --- | --- | --- | --- | --- | --- |--------------------------------------------------------------------------------|
| 토큰 발급 | `POST` | `/oauth2/tokenP` | - | Body: `grant_type`, `appkey`, `appsecret` | `access_token`, `access_token_token_expired`, `token_type`, `expires_in` | 이후 모든 KIS 요청의 `Authorization` header에 사용할 토큰을 받는다..                            |
| 잔고 조회 | `GET` | `/uapi/domestic-stock/v1/trading/inquire-balance` | `TTTC8434R` | Query: `CANO`, `ACNT_PRDT_CD`, `INQR_DVSN`, `UNPR_DVSN`, `PRCS_DVSN`, `CTX_AREA_FK100`, `CTX_AREA_NK100` | `rt_cd`, `msg_cd`, `msg1`, `output1`, `output2` | 내 실전 계좌가 제대로 연결되었는지 확인한다. `output1`은 보유 종목, `output2`는 계좌 평가 정보.               |
| 현재가 조회 | `GET` | `/uapi/domestic-stock/v1/quotations/inquire-price` | `FHKST01010100` | Query: `FID_COND_MRKT_DIV_CODE`, `FID_INPUT_ISCD` | `rt_cd`, `msg_cd`, `msg1`, `output` | 주문 전에 기준 가격을 확인하기 위해 사용한다. 프로토타입에서는 허용된 종목만 조회한다.                              |
| 매수 가능 조회 | `GET` | `/uapi/domestic-stock/v1/trading/inquire-psbl-order` | `TTTC8908R` | Query: `CANO`, `ACNT_PRDT_CD`, `PDNO`, `ORD_UNPR`, `ORD_DVSN`, `CMA_EVLU_AMT_ICLD_YN`, `OVRS_ICLD_YN` | `rt_cd`, `msg_cd`, `msg1`, `output` | 잔고가 있어도 해당 가격으로 주문 가능한지는 별도 확인이 필요하다. 주문 직전에 확인하는 API로 둔다. 특히 나중에 전략별로 계좌를 공유하면 문제가 생길 수도 |
| 현금 주문 | `POST` | `/uapi/domestic-stock/v1/trading/order-cash` | 매수: `TTTC0012U`<br>매도: `TTTC0011U` | Body: `CANO`, `ACNT_PRDT_CD`, `PDNO`, `ORD_DVSN`, `ORD_QTY`, `ORD_UNPR`, `EXCG_ID_DVSN_CD` | `rt_cd`, `msg_cd`, `msg1`, `ODNO`, `KRX_FWDG_ORD_ORGNO`, `ORD_TMD` | 실제 주문이 나가는 API다. 호출 전에 `OrderGuard`로 종목, 수량, 금액을 검사하고, 응답의 주문번호는 조회와 취소에 다시 사용한다. |
| 시장가 주문 | `POST` | `/uapi/domestic-stock/v1/trading/order-cash` | 매수: `TTTC0012U`<br>매도: `TTTC0011U` | Body: `ORD_DVSN=01`, `ORD_UNPR=0`, `ORD_QTY=1` | `rt_cd`, `msg_cd`, `msg1`, `ODNO`, `KRX_FWDG_ORD_ORGNO`, `ORD_TMD` | 시장가 주문 형식만 따로 확인.                                                              |
| 주문/체결 조회 | `GET` | `/uapi/domestic-stock/v1/trading/inquire-daily-ccld` | `TTTC0081R` | Query: `CANO`, `ACNT_PRDT_CD`, `INQR_STRT_DT`, `INQR_END_DT`, `PDNO`, `ODNO`, `ORD_GNO_BRNO`, `EXCG_ID_DVSN_CD` | `rt_cd`, `msg_cd`, `msg1`, 당일 주문/체결 목록 | 주문 응답만 믿지 않고 실제 주문 상태와 체결 여부를 다시 확인. pooling 처리                                |
| 주문 취소 | `POST` | `/uapi/domestic-stock/v1/trading/order-rvsecncl` | `TTTC0013U` | Body: `CANO`, `ACNT_PRDT_CD`, `KRX_FWDG_ORD_ORGNO`, `ORGN_ODNO`, `RVSE_CNCL_DVSN_CD`, `QTY_ALL_ORD_YN`, `EXCG_ID_DVSN_CD` | `rt_cd`, `msg_cd`, `msg1`, 취소 접수 결과 | 미체결 주문을 취소할 때 사용한다. 원주문번호뿐 아니라 거래소 주문조직번호도 필요                                  |


## 주의할 점
정보 마스킹 로직 만들어보기
config 설정 신경쓰기
