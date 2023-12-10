package com.github.hanfeng21050.utils;

import java.io.*;
import java.util.List;

public class ObjectUtil {
    public static <T extends Serializable> T deepCopy(T original) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(original);

        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bis);

        @SuppressWarnings("unchecked")
        T copy = (T) in.readObject();

        return copy;
    }

    public static <T extends Serializable> List<T> deepCopyList(List<T> original) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(original);

        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bis);

        @SuppressWarnings("unchecked")
        List<T> copy = (List<T>) in.readObject();

        return copy;
    }
}
