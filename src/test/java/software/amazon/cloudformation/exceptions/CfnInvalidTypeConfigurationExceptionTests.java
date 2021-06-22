package software.amazon.cloudformation.exceptions;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import org.junit.jupiter.api.Test;

public class CfnInvalidTypeConfigurationExceptionTests {
    
    @Test
    public void cfnInvalidTypeConfigurationException_isBaseHandlerException() {
        assertThatExceptionOfType(BaseHandlerException.class).isThrownBy(() -> {
            throw new CfnInvalidTypeConfigurationException("AWS::Type::Resource", "<request>");
        }).withCauseInstanceOf(RuntimeException.class).withMessageContaining("<request>")
            .withMessageContaining("AWS::Type::Resource")
            .withMessageContaining("Invalid TypeConfiguration");
    }

    @Test
    public void cfnInvalidTypeConfigurationException_noCauseGiven() {
        assertThatExceptionOfType(CfnInvalidTypeConfigurationException.class).isThrownBy(() -> {
            throw new CfnInvalidTypeConfigurationException("AWS::Type::Resource", "<request>");
        }).withNoCause().withMessageContaining("<request>").withMessageContaining("AWS::Type::Resource")
            .withMessageContaining("Invalid TypeConfiguration");
    }
}