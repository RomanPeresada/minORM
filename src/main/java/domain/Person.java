package domain;

import annotation.*;

import java.util.List;

@Table(name = "person")
public class Person {

    @Id
    private long id;

    @Column(name = "login")
    private String login;

    @Column(name = "name")
    private String name;

    @Column(name = "password", nullable = false, length = 40)
    private String password;

    @ManyToMany(targetEntity = Car.class)
    @JoinTable(name = "cars_people",
            joinColumn = "person_id",
            inverseJoinColumn = "car_id")
    private List<Car> cars;

    public Person(long id, String login, String name, String password, List<Car> cars) {
        this.id = id;
        this.login = login;
        this.name = name;
        this.password = password;
        this.cars = cars;
    }
}
