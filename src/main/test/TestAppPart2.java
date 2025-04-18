import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestAppPart2 {

    @Test
    public void testMultiplication() {
        // A sample test for demonstrating part 2 behavior.
        int a = 4, b = 5;
        int product = a * b;
        assertEquals(20, product, "4 * 5 should equal 20");
    }
    
    @Test
    public void testBooleanCondition() {
        // Another simple test in part 2.
        boolean condition = (10 > 5);
        assertTrue(condition, "10 should be greater than 5");
    }
    
    // Add more tests for part 2 as needed.
}
