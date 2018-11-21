import config.TablesInORM;
import domain.Car;
import domain.Director;
import domain.Manufacturer;
import domain.Person;
import service.EntityManager;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException, NoSuchFieldException, InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        TablesInORM.autoCreatingTablesAfterStartOfProgram();
        EntityManager entityManager = EntityManager.getInstance();
//        Manufacturer manufactured = new Manufacturer(1L, "Italy", null, null);
//        entityManager.createObject(manufactured);
//        Director director = new Director(2L, LocalDate.now(), 25, null, 4325.23, "new job");
//        entityManager.createObject(director);
//        manufactured.setDirector(director);
//        entityManager.updateObject(manufactured);
//        director.setManufacturer(manufactured);
//        entityManager.updateObject(director);
//        Car car = new Car(3L, "Opel", 2012, false, null, manufactured);
//        entityManager.createObject(car);
//        Car car2 = new Car(4L, "Toyota", 2015, false, null, manufactured);
//        entityManager.createObject(car2);
//        manufactured.setCars(Arrays.asList(car, car2));
//        entityManager.updateObject(manufactured);
//        Person person = new Person(19L, "login223", "name2", "paseds", Arrays.asList(car, car2));
//        entityManager.createObject(person);
//        Person person2 = new Person(20L, "logidd", "name22", "password", Arrays.asList(car, car2));
//        entityManager.createObject(person2);
//        car.setOwners(Arrays.asList(person, person2));
//        entityManager.updateObject(car);
//        car2.setOwners(Arrays.asList(person, person2));
//        entityManager.updateObject(car2);
//        entityManager.deleteObject(director);
        System.out.println(entityManager.findAll(Director.class));
        System.out.println(entityManager.findAll(Manufacturer.class));
        System.out.println(entityManager.findAll(Car.class));
        System.out.println(entityManager.findAll(Person.class));

    }
}
