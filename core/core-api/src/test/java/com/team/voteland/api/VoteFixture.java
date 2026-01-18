package com.team.voteland.api;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.stereotype.Component;

@Component
public class VoteFixture {
    private final TestRestTemplate restTemplate;

    public VoteFixture(TestRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public TestRestTemplate client() {
        return restTemplate;
    }
}
