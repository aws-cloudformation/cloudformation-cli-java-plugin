package {{ package_name }};

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

public class ResourceModelTest {
    @Test
    public void test_ResourceModel_SimpleSuccess() {
        final ResourceModel model = ResourceModel.builder().build();

        assertThat(model.getPrimaryIdentifier(), is(not(nullValue())));
        assertThat(model.getPrimaryIdentifier().keySet(), hasSize(0));
        assertThat(model.getAdditionalIdentifiers(), is(not(nullValue())));
        assertThat(model.getAdditionalIdentifiers(), hasSize(0));
    }
}
