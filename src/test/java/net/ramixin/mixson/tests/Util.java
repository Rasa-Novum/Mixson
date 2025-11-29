package net.ramixin.mixson.tests;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.server.packs.resources.Resource;

import java.io.ByteArrayInputStream;
import java.util.Random;
import java.util.UUID;

public interface Util {

    Random RANDOM = new Random();

    static Resource createDummyResource(JsonElement element) {
        return new Resource(null, () -> new ByteArrayInputStream(element.toString().getBytes()), null);
    }

    static JsonObject generateRandomJsonObject() {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", UUID.randomUUID().toString());
        obj.addProperty("count", RANDOM.nextInt(1000));
        obj.addProperty("active", RANDOM.nextBoolean());
        obj.addProperty("score", RANDOM.nextDouble() * 100);

        JsonArray arr = new JsonArray();
        int size = RANDOM.nextInt(5) + 1;
        for (int i = 0; i < size; i++) {
            arr.add(new JsonPrimitive(UUID.randomUUID().toString().substring(0, 8)));
        }
        obj.add("tags", arr);

        return obj;
    }

}
