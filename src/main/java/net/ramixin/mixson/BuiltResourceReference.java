package net.ramixin.mixson;

import com.google.gson.JsonElement;
import net.minecraft.util.Identifier;

public class BuiltResourceReference {

    private JsonElement resource;

    private final Identifier referenceId;

    private final Identifier resourceId;

    protected BuiltResourceReference(Identifier resourceId, Identifier referenceId) {
        this.resourceId = resourceId;
        this.referenceId = referenceId;
    }

    public JsonElement consume() {
        if(resource == null) throw new MixsonError("BuiltResourceReference ('"+ referenceId +"') was used before the resource '"+resourceId+"' was loaded");
        JsonElement ret = resource;
        resource = null;
        return ret;
    }

    protected void fulfill(JsonElement elem) {
        this.resource = elem.deepCopy();
    }

    public Identifier getReferenceId() {
        return referenceId;
    }
}
