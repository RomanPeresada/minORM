import config.TablesInORM;
import domain.Car;
import domain.Director;
import domain.Manufactured;
import domain.Person;
import service.EntityManager;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException, NoSuchFieldException, InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        //ConnectionWithDb.getConnection().close();
        TablesInORM.autoCreatingTablesAfterStartOfProgram();
        EntityManager entityManager = new EntityManager();
        Manufactured manufactured = new Manufactured(1L, "Italy", null);
        System.out.println(entityManager.findAll(Manufactured.class));
        System.out.println(entityManager.findAll(Director.class));


        //        entityManager.createObject(manufactured);
//        entityManager.createObject(manufactured);
//        Person person = new Person(7L, "login22343", "name", "pass", null);
//        Car car = new Car(62L, "Mazda", 2017, false, null, manufactured);
//        entityManager.createObject(car);
//        person.setCars(Arrays.asList(car));
//        entityManager.createObject(person);
//        Director director = new Director(1L, manufactured);
//        entityManager.createObject(director);
    }
}
