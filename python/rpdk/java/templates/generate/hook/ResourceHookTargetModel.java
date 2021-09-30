// This is a generated file. Modifications will be overwritten.
package {{ package_name }};

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.json.JSONObject;
import software.amazon.cloudformation.proxy.hook.targetmodel.ResourceHookTargetModel;


@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
@JsonDeserialize(as = {{ model_name|uppercase_first_letter }}TargetModel.class)
public class {{ model_name|uppercase_first_letter }}TargetModel extends ResourceHookTargetModel<{{model_name}}> {

    @JsonIgnore
    private static final TypeReference<{{model_name}}> TARGET_REFERENCE =
        new TypeReference<{{model_name}}>() {};

    @JsonIgnore
    private static final TypeReference<{{model_name}}TargetModel> MODEL_REFERENCE =
        new TypeReference<{{model_name}}TargetModel>() {};

    @JsonIgnore
    public static final String TARGET_TYPE_NAME = "{{ type_name }}";


    @JsonIgnore
    public TypeReference<{{model_name}}> getHookTargetTypeReference() {
        return TARGET_REFERENCE;
    }

    @JsonIgnore
    public TypeReference<{{model_name}}TargetModel> getTargetModelTypeReference() {
        return MODEL_REFERENCE;
    }
}
