package com.test.backend.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.test.backend.dto.response.ApiResponse;
import com.test.backend.exception.TodoApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

// 즉흥형(P유형) 여행자를 위한 AI 코스 큐레이션. Claude(Anthropic Java SDK)로 지역+일수 → 간결한 코스 텍스트.
// 키는 서버 env(ANTHROPIC_API_KEY)에만. 같은 지역+일수는 캐시(Opus 과금 절약).
@Service
public class CurationService {

    private static final String SYSTEM =
            "너는 MBTI P유형(즉흥형) 여행자를 위한 여행 큐레이터야. 계획을 빡빡하게 짜지 말고, "
                    + "가볍게 즐길 수 있는 관광지·맛집 위주로 제안해. 한국어로, 간결하게, 이모지를 조금 섞어서. "
                    + "마크다운 기호(#, *)는 쓰지 말고 각 날짜를 'Day N'으로 시작하는 짧은 줄로.";

    private final boolean configured;
    private final AnthropicClient client;

    public CurationService(@Value("${anthropic.api-key:}") String apiKey) {
        this.configured = apiKey != null && !apiKey.isBlank();
        this.client = configured
                ? AnthropicOkHttpClient.builder().apiKey(apiKey).build()
                : null;
    }

    @Cacheable(value = "curation", key = "#region.strip().toLowerCase() + '|' + #days")
    public ApiResponse<String> curate(String region, int days) {
        if (!configured) {
            throw new TodoApiException(HttpStatus.SERVICE_UNAVAILABLE, "CURATION_NOT_CONFIGURED",
                    "AI 추천이 설정되지 않았습니다(ANTHROPIC_API_KEY).");
        }
        if (region == null || region.isBlank()) {
            throw new TodoApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "지역을 입력해야 합니다.");
        }
        int d = Math.max(1, Math.min(days, 14)); // 과금·길이 방어
        String prompt = region.strip() + " " + d + "일 여행 추천 코스를 짜줘. "
                + "하루에 관광지 1~2곳과 맛집 1곳 정도, 즉흥적으로 즐길 수 있게. 각 날짜별로 짧게.";
        try {
            Message response = client.messages().create(MessageCreateParams.builder()
                    .model(Model.of("claude-opus-4-8")) // 이 SDK 버전엔 4.8 상수가 없어 문자열 ID로(유효)
                    .maxTokens(2000L)
                    .system(SYSTEM)
                    .addUserMessage(prompt)
                    .build());
            StringBuilder sb = new StringBuilder();
            response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .forEach(t -> sb.append(t.text()));
            String text = sb.toString().strip();
            if (text.isEmpty()) {
                throw new TodoApiException(HttpStatus.BAD_GATEWAY, "CURATION_FAILED", "추천을 생성하지 못했어요.");
            }
            return new ApiResponse<>(text);
        } catch (TodoApiException e) {
            throw e;
        } catch (Exception e) {
            throw new TodoApiException(HttpStatus.BAD_GATEWAY, "CURATION_FAILED", "AI 추천 호출에 실패했어요.");
        }
    }
}
