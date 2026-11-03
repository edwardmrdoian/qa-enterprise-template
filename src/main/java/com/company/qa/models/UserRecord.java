package com.company.qa.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserRecord(String id, String name, String job, String email, String createdAt) {}
