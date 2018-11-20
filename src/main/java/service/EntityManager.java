package service;

import annotation.*;
import javafx.util.Pair;
import util.ORMUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.*;

import static config.ConnectionWithDb.getConnection;

public class EntityManager {


    public <T> boolean createObject(T object) throws SQLException, IllegalAccessException, NoSuchFieldException {
        String tableName = ORMUtil.getTableNameByClass(object.getClass());
        Field fieldForManyToManyMapped = null, fieldForManyToManyWithTable = null;
        Annotation annotationForManyToMany = null, annotationForManyToManyWithTable = null;
        List<Pair<String, String>> valuesTable = new ArrayList<>();
        for (Field field : object.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Annotation[] annotations = field.getDeclaredAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation instanceof Column) {
                    String value = field.get(object).toString();
                    if (value.equals("true") || value.equals("false")) {
                        value = value.equals("true") ? "1" : "0";
                    }
                    valuesTable.add(new Pair<>(((Column) annotation).name(), value));
                } else if (annotation instanceof PrimaryKey) {
                    valuesTable.add(new Pair<>(((PrimaryKey) annotation).name(), field.get(object).toString()));
                } else if (annotation instanceof Id) {
                    valuesTable.add(new Pair<>(((Id) annotation).name(), field.get(object).toString()));
                } else if ((annotation instanceof OneToOne && ((OneToOne) annotation).mappedBy().isEmpty()
                        || annotation instanceof ManyToOne) && field.get(object) != null) {
                    Field fieldOtherEntity = field.get(object).getClass().getDeclaredField("id");
                    fieldOtherEntity.setAccessible(true);
                    valuesTable.add(new Pair<>(field.getName() + "_id", fieldOtherEntity.get(field.get(object)).toString()));
                } else if (annotation instanceof ManyToMany && !((ManyToMany) annotation).mappedBy().isEmpty() && field.get(object) != null) {
                    fieldForManyToManyMapped = field;
                    annotationForManyToMany = annotation;
                } else if (annotation instanceof JoinTable) {
                    fieldForManyToManyWithTable = field;
                    annotationForManyToManyWithTable = annotation;
                }
            }
        }
        int res;
        try (Statement statement = getConnection().createStatement()) {
            String query = getQueryForCreateObject(tableName, valuesTable).toString();
            System.out.println(query);
            res = statement.executeUpdate(query);
        }
        if (fieldForManyToManyMapped != null) {
            executeQueries(getQueriesForCreatingManyToManyTableWithMapped(fieldForManyToManyMapped, object, annotationForManyToMany));
        }
        if (fieldForManyToManyWithTable != null) {
            executeQueries(getQueriesForCreatingManyToManyTableWithTable(fieldForManyToManyWithTable, object, annotationForManyToManyWithTable));
        }
        return res > 0;
    }

    public <T> boolean updateObject(T object) throws SQLException, IllegalAccessException, NoSuchFieldException {
        String tableName = ORMUtil.getTableNameByClass(object.getClass());
        List<Pair<String, String>> primaryKeys = new ArrayList<>();
        List<Pair<String, String>> valuesForMainTable = new ArrayList<>();
        for (Field field : object.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Annotation[] annotations = field.getDeclaredAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation instanceof Column) {
                    String value = field.get(object).toString();
                    if (value.equals("true") || value.equals("false")) {
                        value = value.equals("true") ? "1" : "0";
                    }
                    valuesForMainTable.add(new Pair<>(((Column) annotation).name(), value));
                } else if ((annotation instanceof OneToOne && ((OneToOne) annotation).mappedBy().isEmpty()
                        || annotation instanceof ManyToOne) && field.get(object) != null) {
                    Field fieldOtherEntity = field.get(object).getClass().getDeclaredField("id");
                    fieldOtherEntity.setAccessible(true);
                    valuesForMainTable.add(new Pair<>(field.getName() + "_id", fieldOtherEntity.get(field.get(object)).toString()));
                }
                Pair<String, String> pair = getPrimaryKey(field, annotation, object);
                if (pair != null) {
                    primaryKeys.add(pair);
                }
            }
        }

        try (Statement statement = getConnection().createStatement()) {
            String query = getQueryForUpdateObject(tableName, primaryKeys, valuesForMainTable).toString();
            System.out.println(query);
            return statement.executeUpdate(query) > 0;
        }
    }

    public <T> boolean deleteObject(T object) throws IllegalAccessException, SQLException {
        String tableName = ORMUtil.getTableNameByClass(object.getClass());
        List<Pair<String, String>> primaryKeys = new ArrayList<>();
        for (Field field : object.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Annotation[] annotations = field.getDeclaredAnnotations();
            for (Annotation annotation : annotations) {
                Pair<String, String> pair = getPrimaryKey(field, annotation, object);
                if (pair != null) {
                    primaryKeys.add(pair);
                }
            }
        }

        int res;
        try (Statement statement = getConnection().createStatement()) {
            String query = getQueryForDeleteObject(tableName, primaryKeys).toString();
            System.out.println(query);
            res = statement.executeUpdate(query);
        }
        return res > 0;
    }

    public <T> List<T> findAll(Class cl) throws SQLException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        String tableName = ORMUtil.getTableNameByClass(cl);
        String query = "SELECT * FROM " + tableName + ";";
        System.out.println(query);
        List<T> objects = new ArrayList<>();
        try (Statement statement = getConnection().createStatement()) {
            ResultSet resultSet = statement.executeQuery(query);
            while (resultSet.next()) {
                T object = (T) cl.newInstance();
                Field[] fields = cl.getDeclaredFields();
                List<String> fieldsInDB = ORMUtil.getFieldsInDBByClass(cl);
                for (int i = 0; i < fieldsInDB.size(); i++) {
                    String nameOfMethod = "set" + String.valueOf(fields[i].getName().charAt(0)).toUpperCase() + fields[i].getName().substring(1);
                    Method method = cl.getDeclaredMethod(nameOfMethod, fields[i].getType());
                    String type = fields[i].getType().toString();
                    String value = resultSet.getString(fieldsInDB.get(i));
                    System.out.println(fieldsInDB.get(i) + " = " + value);
                    Object param = value;
                    if (type.toLowerCase().contains("int")) {
                        param = Integer.valueOf(value);
                    } else if (type.toLowerCase().contains("long")) {
                        param = Long.valueOf(value);
                    } else if (type.toLowerCase().contains("double")) {
                        param = Double.valueOf(value);
                    }
                    else if (type.toLowerCase().contains("localdate")) {
                        param = LocalDate.parse(value);
                    }
                    method.invoke(object, param);
                }
                objects.add(object);

            }
        }
        return objects;

    }


    private static Pair<String, String> getPrimaryKey(Field field, Annotation annotation, Object object) throws IllegalAccessException {
        if (annotation instanceof Id) {
            String key = ((Id) annotation).name();
            String value = field.get(object).toString();
            return new Pair<>(key, value);
        } else if (annotation instanceof PrimaryKey) {
            String key = ((PrimaryKey) annotation).name();
            String value = field.get(object).toString();
            return new Pair<>(key, value);
        }
        return null;
    }

    private static StringBuilder getQueryForCreateObject(String tableName, List<Pair<String, String>> valuesForMainTable) {
        StringBuilder fields = new StringBuilder("INSERT INTO ").append(tableName).append("(");
        StringBuilder values = new StringBuilder("VALUES('");
        for (Pair<String, String> pair : valuesForMainTable) {
            fields.append(pair.getKey()).append(",");
            values.append(pair.getValue()).append("','");
        }
        return fields.deleteCharAt(fields.length() - 1).append(") ")
                .append(values.deleteCharAt(values.length() - 1).deleteCharAt(values.length() - 1).append(");"));
    }

    private static StringBuilder getQueryForUpdateObject(String tableName, List<Pair<String, String>> primaryKeys, List<Pair<String, String>> valuesForMainTable) {
        StringBuilder fields = new StringBuilder("UPDATE ").append(tableName).append(" SET ");
        for (Pair<String, String> pair : valuesForMainTable) {
            fields.append(pair.getKey()).append("=").append("'").append(pair.getValue()).append("',");
        }
        fields.deleteCharAt(fields.length() - 1).append(" WHERE ");
        for (Pair<String, String> primaryKey : primaryKeys) {
            fields.append(primaryKey.getKey()).append("=").append(primaryKey.getValue()).append(" and ");
        }
        return fields.delete(fields.length() - 5, fields.length() - 1).append(";");

    }

    private static StringBuilder getQueryForDeleteObject(String tableName, List<Pair<String, String>> primaryKeys) {
        StringBuilder fields = new StringBuilder("DELETE FROM ").append(tableName).append(" WHERE ");
        for (Pair<String, String> primaryKey : primaryKeys) {
            fields.append(primaryKey.getKey()).append("=").append(primaryKey.getValue()).append(" and ");
        }
        return fields.delete(fields.length() - 5, fields.length() - 1).append(";");
    }

    private static void executeQueries(List<String> queries) throws SQLException {
        for (String query : queries) {
            System.out.println(query);
            try (Statement statement = getConnection().createStatement()) {
                statement.executeUpdate(query);
            }
        }
    }

    private static List<String> getQueriesForCreatingManyToManyTableWithMapped(Field currentField, Object
            currentObject, Annotation currentAnnotation) throws NoSuchFieldException, IllegalAccessException {
        List<String> queriesForManyToManyTable = new ArrayList<>();
        String tableNameManyToMany = null;
        String mappedBy = ((ManyToMany) currentAnnotation).mappedBy();
        List classOfOtherEntity = ((List) currentField.get(currentObject));

        Field fieldManyToManyOtherEntity = classOfOtherEntity.get(0).getClass().getDeclaredField(mappedBy);
        Annotation[] annotationsInOtherEntity = fieldManyToManyOtherEntity.getDeclaredAnnotations();
        for (Annotation otherAnnotation : annotationsInOtherEntity) {
            if (otherAnnotation instanceof JoinTable) {
                tableNameManyToMany = ((JoinTable) otherAnnotation).name();
            }
        }
        Field inverseColumnField = currentField.getDeclaringClass().getDeclaredField("id");
        Field columnField = classOfOtherEntity.get(0).getClass().getDeclaredField("id");
        columnField.setAccessible(true);
        inverseColumnField.setAccessible(true);
        for (Object o : classOfOtherEntity) {
            StringBuilder builder = new StringBuilder("INSERT INTO ").append(tableNameManyToMany).append(" VALUES(")
                    .append(columnField.get(o)).append(",").append(inverseColumnField.get(currentObject).toString()).append(");");
            queriesForManyToManyTable.add(builder.toString());
        }
        return queriesForManyToManyTable;
    }

    private static List<String> getQueriesForCreatingManyToManyTableWithTable(Field field, Object
            object, Annotation annotation) throws IllegalAccessException, NoSuchFieldException {
        List<String> queriesForManyToManyTable = new ArrayList<>();
        String tableNameManyToMany = ((JoinTable) annotation).name();
        List classOfOtherEntity = ((List) field.get(object));

        Field columnField = field.getDeclaringClass().getDeclaredField("id");
        Field inverseColumnField = classOfOtherEntity.get(0).getClass().getDeclaredField("id");
        columnField.setAccessible(true);
        inverseColumnField.setAccessible(true);
        for (Object o : classOfOtherEntity) {
            StringBuilder builder = new StringBuilder("INSERT INTO ").append(tableNameManyToMany).append(" VALUES(")
                    .append(columnField.get(object)).append(",").append(inverseColumnField.get(o).toString()).append(");");
            queriesForManyToManyTable.add(builder.toString());
        }
        return queriesForManyToManyTable;
    }
}
