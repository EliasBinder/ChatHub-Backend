package it.eliasandandrea.chathubbackend.util;

import java.io.*;

public final class ObjectByteConverter {

    private ObjectByteConverter() {
    }

    public static byte[] serialize(Object obj) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ObjectOutputStream os = new ObjectOutputStream(out)) {
            os.writeObject(obj);
            return out.toByteArray();
        } catch (IOException e) {
            Log.warning("Could not serialize object: " + obj, e);
        }
        return null;
    }

    public static Object deserialize(byte[] data) {
        if (data == null || data.length == 0) {
            Log.warning("Deserializing null payload!");
            return null;
        }
        final ByteArrayInputStream in = new ByteArrayInputStream(data);
        try (final ObjectInputStream is = new ObjectInputStream(in)) {
            return is.readObject();
        } catch (IOException | ClassNotFoundException e) {
            Log.warning("Could not deserialize object", e);
        }
        return null;
    }
}