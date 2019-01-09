package com.aws.cfn.resource;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;

public class Validator<T> {

    public void validateModel(final T model,
                              final String schemaPath) throws IOException {

        try (InputStream inputStream = getClass().getResourceAsStream(schemaPath)) {
            final SchemaLoader loader = SchemaLoader.builder()
                .schemaJson(new JSONObject(new JSONTokener(inputStream)))
                .draftV7Support()
                .build();
            final Schema schema = loader.load().build();

            schema.validate(new JSONObject("{\"hello\" : \"world\"}")); // throws a ValidationException if this object is invalid
        } catch (final IOException e) {
            e.printStackTrace();
            throw e;
        }
    }

}
