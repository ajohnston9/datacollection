package edu.fordham.cis.wisdm.actipebble;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * Created by susannegeorge on 8/6/15.
 */
public class AccelerationRecordSerializer implements JsonSerializer<AccelerationRecord> {

    @Override
    public JsonElement serialize(AccelerationRecord src, Type typeOfSrc, JsonSerializationContext context) {
        JsonArray array = new JsonArray();
        array.add(new JsonPrimitive(src.getTimestamp()));
        array.add(new JsonPrimitive(src.getX()));
        array.add(new JsonPrimitive(src.getY()));
        array.add(new JsonPrimitive(src.getZ()));
        return array;
    }
}