package com.example;

public class Calculator {

    public int add(int a, int b) {
        return a + b;
    }

    public int subtract(int a, int b) {
        return a - b;
    }

    public int multiply(int a, int b) {
        return a * b;
    }

    public double divide(int a, int b) {
        if (b == 0) {
            throw new ArithmeticException("Division by zero");
        }
        return (double) a / b;
    }

    public boolean isPositive(int n) {
        return n > 0;
    }

    public int abs(int n) {
        if (n < 0) {
            return -n;
        }
        return n;
    }

    public int max(int a, int b) {
        return a >= b ? a : b;
    }

    public int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
}
