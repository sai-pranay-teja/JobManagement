import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestAppPart1 {

    @Test
    public void testAddition() {
        // A sample test for demonstrating part 1 behavior.
        int a = 2, b = 3;
        int sum = a + b;
        assertEquals(5, sum, "2 + 3 should equal 5");
    }
    
    @Test
    public void testStringNotNull() {
        // Another simple test in part 1.
        String message = "Hello, part 1!";
        assertNotNull(message, "Message should not be null");
    }
    
    // Add more tests for part 1 as needed.
}
