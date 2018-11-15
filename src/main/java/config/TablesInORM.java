package config;


import annotation.Column;
import annotation.Id;
import annotation.Table;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static config.ConnectionWithDb.getConnection;

public class TablesInORM {
    private static String pathToEntities = null;
    private final static Logger log = Logger.getLogger(TablesInORM.class);


    public static void autoCreatingTablesAfterStartOfProgram() throws ClassNotFoundException, SQLException, IOException {
        getInfoFromProperties();
        String[] classesName = new File(pathToEntities).list();
        pathToEntities = replaceSymbolsInPathForReflect(pathToEntities);
        if (classesName != null) {
            log.debug("Classes were found");
            for (String name : classesName) {
                Class foundClass = Class.forName(pathToEntities + name.replaceAll(".java", ""));
                String query = getQueryForCreateTable(foundClass);
                try (Statement statement = getConnection().createStatement()) {
                    int res = statement.executeUpdate(query);
                    log.debug(res == 1 ? "query was competed : " + query: "table exists already with name : " + getTableName(foundClass));
                }
            }
        }
    }

    private static String getQueryForCreateTable(Class foundClass) {
        String tableName = getTableName(foundClass);
        StringBuilder primaryKey = new StringBuilder(" PRIMARY KEY(");
        boolean doesExistPrimaryKey = false;
        List<FieldPropertiesInDatabase> fieldsForDatabase = getFieldsPropertiesNecessaryForTableInDb(foundClass);
        StringBuilder builder = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(tableName).append("(");
        for (FieldPropertiesInDatabase field : fieldsForDatabase) {
            if (field.isPrimary()) {
                primaryKey.append(field.getName()).append(")");
                builder.append(getRowWithAutoIncrementForPrimaryKey(field));
                doesExistPrimaryKey = true;
            } else {
                builder.append(getRowForUsualField(field));
            }
        }
        builder = doesExistPrimaryKey ? builder.append(primaryKey) : builder.deleteCharAt(builder.length() - 1);
        builder.append(");");
       // System.out.println(builder.toString());
        return builder.toString();
    }

    private static StringBuilder getRowForUsualField(FieldPropertiesInDatabase field) {
        StringBuilder builder = new StringBuilder();
        String type = getTypeForField(field);
        builder.append(field.getName())
                .append(" ").append(type).append(field.getType().equals("VARCHAR") ? "(" + field.getLength() + ")" : "")
                .append(field.isPrimary() ? " AUTO_INCREMENT " : " ")
                .append(!field.isNullable() ? "NOT NULL " : "")
                .append(field.isUnique() ? "UNIQUE," : ",");
        return builder;
    }

    private static StringBuilder getRowWithAutoIncrementForPrimaryKey(FieldPropertiesInDatabase field) {
        StringBuilder builder = new StringBuilder();
        String type = getTypeForField(field);
        builder.append(field.getName())
                .append(" ").append(type).append(field.getType().equals("VARCHAR") ? "(" + field.getLength() + ")" : "")
                .append(field.isPrimary() && !field.getType().equals("VARCHAR") ? " AUTO_INCREMENT, " : " ");
        return builder;
    }

    private static String getTableName(Class<?> c) {
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

    private static String replaceSymbolsInPathForReflect(String fullPathWhereAreEntity) {
        fullPathWhereAreEntity = fullPathWhereAreEntity.replaceAll("[/\\\\]", ".");
        for (int i = 0; i < 3; i++) {
            int indexFirstDot = fullPathWhereAreEntity.indexOf(".");
            fullPathWhereAreEntity = fullPathWhereAreEntity.substring(indexFirstDot + 1);
        }
        return fullPathWhereAreEntity;
    }

    private static List<FieldPropertiesInDatabase> getFieldsPropertiesNecessaryForTableInDb(Class<?> c) {
        List<FieldPropertiesInDatabase> fieldsInDb = new ArrayList<>();
        Field[] fields = c.getDeclaredFields();
        for (Field field : fields) {
            Annotation[] annotations = field.getDeclaredAnnotations();
            for (Annotation currentAnnotation : annotations) {
                FieldPropertiesInDatabase fieldInDatabase = new FieldPropertiesInDatabase();
                if (currentAnnotation instanceof Id) {
                    fieldInDatabase.setName(((Id) currentAnnotation).name());
                    fieldInDatabase.setPrimary(true);
                    fieldInDatabase.setType(field.getType().toString().endsWith("String") ? "VARCHAR" : field.getType().toString());
                }
                if (currentAnnotation instanceof Column) {
                    fieldInDatabase.setName(((Column) currentAnnotation).name());
                    fieldInDatabase.setType(field.getType().toString().endsWith("String") ? "VARCHAR" : field.getType().toString());
                    fieldInDatabase.setUnique(((Column) currentAnnotation).unique());
                    fieldInDatabase.setNullable(((Column) currentAnnotation).nullable());
                    fieldInDatabase.setLength(((Column) currentAnnotation).length());
                }
                fieldsInDb.add(fieldInDatabase);
            }
        }
        return fieldsInDb;
    }

    private static String getTypeForField(FieldPropertiesInDatabase field) {
        String typeInDb = field.getType();
        if (typeInDb.endsWith("String")) {
            typeInDb = "VARCHAR";
        } else if (typeInDb.endsWith("long")) {
            typeInDb = "BIGINT";
        }
        return typeInDb;
    }


    private static void getInfoFromProperties() throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream(new File("src/main/resources/db.properties")));
        pathToEntities = properties.get("pathToEntities").toString();
    }
}
