// This is a generated file. Modifications will be overwritten.
package {{ package_name }};

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class {{ pojo_name|uppercase_first_letter }} {

    public static String TYPE_NAME = "{{ type_name }}";

    {% for name, type in properties.items() %}
    @JsonProperty("{{ name }}")
    private {{ type }} {{ name|lowercase_first_letter }};

    {% endfor %}
}
