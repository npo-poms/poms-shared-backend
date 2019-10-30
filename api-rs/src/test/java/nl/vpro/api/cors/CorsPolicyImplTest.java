package nl.vpro.api.cors;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
public class CorsPolicyImplTest {


    @Test
    public void test() throws Exception {
        CorsPolicyImpl impl = new CorsPolicyImpl(true, "classpath:/cors/policy.properties");

        assertTrue(impl.allowedOriginAndMethod("localhost", "GET"));
        assertTrue(impl.isEnabled());

        assertFalse(impl.allowedOriginAndMethod("localhost", "PUT"));
    }


}
