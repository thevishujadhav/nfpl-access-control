package com.nfpl.access.service;

public record AccessDecision(boolean allowed, String reasonCode, String message) {}
