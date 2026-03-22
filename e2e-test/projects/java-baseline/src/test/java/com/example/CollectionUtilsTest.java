package com.example;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CollectionUtilsTest {
    final CollectionUtils cu = new CollectionUtils();

    @Test void filterEven() { assertEquals(List.of(2, 4), cu.filter(List.of(1, 2, 3, 4), n -> n % 2 == 0)); }
    @Test void filterNull() { assertEquals(List.of(), cu.filter(null, n -> true)); }
    @Test void filterNone() { assertEquals(List.of(), cu.filter(List.of(1, 3, 5), n -> n % 2 == 0)); }
    @Test void findMax() { assertEquals(5, cu.findMax(List.of(1, 5, 3))); }
    @Test void findMaxSingle() { assertEquals(1, cu.findMax(List.of(1))); }
    @Test void findMaxNull() { assertNull(cu.findMax(null)); }
    @Test void findMaxEmpty() { assertNull(cu.findMax(List.<Integer>of())); }
    @Test void isSortedTrue() { assertTrue(cu.isSorted(List.of(1, 2, 3))); }
    @Test void isSortedFalse() { assertFalse(cu.isSorted(List.of(3, 1, 2))); }
    @Test void isSortedEmpty() { assertTrue(cu.isSorted(List.<Integer>of())); }
    @Test void isSortedNull() { assertTrue(cu.isSorted(null)); }
    @Test void isSortedSingle() { assertTrue(cu.isSorted(List.of(1))); }
    @Test void range() { assertEquals(List.of(1, 2, 3), cu.range(1, 4)); }
    @Test void rangeEmpty() { assertEquals(List.of(), cu.range(5, 5)); }
    @Test void indexOf() { assertEquals(1, cu.indexOf(List.of("a", "b", "c"), "b")); }
    @Test void indexOfNotFound() { assertEquals(-1, cu.indexOf(List.of("a", "b"), "z")); }
    @Test void indexOfNull() { assertEquals(-1, cu.indexOf(null, "x")); }
}
