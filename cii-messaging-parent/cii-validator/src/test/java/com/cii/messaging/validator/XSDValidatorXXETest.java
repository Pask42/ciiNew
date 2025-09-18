package com.cii.messaging.validator;

import com.cii.messaging.validator.impl.XSDValidator;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class XSDValidatorXXETest {
    private static final String XXE = """
            <?xml version=\"1.0\"?>
            <!DOCTYPE foo [<!ELEMENT foo ANY><!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>
            <foo>&xxe;</foo>
            """;

    @Test
    void xsdValidatorRejetteEntitesExternes() {
        XSDValidator validator = new XSDValidator();
        ByteArrayInputStream input = new ByteArrayInputStream(XXE.getBytes(StandardCharsets.UTF_8));
        ValidationResult result = validator.validate(input);
        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
        assertTrue(result.getErrors().get(0).getMessage().contains("DOCTYPE"));
    }
}
