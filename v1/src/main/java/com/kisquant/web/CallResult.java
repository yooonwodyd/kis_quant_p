package com.kisquant.web;

/**
 * 브라우저 테스트 화면에 내려줄 KIS 호출 결과.
 * raw response를 보되, 위험한 값은 마스킹된 상태로 내려간다.
 */
public record CallResult(
		String name,
		boolean ok,
		int httpStatus,
		Object request,
		Object parsedResponse,
		Object rawResponse
) {
}
