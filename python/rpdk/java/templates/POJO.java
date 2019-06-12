// This is a generated file. Modifications will be overwritten.
package {{ package_name }};

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.json.JSONObject;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class {{ pojo_name|uppercase_first_letter }} {

    public static final String TYPE_NAME = "{{ type_name }}";

    {% for name, type in properties.items() %}
    @JsonProperty("{{ name }}")
    private {{ type }} {{ name|lowercase_first_letter }};

    {% endfor %}

    {% if primaryIdentifier is not none %}
    public JSONObject getPrimaryIdentifier(){
        final JSONObject identifier = new JSONObject();
        {% for identifier in primaryIdentifier %}
        {% set components = identifier.split("/") %}
        identifier.append("{{ components[-1] }}", this{% for component in components[2:] %}.get{{component|uppercase_first_letter}}(){% endfor %});
        {% endfor %}
        return identifier;
    }
    {% endif %}

    {% if additionalIdentifiers is not none %}
    public List<JSONObject> getAdditionalIdentifiers(){
        final List<JSONObject> identifiers = new ArrayList<JSONObject>();
        {% for identifiers in additionalIdentifiers %}
        identifiers.add(new JSONObject()
        {% for identifier in identifiers %}
        {% set components = identifier.split("/") %}
                .accumulate("{{ components[-1] }}", this{% for component in components[2:] %}.get{{component|uppercase_first_letter}}(){% endfor %})
        {% endfor %}
        );
        {% endfor %}
        return identifiers;
    }
    {% endif %}

}
