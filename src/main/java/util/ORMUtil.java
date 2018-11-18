package util;

import annotation.Table;

import java.lang.annotation.Annotation;

public class ORMUtil {
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
}
