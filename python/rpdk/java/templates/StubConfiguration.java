package {{ package_name }};

import java.util.Map;
import org.json.JSONObject;
import org.json.JSONTokener;

class Configuration extends BaseConfiguration {

    public Configuration() {
        super("{{ schema_file_name }}");
    }

    public JSONObject resourceSchemaJSONObject() {
        return new JSONObject(new JSONTokener(this.getClass().getClassLoader().getResourceAsStream(schemaFilename)));
    }

    /**
     * Providers should implement this method if their resource has a 'Tags' property to define resource-level tags
     * @return
     */
    public Map<String, String> resourceDefinedTags(final {{ pojo_name }} resourceModel) {
        return null;
    }

}
