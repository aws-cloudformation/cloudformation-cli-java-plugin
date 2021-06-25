// This is a generated file. Modifications will be overwritten.
package {{ package_name }};

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;{{ '\nimport lombok.NoArgsConstructor;' if  no_args_constructor_required }}
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor{{ '\n@NoArgsConstructor' if no_args_constructor_required }}
@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
public class {{ model_name|uppercase_first_letter }} {
    {% for name, type in properties.items() %}
    @JsonProperty("{{ name }}")
    private {{ type|translate_type }} {{ name|lowercase_first_letter|safe_reserved }};

    {% endfor %}
}
