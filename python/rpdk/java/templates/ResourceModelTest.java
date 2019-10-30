package {{ package_name }};

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class {{ pojo_name }}Test {
    @Test
    public void test_ResourceModel_SimpleSuccess() {
        final {{ pojo_name }} model = {{ pojo_name }}.builder().build();

        assertThat({{ pojo_name }}.TYPE_NAME).isEqualTo("{{ type_name }}");
        assertThat(model.getPrimaryIdentifier()).isNull();
        assertThat(model.getAdditionalIdentifiers()).isNull();
    }
}
