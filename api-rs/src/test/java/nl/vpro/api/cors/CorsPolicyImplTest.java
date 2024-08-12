package nl.vpro.api.cors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
public class CorsPolicyImplTest {


    @Test
    public void test() {
        CorsPolicyImpl impl = new CorsPolicyImpl(true, "classpath:/cors/policy.properties");

        assertTrue(impl.allowedOriginAndMethod("localhost", "GET"));
        assertTrue(impl.isEnabled());

        assertFalse(impl.allowedOriginAndMethod("localhost", "PUT"));
    }


}
