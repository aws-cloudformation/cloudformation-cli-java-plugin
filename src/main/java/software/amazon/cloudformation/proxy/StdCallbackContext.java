/*
* Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License").
* You may not use this file except in compliance with the License.
* A copy of the License is located at
*
*  http://aws.amazon.com/apache2.0
*
* or in the "license" file accompanying this file. This file is distributed
* on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
* express or implied. See the License for the specific language governing
* permissions and limitations under the License.
*/
package software.amazon.cloudformation.proxy;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.concurrent.ThreadSafe;

/**
 * StdCallbackContext provide a mechanism that automatically provides the
 * memoization for retention and callback of request, responses, stabilize
 * handles during handler invocations. During replay callbacks, this
 * automatically replays responses for different calls along the call graph
 * ensuring that we only execute the portions of the call graph that needs
 * execution and dedupe calls as needed.
 *
 * This is not a sophisticated class that does request inspection based call
 * result, it is primarily a function result memoization that is ensured that it
 * is invoked once. Attempts to call the function multiple times with different
 * arguments will yield the same result for the same call graph key for
 * {@link StdCallbackContext#request(String, Function)} and
 * {@link StdCallbackContext#response(String, BiFunction)}. For
 * {@link StdCallbackContext#stabilize(String, CallChain.Callback)}, only when
 * True is returned it is memoized.
 */
@ThreadSafe
@lombok.EqualsAndHashCode
@lombok.ToString
public class StdCallbackContext {

    public static class Serializer extends JsonSerializer<Map<String, Object>> {
        @Override
        public void serialize(Map<String, Object> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            writeMap(value, gen, serializers);
        }

        @SuppressWarnings("unchecked")
        private void writeObject(Object val, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (val == null) {
                gen.writeNull();
                return;
            }

            // Primitive
            if (val instanceof String || val instanceof Number || val instanceof Boolean) {
                gen.writeObject(val);
                return;
            }

            // Encode object type information
            gen.writeStartArray();
            Class<?> type = val.getClass();
            // write class name first
            gen.writeString(type.getName());
            // the write value next
            if (val instanceof Collection<?>) {
                writeCollection((Collection<?>) val, gen, serializers);
            } else if (val instanceof Map<?, ?>) {
                Map<?, ?> map = (Map<?, ?>) val;
                writeMap(map, gen, serializers);
            } else {
                JsonSerializer<Object> serializer = serializers.findValueSerializer(type);
                serializer.serialize(val, gen, serializers);
            }
            // end marker
            gen.writeEndArray();
        }

        private void writeCollection(Collection<?> collection, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
            gen.writeStartArray();
            for (Object each : collection) {
                writeObject(each, gen, serializers);
            }
            gen.writeEndArray();
        }

        private void writeMap(Map<?, ?> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            for (Map.Entry<?, ?> each : value.entrySet()) {
                Object key = each.getKey();
                if (!(key instanceof String)) {
                    throw new JsonGenerationException("Expected string key got " + key.getClass(), gen);
                }
                gen.writeFieldName((String) each.getKey());
                writeObject(each.getValue(), gen, serializers);
            }
            gen.writeEndObject();
        }

    }

    public static class Deserializer extends JsonDeserializer<Map<String, Object>> {
        @Override
        public Map<String, Object> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return readMap(LinkedHashMap.class, p, ctxt);
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> readMap(Class<?> type, JsonParser p, DeserializationContext ctxt) throws IOException {
            if (!p.isExpectedStartObjectToken()) {
                throw new JsonParseException(p, "Expected start of object for Map got " + p.currentToken());
            }
            try {
                Map<String, Object> value = (Map<String, Object>) type.getDeclaredConstructor().newInstance();
                JsonToken next = p.nextToken();
                while (next != JsonToken.END_OBJECT) {
                    if (next != JsonToken.FIELD_NAME) {
                        throw new JsonParseException(p, "Key was not present " + next);
                    }
                    String key = p.currentName();
                    p.nextToken(); // position to next
                    Object val = readObject(p, ctxt);
                    value.put(key, val);
                    next = p.nextToken();
                }
                return value;
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                throw new JsonMappingException(p, "Can not create empty map for class " + type + " @ " + p.getCurrentLocation(),
                                               e);
            }
        }

        private Object readObject(JsonParser p, DeserializationContext ctxt) throws IOException,
            NoSuchMethodException,
            InvocationTargetException {
            Object val = null;
            JsonToken next = p.currentToken();
            switch (next) {
                // Primitive Types
                case VALUE_TRUE:
                case VALUE_FALSE:
                    val = p.getValueAsBoolean();
                    break;

                case VALUE_STRING:
                    val = p.getText();
                    break;

                case VALUE_NUMBER_FLOAT:
                case VALUE_NUMBER_INT:
                    val = p.getNumberValue();
                    break;

                // Encoded Object information
                case START_ARRAY:
                    val = readEncoded(p, ctxt);
                    break;

                default:
                    throw new JsonParseException(p, "Object encoding not understood " + next);
            }
            return val;
        }

        private Object readEncoded(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (!p.isExpectedStartArrayToken()) {
                throw new JsonParseException(p, "Expected array for encoded object got " + p.currentToken());
            }

            Object value = null;
            JsonToken next = p.nextToken();
            if (next != JsonToken.VALUE_STRING) {
                throw new JsonParseException(p, "Encoded Class value not present " + next);
            }
            String typeName = p.getText();
            p.nextToken(); // fwd to next
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader == null) {
                loader = getClass().getClassLoader();
            }
            try {
                Class<?> type = loader.loadClass(typeName);
                if (Collection.class.isAssignableFrom(type)) {
                    value = readCollection(type, p, ctxt);
                } else if (Map.class.isAssignableFrom(type)) {
                    value = readMap(type, p, ctxt);
                } else {
                    JsonDeserializer<Object> deser = ctxt.findRootValueDeserializer(ctxt.constructType(type));
                    value = deser.deserialize(p, ctxt);
                }
            } catch (ClassNotFoundException e) {
                throw new JsonParseException(p, "Type name encoded " + typeName + " could not be loaded", e);
            }
            if (p.nextToken() != JsonToken.END_ARRAY) {
                throw new JsonParseException(p, "Encoded expected end of ARRAY marker " + p.currentToken());
            }
            return value;
        }

        @SuppressWarnings("unchecked")
        private Object readCollection(Class<?> type, JsonParser p, DeserializationContext ctxt) throws IOException {
            if (!p.isExpectedStartArrayToken()) {
                throw new JsonParseException(p, "Expected array for encoded object got " + p.currentToken());
            }
            try {
                Collection<Object> value = (Collection<Object>) type.getDeclaredConstructor().newInstance();
                p.nextToken(); // move to next token
                do {
                    Object val = readObject(p, ctxt);
                    value.add(val);
                } while (p.nextToken() != JsonToken.END_ARRAY);
                return value;
            } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                throw new IOException("Can not create empty constructor collection class " + type + " @ "
                    + p.getCurrentLocation(), e);
            }
        }
    }

    /*
     * Uses a LinkedHashMap to preserve the order of calls within a set of
     * callGraphs. If things interleave in terms of entries then it means that the
     * context was being used in different threads.
     */
    @JsonDeserialize(using = Deserializer.class)
    @JsonSerialize(using = Serializer.class)
    private Map<String, Object> callGraphs = Collections.synchronizedMap(new LinkedHashMap<>(10));

    @SuppressWarnings("unchecked")
    public <M, R> Function<M, R> request(String callGraph, Function<M, R> func) {
        return (m) -> (R) callGraphs.computeIfAbsent(callGraph + ".request", (ign) -> func.apply(m));
    }

    @SuppressWarnings("unchecked")
    public <R> R evictRequestRecord(String callGraph) {
        return (R) callGraphs.remove(callGraph + ".request");
    }

    @SuppressWarnings("unchecked")
    public <R, C, RT> BiFunction<R, C, RT> response(String callGraph, BiFunction<R, C, RT> func) {
        return (r, c) -> (RT) callGraphs.computeIfAbsent(callGraph + ".response", (ign) -> func.apply(r, c));
    }

    public Map<String, Object> callGraphs() {
        return Collections.unmodifiableMap(callGraphs);
    }

    @SuppressWarnings("unchecked")
    public <ResponseT> ResponseT response(String callGraph) {
        return (ResponseT) callGraphs.get(callGraph + ".response");
    }

    @SuppressWarnings("unchecked")
    public <RequestT> RequestT findFirstRequestByContains(String contains) {
        return (RequestT) findFirst((key) -> key.contains(contains) && key.endsWith(".request"));
    }

    @SuppressWarnings("unchecked")
    public <RequestT> List<RequestT> findAllRequestByContains(String contains) {
        return (List<RequestT>) findAll((key) -> key.contains(contains) && key.endsWith(".request"));
    }

    @SuppressWarnings("unchecked")
    public <ResponseT> ResponseT findFirstResponseByContains(String contains) {
        return (ResponseT) findFirst((key) -> key.contains(contains) && key.endsWith(".response"));
    }

    @SuppressWarnings("unchecked")
    public <ResponseT> List<ResponseT> findAllResponseByContains(String contains) {
        return (List<ResponseT>) findAll((key) -> key.contains(contains) && key.endsWith(".response"));
    }

    Object findFirst(Predicate<String> contains) {
        Objects.requireNonNull(contains);
        return callGraphs.entrySet().stream().filter(e -> contains.test(e.getKey())).findFirst().map(Map.Entry::getValue)
            .orElse(null);

    }

    List<Object> findAll(Predicate<String> contains) {
        Objects.requireNonNull(contains);
        return callGraphs.entrySet().stream().filter(e -> contains.test(e.getKey())).map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

    <RequestT, ResponseT, ClientT, ModelT, CallbackT extends StdCallbackContext>
        CallChain.Callback<RequestT, ResponseT, ClientT, ModelT, CallbackT, Boolean>
        stabilize(String callGraph, CallChain.Callback<RequestT, ResponseT, ClientT, ModelT, CallbackT, Boolean> callback) {
        return (request1, response1, client, model, context) -> {
            final String key = callGraph + ".stabilize";
            Boolean result = (Boolean) callGraphs.getOrDefault(key, Boolean.FALSE);
            if (!result) {
                //
                // The StdCallbackContext can be shared. However the call to stabilize for a
                // given content
                // is usually confined to one thread. If for some reason we spread that across
                // threads, the
                // worst that can happen is a double compute for stabilize. This isn't the
                // intended pattern.
                // Why are we changing it from computeIfAbsent pattern? For the callback we send
                // in the
                // StdCallbackContext which can be used to add things into context. That will
                // lead to
                // ConcurrentModificationExceptions when the compute running added things into
                // context when
                // needed
                //
                result = callback.invoke(request1, response1, client, model, context);
                if (result) {
                    callGraphs.put(key, Boolean.TRUE);
                }
            }
            return result;
        };
    }

    public int attempts(String callGraph) {
        return (Integer) callGraphs.computeIfAbsent(callGraph + ".attempts", (ign) -> 1);
    }

    public void attempts(String callGraph, int attempts) {
        callGraphs.put(callGraph + ".attempts", attempts);
    }

    @VisibleForTesting
    void setCallGraphs(LinkedHashMap<String, Object> graphs) {
        this.callGraphs = Collections.synchronizedMap(new LinkedHashMap<>(graphs));
    }
}
