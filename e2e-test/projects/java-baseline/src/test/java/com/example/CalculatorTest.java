package com.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CalculatorTest {
    final Calculator calc = new Calculator();

    @Test void add() { assertEquals(5, calc.add(2, 3)); }
    @Test void addNegative() { assertEquals(-1, calc.add(2, -3)); }
    @Test void subtract() { assertEquals(1, calc.subtract(3, 2)); }
    @Test void multiply() { assertEquals(6, calc.multiply(2, 3)); }
    @Test void multiplyByZero() { assertEquals(0, calc.multiply(5, 0)); }
    @Test void divide() { assertEquals(2.5, calc.divide(5, 2)); }
    @Test void divideByZero() { assertThrows(ArithmeticException.class, () -> calc.divide(1, 0)); }
    @Test void isPositive() { assertTrue(calc.isPositive(1)); }
    @Test void isNotPositive() { assertFalse(calc.isPositive(0)); }
    @Test void isNegative() { assertFalse(calc.isPositive(-1)); }
    @Test void absPositive() { assertEquals(5, calc.abs(5)); }
    @Test void absNegative() { assertEquals(5, calc.abs(-5)); }
    @Test void absZero() { assertEquals(0, calc.abs(0)); }
    @Test void maxFirst() { assertEquals(5, calc.max(5, 3)); }
    @Test void maxSecond() { assertEquals(5, calc.max(3, 5)); }
    @Test void maxEqual() { assertEquals(5, calc.max(5, 5)); }
    @Test void clampBelow() { assertEquals(0, calc.clamp(-5, 0, 100)); }
    @Test void clampAbove() { assertEquals(100, calc.clamp(150, 0, 100)); }
    @Test void clampInRange() { assertEquals(50, calc.clamp(50, 0, 100)); }
}
