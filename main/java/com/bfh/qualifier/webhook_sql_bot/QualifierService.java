package com.bfh.qualifier.webhook_sql_bot;

import com.bfh.qualifier.dto.GenerateWebhookResponse;
import com.bfh.qualifier.dto.SubmitSolutionRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class QualifierService {

    private final RestTemplate restTemplate;

    @Value("${app.candidate.name}")
    private String name;

    @Value("${app.candidate.regNo}")
    private String regNo;

    @Value("${app.candidate.email}")
    private String email;

    @Value("${app.solution.finalSql}")
    private String finalSql;

    private static final String GENERATE_WEBHOOK_URL =
            "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

    public QualifierService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void runFlow() throws Exception {
        // 1) Generate webhook
        GenerateWebhookResponse resp = generateWebhook();

        if (resp == null || resp.getWebhook() == null || resp.getAccessToken() == null) {
            throw new IllegalStateException("Invalid response from generateWebhook");
        }

        String webhookUrl = resp.getWebhook();
        String accessToken = resp.getAccessToken();

        System.out.println("Webhook URL: " + webhookUrl);
        System.out.println("Access Token: " + accessToken);

        // 2) Odd/Even check
        int lastTwo = parseLastTwoDigits(regNo);
        boolean isOdd = (lastTwo % 2 != 0);
        System.out.println("regNo last two digits = " + lastTwo +
                " → You get Question " + (isOdd ? "1 (odd)" : "2 (even)"));

        // 3) Save SQL locally
        Path out = Path.of("final_query.sql");
        Files.writeString(out, finalSql);
        System.out.println("Wrote final SQL to " + out.toAbsolutePath());

        // 4) Submit SQL
        submitFinalQuery(webhookUrl, accessToken, finalSql);
        System.out.println("✅ Submission attempted to: " + webhookUrl);
    }

    private GenerateWebhookResponse generateWebhook() {
        GenerateWebhookRequest body = new GenerateWebhookRequest(name, regNo, email);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<GenerateWebhookRequest> entity = new HttpEntity<>(body, headers);

        ResponseEntity<GenerateWebhookResponse> response =
                restTemplate.postForEntity(GENERATE_WEBHOOK_URL, entity, GenerateWebhookResponse.class);

        return response.getBody();
    }

    private void submitFinalQuery(String webhookUrl, String accessToken, String finalQuery) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Try simple token first. If 401, switch to headers.setBearerAuth(accessToken)
        headers.set("Authorization", accessToken);

        SubmitSolutionRequest body = new SubmitSolutionRequest(finalQuery);
        HttpEntity<SubmitSolutionRequest> entity = new HttpEntity<>(body, headers);

        restTemplate.exchange(webhookUrl, HttpMethod.POST, entity, String.class);
    }

    private int parseLastTwoDigits(String regNo) {
        String digits = regNo.replaceAll("\\D", "");
        if (digits.length() < 2) {
            return Integer.parseInt(digits);
        }
        return Integer.parseInt(digits.substring(digits.length() - 2));
    }
}
