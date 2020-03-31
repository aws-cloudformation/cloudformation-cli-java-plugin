package {{ package_name }};

import java.util.Map;
import org.json.JSONObject;
import org.json.JSONTokener;

class Configuration extends BaseConfiguration {

    public Configuration() {
        super("{{ schema_file_name }}");
    }
}
