package net.ramixin.mixson.util.functions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@FunctionalInterface
public interface ResourceExporter<T> {

    ByteArrayOutputStream export(T resource) throws IOException;

}
