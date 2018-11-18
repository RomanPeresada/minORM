package service;

import config.ConnectionWithDb;
import util.ORMUtil;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EntityManager {


    public <T> T createObject(T object) throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        String tableName = ORMUtil.getTableNameByClass(object.getClass());
        //  T newObj = (T) object.getClass().newInstance();
        List<String> values = new ArrayList<>();
        for (Field field : object.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            values.add(field.get(object) != null ? field.get(object).toString() : "null");
        }
        System.out.println(values);

        try (Connection connection = ConnectionWithDb.getConnection()) {

        }
        return object;
    }
}
