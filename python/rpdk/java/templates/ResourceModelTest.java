package {{ package_name }};

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

public class ResourceModelTest {
    @Test
    public void test_ResourceModel_SimpleSuccess() {
        final ResourceModel model = ResourceModel.builder().build();

        assertThat(ResourceModel.TYPE_NAME, is(equalTo("{{ type_name }}")));
        assertThat(model.getPrimaryIdentifier(), is(nullValue()));
        assertThat(model.getAdditionalIdentifiers(), is(nullValue()));
    }
}
