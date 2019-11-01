package {{ package_name }};

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigurationTest {
    @Test
    public void test_resourceSchemaJSONObject() {
        final Configuration configuration = new Configuration();

        assertThat(configuration.resourceSchemaJSONObject()).isNotNull();
    }

    @Test
    public void test_resourceDefinedTags() {
        final Configuration configuration = new Configuration();

        {{ pojo_name }} model = {{ pojo_name }}.builder().build();
        assertThat(configuration.resourceDefinedTags(model)).isNull();
    }
}
