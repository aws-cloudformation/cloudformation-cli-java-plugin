// This is a generated file. Modifications will be overwritten.
package {{ package_name }};

import com.google.common.collect.ImmutableMap;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.json.JSONTokener;

@Data
public abstract class BaseHookConfiguration {

    private final Map<String, String> targetSchemaPaths = new HashMap<>();
    private final Map<String, InputStream> targetSchemas = new HashMap<>();
    protected final String schemaFilename;

    public BaseHookConfiguration(final String schemaFilename) {
        this.schemaFilename = schemaFilename;
        initialiseTargetSchemaFilePaths();
    }

    private void initialiseTargetSchemaFilePaths() {
    {% for target_name, schema_path in target_schema_paths.items() %}
        this.targetSchemaPaths.put("{{target_name}}", "{{schema_path}}");
    {% endfor %}
    }

    public JSONObject hookSchemaJSONObject() {
        return new JSONObject(new JSONTokener(this.getClass().getClassLoader().getResourceAsStream(schemaFilename)));
    }

    public JSONObject targetSchemaJSONObject(final String targetName) {
        if (!this.targetSchemaPaths.containsKey(targetName)) {
            return null;
        }

        return new JSONObject(
            new JSONTokener(
                this.targetSchemas.computeIfAbsent(
                    targetName,
                    tn -> this.getClass().getClassLoader().getResourceAsStream(this.targetSchemaPaths.get(tn))
                )
            )
        );
    }

    public Map<String, String> getTargetSchemaPaths() {
        return ImmutableMap.copyOf(this.targetSchemaPaths);
    }

    public Map<String, InputStream> getTargetSchemas() {
        return ImmutableMap.copyOf(this.targetSchemas);
    }
}
