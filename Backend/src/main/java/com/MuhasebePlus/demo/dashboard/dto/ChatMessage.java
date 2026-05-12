package com.MuhasebePlus.demo.dashboard.dto;

public record ChatMessage(String role, String content) {
    // role: "user" | "assistant"
}
