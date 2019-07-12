package {{ package_name }};

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ResourceModelTest {
    @Test
    public void test_ResourceModel_SimpleSuccess() {
        final ResourceModel model = ResourceModel.builder().build();

        assertThat(ResourceModel.TYPE_NAME).isEqualTo("{{ type_name }}");
        assertThat(model.getPrimaryIdentifier()).isNull();
        assertThat(model.getAdditionalIdentifiers()).isNull();
    }
}
