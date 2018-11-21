package util;

import annotation.*;
import javafx.util.Pair;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
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
            s = replaceSymbolsInPathForReflect(pathToEntities + s);
            Class c = Class.forName(s.replaceAll(".java", ""));
            Annotation[] annotations = c.getDeclaredAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation instanceof Table && ((Table) annotation).name().equals(tableName)) {
                    return c;
                }
            }
        }
        return null;
    }

    public static List<Pair<String, Type>> getFieldsInDBByClassWithRelationship(Class cl) throws NoSuchFieldException {
        Field[] fields = cl.getDeclaredFields();
        List<Pair<String, Type>> fieldsNameInDb = new ArrayList<>();
        for (Field field : fields) {
            Class targetEntity = null;
            Annotation[] annotations = field.getDeclaredAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation instanceof Id) {
                    fieldsNameInDb.add(new Pair<>(((Id) annotation).name(), field.getGenericType()));
                } else if (annotation instanceof PrimaryKey) {
                    fieldsNameInDb.add(new Pair<>(((PrimaryKey) annotation).name(), field.getGenericType()));
                } else if (annotation instanceof Column) {
                    fieldsNameInDb.add(new Pair<>(((Column) annotation).name(), field.getGenericType()));
                } else if (annotation instanceof OneToOne && ((OneToOne) annotation).mappedBy().isEmpty()) {
                    String tableOfOtherClass = getTableNameByClass(((OneToOne) annotation).targetEntity());
                    fieldsNameInDb.add(new Pair<>(tableOfOtherClass + " " + field.getName() + "_id", field.getGenericType()));
                } else if (annotation instanceof OneToOne && !((OneToOne) annotation).mappedBy().isEmpty()) {
                    String tableOfOtherClass = getTableNameByClass(((OneToOne) annotation).targetEntity());
                    fieldsNameInDb.add(new Pair<>(tableOfOtherClass + " " +
                            ((OneToOne) annotation).mappedBy() + "_id", field.getGenericType()));
                } else if (annotation instanceof ManyToOne) {
                    String tableOfOtherClass = getTableNameByClass(((ManyToOne) annotation).targetEntity());
                    fieldsNameInDb.add(new Pair<>(tableOfOtherClass + " " + field.getName() + "_id", field.getGenericType()));
                }
//                else if (annotation instanceof OneToMany) {
//                    String tableOfOtherClass = getTableNameByClass(((OneToMany) annotation).targetEntity());
//                    fieldsNameInDb.add(new Pair<>(tableOfOtherClass + " " + ((OneToMany) annotation).mappedBy() + "_id", field.getGenericType()));
//                } else if (annotation instanceof ManyToMany ) {
//                    targetEntity = ((ManyToMany) annotation).targetEntity();
//                    if (!((ManyToMany) annotation).mappedBy().isEmpty()) {
//                        String tableOfCurrentClass = getTableNameByClass(cl);
//                        String tableOfOtherClass = getTableNameByClass(((ManyToMany) annotation).targetEntity());
//                        Field fieldOfOtherEntity = ((ManyToMany) annotation).targetEntity().getDeclaredField(((ManyToMany) annotation).mappedBy());
//                        String inverseColumn = fieldOfOtherEntity.getAnnotation(JoinTable.class).inverseJoinColumn();
//                        String column = fieldOfOtherEntity.getAnnotation(JoinTable.class).joinColumn();
//                        String manyToManyTable = fieldOfOtherEntity.getAnnotation(JoinTable.class).name();
//                        fieldsNameInDb.add(new Pair<>((tableOfCurrentClass + " " + manyToManyTable + " " + tableOfOtherClass + " " + inverseColumn + " " + column),
//                                field.getGenericType()));
//                    }
//                } else if (annotation instanceof JoinTable) {
//                    String tableOfCurrentClass = getTableNameByClass(cl);
//                    String column = field.getAnnotation(JoinTable.class).joinColumn();
//                    String inverseColumn = field.getAnnotation(JoinTable.class).inverseJoinColumn();
//                    String manyToManyTable = field.getAnnotation(JoinTable.class).name();
//                    String tableOfOtherClass = getTableNameByClass(targetEntity);
//                    fieldsNameInDb.add(new Pair<>((tableOfCurrentClass + " "+ manyToManyTable + " " +tableOfOtherClass  +" " + column + " "  + inverseColumn),
//                            field.getGenericType()));
//                }
            }
        }
        return fieldsNameInDb;
    }

    public static List<Pair<String, Type>> getFieldsInDBByClass(Class cl) throws NoSuchFieldException {
        Field[] fields = cl.getDeclaredFields();
        List<Pair<String, Type>> fieldsNameInDb = new ArrayList<>();
        for (Field field : fields) {
            Annotation[] annotations = field.getDeclaredAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation instanceof Id) {
                    fieldsNameInDb.add(new Pair<>(((Id) annotation).name(), field.getGenericType()));
                } else if (annotation instanceof PrimaryKey) {
                    fieldsNameInDb.add(new Pair<>(((PrimaryKey) annotation).name(), field.getGenericType()));
                } else if (annotation instanceof Column) {
                    fieldsNameInDb.add(new Pair<>(((Column) annotation).name(), field.getGenericType()));
                }
            }
        }
        return fieldsNameInDb;
    }

    public static String replaceSymbolsInPathForReflect(String fullPathWhereAreEntity) {
        fullPathWhereAreEntity = fullPathWhereAreEntity.replaceAll("[/\\\\]", ".");
        for (int i = 0; i < 3; i++) {
            int indexFirstDot = fullPathWhereAreEntity.indexOf(".");
            fullPathWhereAreEntity = fullPathWhereAreEntity.substring(indexFirstDot + 1);
        }
        return fullPathWhereAreEntity;
    }

    public static boolean checkFieldInClass(Class cl, String column) {
        for (Field field : cl.getDeclaredFields()) {
            if ((field.getName() + "_id").equals(column)) return true;
        }
        return false;
    }
}
