/*
@VaadinApache2LicenseForJavaFiles@
 */

package com.vaadin.terminal.gwt.client.communication;

import java.util.Map;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNull;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.Connector;
import com.vaadin.terminal.gwt.client.ConnectorMap;

/**
 * Encoder for converting RPC parameters and other values to JSON for transfer
 * between the client and the server.
 * 
 * Currently, basic data types as well as Map, String[] and Object[] are
 * supported, where maps and Object[] can contain other supported data types.
 * 
 * TODO extensible type support
 * 
 * @since 7.0
 */
public class JsonEncoder {

    public static final String VTYPE_PAINTABLE = "p";
    public static final String VTYPE_BOOLEAN = "b";
    public static final String VTYPE_DOUBLE = "d";
    public static final String VTYPE_FLOAT = "f";
    public static final String VTYPE_LONG = "l";
    public static final String VTYPE_INTEGER = "i";
    public static final String VTYPE_STRING = "s";
    public static final String VTYPE_ARRAY = "a";
    public static final String VTYPE_STRINGARRAY = "c";
    public static final String VTYPE_MAP = "m";
    // TODO this will be replaced by the shared state class name
    public static final String VTYPE_SHAREDSTATE = "t";

    // TODO is this needed?
    public static final String VTYPE_UNDEFINED = "u";

    /**
     * Encode a value to a JSON representation for transport from the client to
     * the server.
     * 
     * @param value
     *            value to convert
     * @param connectorMap
     *            mapper from connectors to connector IDs
     * @param connection
     * @return JSON representation of the value
     */
    public static JSONValue encode(Object value, ConnectorMap connectorMap,
            ApplicationConnection connection) {
        if (null == value) {
            // TODO as undefined type?
            return combineTypeAndValue(VTYPE_UNDEFINED, JSONNull.getInstance());
        } else if (value instanceof String[]) {
            String[] array = (String[]) value;
            JSONArray jsonArray = new JSONArray();
            for (int i = 0; i < array.length; ++i) {
                jsonArray.set(i, new JSONString(array[i]));
            }
            return combineTypeAndValue(VTYPE_STRINGARRAY, jsonArray);
        } else if (value instanceof String) {
            return combineTypeAndValue(VTYPE_STRING, new JSONString(
                    (String) value));
        } else if (value instanceof Boolean) {
            return combineTypeAndValue(VTYPE_BOOLEAN,
                    JSONBoolean.getInstance((Boolean) value));
        } else if (value instanceof Object[]) {
            Object[] array = (Object[]) value;
            JSONArray jsonArray = new JSONArray();
            for (int i = 0; i < array.length; ++i) {
                // TODO handle object graph loops?
                jsonArray.set(i, encode(array[i], connectorMap, connection));
            }
            return combineTypeAndValue(VTYPE_ARRAY, jsonArray);
        } else if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            JSONObject jsonMap = new JSONObject();
            for (String mapKey : map.keySet()) {
                // TODO handle object graph loops?
                Object mapValue = map.get(mapKey);
                jsonMap.put(mapKey, encode(mapValue, connectorMap, connection));
            }
            return combineTypeAndValue(VTYPE_MAP, jsonMap);
        } else if (value instanceof Connector) {
            Connector paintable = (Connector) value;
            return combineTypeAndValue(VTYPE_PAINTABLE, new JSONString(
                    connectorMap.getConnectorId(paintable)));
        } else {
            if (getTransportType(value) != VTYPE_UNDEFINED) {
                return combineTypeAndValue(getTransportType(value),
                        new JSONString(String.valueOf(value)));
            } else {
                // Try to find a generated serializer object, class name is the
                // type
                String type = value.getClass().getName();
                JSONSerializer serializer = JsonDecoder.serializerMap
                        .getSerializer(type);

                // TODO handle case with no serializer found
                return combineTypeAndValue(type,
                        serializer.serialize(value, connectorMap, connection));
            }
        }
    }

    private static JSONValue combineTypeAndValue(String type, JSONValue value) {
        JSONArray outerArray = new JSONArray();
        outerArray.set(0, new JSONString(type));
        outerArray.set(1, value);
        return outerArray;
    }

    private static String getTransportType(Object value) {
        if (value instanceof String) {
            return VTYPE_STRING;
        } else if (value instanceof Connector) {
            return VTYPE_PAINTABLE;
        } else if (value instanceof Boolean) {
            return VTYPE_BOOLEAN;
        } else if (value instanceof Integer) {
            return VTYPE_INTEGER;
        } else if (value instanceof Float) {
            return VTYPE_FLOAT;
        } else if (value instanceof Double) {
            return VTYPE_DOUBLE;
        } else if (value instanceof Long) {
            return VTYPE_LONG;
        } else if (value instanceof Enum) {
            return VTYPE_STRING; // transported as string representation
        } else if (value instanceof String[]) {
            return VTYPE_STRINGARRAY;
        } else if (value instanceof Object[]) {
            return VTYPE_ARRAY;
        } else if (value instanceof Map) {
            return VTYPE_MAP;
        }
        // TODO throw exception?
        return VTYPE_UNDEFINED;
    }

}