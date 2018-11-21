package service;

import annotation.*;
import javafx.util.Pair;
import util.ORMUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.*;

import static config.ConnectionWithDb.getConnection;

public class EntityManager {
    private static EntityManager entityManager;

    private EntityManager() {
    }

    public static EntityManager getInstance() {
        if (entityManager == null)
            entityManager = new EntityManager();
        return entityManager;
    }

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

    public <T> T findById(Class cl, long id) throws SQLException, NoSuchFieldException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        String tableName = ORMUtil.getTableNameByClass(cl);
        String query = "SELECT * FROM " + tableName + " WHERE id=" + id + ";";
        System.out.println(query);
        T object = (T) cl.newInstance();
        try (Statement statement = getConnection().createStatement()) {
            ResultSet resultSet = statement.executeQuery(query);
            while (resultSet.next()) {

                Field[] fields = cl.getDeclaredFields();
                List<Pair<String, Type>> fieldsInDB = ORMUtil.getFieldsInDBByClass(cl);
                for (int i = 0; i < fieldsInDB.size(); i++) {
                    Field currentField = fields[i];
                    int counter = 1;
                    while (counter < fields.length && !currentField.getGenericType().equals(fieldsInDB.get(i).getValue())) {
                        currentField = fields[i + counter];
                        counter++;
                    }
                    String nameOfMethod = "set" + String.valueOf(currentField.getName().charAt(0)).toUpperCase() + currentField.getName().substring(1);
                    Method method = cl.getDeclaredMethod(nameOfMethod, currentField.getType());
                    String type = currentField.getType().toString();
                    String value = resultSet.getString(fieldsInDB.get(i).getKey());
                    Object param = getParamByType(type, value);
                    method.invoke(object, param);
                }

            }
        }
        return object;
    }

    public <T> List<T> findAll(Class currentClass) throws SQLException, IllegalAccessException, InstantiationException, NoSuchMethodException, NoSuchFieldException, ClassNotFoundException, InvocationTargetException {
        String tableName = ORMUtil.getTableNameByClass(currentClass);
        String query = "SELECT * FROM " + tableName + ";";
        System.out.println(query);
        List<T> objects = new ArrayList<>();
        try (Statement statement = getConnection().createStatement()) {
            ResultSet resultSet = statement.executeQuery(query);
            while (resultSet.next()) {
                T object = (T) currentClass.newInstance();
                Field[] fields = currentClass.getDeclaredFields();
                List<Pair<String, Type>> fieldsInDB = ORMUtil.getFieldsInDBByClassWithRelationship(currentClass);
                for (int i = 0; i < fieldsInDB.size(); i++) {
                    Field currentField = fields[i];
                    int counter = 1;
                    while (counter < fields.length && !currentField.getGenericType().equals(fieldsInDB.get(i).getValue())) {
                        currentField = fields[i + counter];
                        counter++;
                    }
                    String nameOfMethod = "set" + String.valueOf(currentField.getName().charAt(0)).toUpperCase() + currentField.getName().substring(1);
                    Method method = currentClass.getDeclaredMethod(nameOfMethod, currentField.getType());
                    String type = currentField.getType().toString();
                    Object param;
                    String[] params = fieldsInDB.get(i).getKey().split(" ");
                    if (params.length == 2) {
                        param = getParamForReferenceEntity(currentClass, params, resultSet, fieldsInDB.get(i).getValue().getTypeName());
                    }
                    //  else if (params.length == 5) {
                    //      param = getDependsEntityForManyToMany(params, resultSet.getString("id"));
                    //      param = null;
                    //  }
                    else {
                        String value = resultSet.getString(fieldsInDB.get(i).getKey());
                        param = getParamByType(type, value);
                    }
                    method.invoke(object, param);
                }
                objects.add(object);
            }
        }
        return objects;
    }

    private Object getParamForReferenceEntity(Class currentClass, String[] arr, ResultSet resultSet, String type) throws NoSuchMethodException, InstantiationException, NoSuchFieldException, SQLException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        Object param;

        if (ORMUtil.checkFieldInClass(currentClass, arr[1])) {
            param = getMainEntity(arr, resultSet);
        } else {
            if (type.contains("List")) {
                param = getDependsEntity(arr, resultSet.getString("id"));
            } else {
                param = !getDependsEntity(arr, resultSet.getString("id")).isEmpty() ?
                        getDependsEntity(arr, resultSet.getString("id")).get(0) : null;
            }
        }
        return param;
    }

    private <T> T getMainEntity(String[] params, ResultSet resultSet) throws SQLException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, NoSuchFieldException {
        String idOtherEntityInString = resultSet.getString(params[1]);
        if (idOtherEntityInString != null && !idOtherEntityInString.toLowerCase().contains("null")) {
            Class classOfOtherEntity = ORMUtil.getClassByTableName(params[0]);
            long idOtherEntity = Long.parseLong(idOtherEntityInString);
            return findById(classOfOtherEntity, idOtherEntity);
        }
        return null;
    }

    private <T> List<T> getDependsEntity(String[] params, String idThisEntityInString) throws SQLException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchFieldException, ClassNotFoundException {
        Class clEntity = ORMUtil.getClassByTableName(params[0]);
        T object = (T) clEntity.newInstance();
        List<T> list = new ArrayList<>();
        long idCurrentEntity = Long.parseLong(idThisEntityInString);
        String query = "SELECT * from " + params[0] + " where " + params[1] + "=" + idCurrentEntity + ";";
        System.out.println(query);
        try (Statement statement1 = getConnection().createStatement()) {
            ResultSet resultSet = statement1.executeQuery(query);
            while (resultSet.next()) {
                Field[] fields = clEntity.getDeclaredFields();
                List<Pair<String, Type>> fieldsInDB = ORMUtil.getFieldsInDBByClass(clEntity);
                for (int i = 0; i < fieldsInDB.size(); i++) {
                    Field currentField = fields[i];
                    int counter = 1;
                    while (counter < fields.length && !currentField.getGenericType().equals(fieldsInDB.get(i).getValue())) {
                        currentField = fields[i + counter];
                        counter++;
                    }
                    String nameOfMethod = "set" + String.valueOf(currentField.getName().charAt(0)).toUpperCase() + currentField.getName().substring(1);
                    Method method = clEntity.getDeclaredMethod(nameOfMethod, currentField.getType());
                    String value = resultSet.getString(fieldsInDB.get(i).getKey());
                    Object param = getParamByType(currentField.getType().toString(), value);
                    method.invoke(object, param);
                }
                list.add(object);
            }
        }
        return list;
    }

    private static Pair<String, String> getPrimaryKey(Field field, Annotation annotation, Object object) throws
            IllegalAccessException {
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
        columnField.setAccessible(true);
        Field inverseColumnField;
        inverseColumnField = classOfOtherEntity.get(0).getClass().getDeclaredField("id");
        inverseColumnField.setAccessible(true);
        for (Object o : classOfOtherEntity) {
            StringBuilder builder = new StringBuilder("INSERT INTO ").append(tableNameManyToMany).append(" VALUES(")
                    .append(columnField.get(object)).append(",").append(inverseColumnField.get(o).toString()).append(");");
            queriesForManyToManyTable.add(builder.toString());
        }
        return queriesForManyToManyTable;
    }

    private <T> List<T> getDependsEntityForManyToMany(String[] params, String idThisEntityInString) throws SQLException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchFieldException, ClassNotFoundException {
        Class clEntity = ORMUtil.getClassByTableName(params[2]);
        T object = (T) clEntity.newInstance();
        List<T> list = new ArrayList<>();
        long idCurrentEntity = Long.parseLong(idThisEntityInString);
        String query = "SELECT * from " + params[0] + " l JOIN " + params[1] + " s ON l.id=" + params[3] + " AND " + params[3] +
                "=" + idCurrentEntity + "  JOIN " + params[2]
                + " r ON r.id=" + params[4] + ";";
        System.out.println(query);
        try (Statement statement1 = getConnection().createStatement()) {
            ResultSet resultSet = statement1.executeQuery(query);
            Field[] fields = clEntity.getDeclaredFields();
            List<Pair<String, Type>> fieldsInDB = ORMUtil.getFieldsInDBByClass(clEntity);
            for (int i = 0; i < fieldsInDB.size() && resultSet.next(); i++) {
                Field currentField = fields[i];
                int counter = 1;
                while (counter < fields.length && !currentField.getGenericType().equals(fieldsInDB.get(i).getValue())) {
                    currentField = fields[i + counter];
                    counter++;
                }
                String nameOfMethod = "set" + String.valueOf(currentField.getName().charAt(0)).toUpperCase() + currentField.getName().substring(1);
                Method method = clEntity.getDeclaredMethod(nameOfMethod, currentField.getType());
                String value = resultSet.getString("r.id");
                Object param = getParamByType(currentField.getType().toString(), value);
                method.invoke(object, param);
            }
            list.add(object);
        }
        return list;
    }

    private static Object getParamByType(String type, String value) {
        Object param = value;
        if (type.toLowerCase().contains("int")) {
            param = Integer.valueOf(value);
        } else if (type.toLowerCase().contains("long")) {
            param = Long.valueOf(value);
        } else if (type.toLowerCase().contains("double")) {
            param = Double.valueOf(value);
        } else if (type.toLowerCase().contains("localdate")) {
            param = LocalDate.parse(value);
        } else if (type.toLowerCase().contains("boolean")) {
            param = value.equals("1");
        } else if (type.toLowerCase().contains("string")) {
            param = String.valueOf(value);
        }
        return param;
    }

}
