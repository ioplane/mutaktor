package com.example;

import java.util.ArrayList;
import java.util.List;

public class StringProcessor {

    public String reverse(String input) {
        if (input == null) return null;
        return new StringBuilder(input).reverse().toString();
    }

    public boolean isPalindrome(String input) {
        if (input == null || input.isEmpty()) return false;
        String cleaned = input.toLowerCase().replaceAll("[^a-z0-9]", "");
        return cleaned.equals(new StringBuilder(cleaned).reverse().toString());
    }

    public int countVowels(String input) {
        if (input == null) return 0;
        int count = 0;
        for (char c : input.toLowerCase().toCharArray()) {
            if (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u') {
                count++;
            }
        }
        return count;
    }

    public String truncate(String input, int maxLength) {
        if (input == null) return null;
        if (input.length() <= maxLength) return input;
        return input.substring(0, maxLength) + "...";
    }

    public List<String> split(String input, char delimiter) {
        if (input == null) return List.of();
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c == delimiter) {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        parts.add(current.toString());
        return parts;
    }

    public String classify(int score) {
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }
}
