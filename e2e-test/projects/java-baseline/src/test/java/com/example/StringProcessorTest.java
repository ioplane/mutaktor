package com.example;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class StringProcessorTest {
    final StringProcessor sp = new StringProcessor();

    @Test void reverse() { assertEquals("cba", sp.reverse("abc")); }
    @Test void reverseNull() { assertNull(sp.reverse(null)); }
    @Test void reverseEmpty() { assertEquals("", sp.reverse("")); }
    @Test void palindromeTrue() { assertTrue(sp.isPalindrome("racecar")); }
    @Test void palindromeFalse() { assertFalse(sp.isPalindrome("hello")); }
    @Test void palindromeNull() { assertFalse(sp.isPalindrome(null)); }
    @Test void palindromeEmpty() { assertFalse(sp.isPalindrome("")); }
    @Test void countVowels() { assertEquals(2, sp.countVowels("hello")); }
    @Test void countVowelsNone() { assertEquals(0, sp.countVowels("xyz")); }
    @Test void countVowelsNull() { assertEquals(0, sp.countVowels(null)); }
    @Test void truncateShort() { assertEquals("hi", sp.truncate("hi", 10)); }
    @Test void truncateLong() { assertEquals("hel...", sp.truncate("hello world", 3)); }
    @Test void truncateNull() { assertNull(sp.truncate(null, 5)); }
    @Test void splitSimple() { assertEquals(List.of("a", "b", "c"), sp.split("a,b,c", ',')); }
    @Test void splitNull() { assertEquals(List.of(), sp.split(null, ',')); }
    @Test void classifyA() { assertEquals("A", sp.classify(95)); }
    @Test void classifyB() { assertEquals("B", sp.classify(85)); }
    @Test void classifyC() { assertEquals("C", sp.classify(75)); }
    @Test void classifyD() { assertEquals("D", sp.classify(65)); }
    @Test void classifyF() { assertEquals("F", sp.classify(50)); }
}
