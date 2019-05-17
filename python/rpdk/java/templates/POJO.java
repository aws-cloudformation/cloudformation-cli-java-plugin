// This is a generated file. Modifications will be overwritten.
package {{ package_name }};

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class {{ pojo_name|uppercase_first_letter }} {
    {% for name, type in properties.items() %}
    @JsonProperty("{{ name }}")
    private {{ type }} {{ name|lowercase_first_letter }};

    {% endfor %}
}
