package util;

import annotation.Table;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

public class ORMUtil {

    private static String pathToEntities;

    static {
        try {
            Properties properties = new Properties();
            properties.load(new FileInputStream(new File("src/main/resources/db.properties")));
            pathToEntities = properties.get("pathToEntities").toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getTableNameByClass(Class<?> c) {
        String tableName = "";
        Annotation[] annotations = c.getDeclaredAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation instanceof Table) {
                tableName = ((Table) annotation).name();
                break;
            }
        }
        return tableName;
    }

    public static Class<?> getClassByTableName(String tableName) throws ClassNotFoundException {
        File file = new File(pathToEntities);
        for (String s : Objects.requireNonNull(file.list())) {
           Class c = Class.forName(s);
            Annotation[] annotations = c.getDeclaredAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation instanceof Table &&((Table) annotation).name().equals(tableName) ) {
                        return c.getDeclaringClass();
                }
            }
        }
        return null;
    }
}
