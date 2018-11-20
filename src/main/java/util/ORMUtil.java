package util;

import annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
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
                if (annotation instanceof Table && ((Table) annotation).name().equals(tableName)) {
                    return c.getDeclaringClass();
                }
            }
        }
        return null;
    }

    public static List<String> getFieldsInDBByClass(Class cl) {
        Field[] fields = cl.getDeclaredFields();
        List<String> fieldsNameInDb = new ArrayList<>();
        for (Field field : fields) {
            Annotation[] annotations = field.getDeclaredAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation instanceof Id) {
                    fieldsNameInDb.add(((Id) annotation).name());
                } else if (annotation instanceof PrimaryKey) {
                    fieldsNameInDb.add(((PrimaryKey) annotation).name());

                } else if (annotation instanceof Column) {
                    fieldsNameInDb.add(((Column) annotation).name());

                }
//                else if (annotation instanceof OneToOne && !((OneToOne) annotation).mappedBy().isEmpty()) {
//                    fieldsNameInDb.add(getTableNameByClass(((OneToOne) annotation).targetEntity()) + " id");
//                } else if (annotation instanceof ManyToOne) {
//                    fieldsNameInDb.add(getTableNameByClass(((ManyToOne) annotation).targetEntity()) + " id");
//                }

            }
        }
        return fieldsNameInDb;
    }
}
