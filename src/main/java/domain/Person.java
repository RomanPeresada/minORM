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
    @JoinTable(name = "person_car",
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

    public void setId(long id) {
        this.id = id;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setCars(List<Car> cars) {
        this.cars = cars;
    }

    @Override
    public String toString() {
        return "Person{" +
                "id=" + id +
                ", login='" + login + '\'' +
                ", name='" + name + '\'' +
                ", password='" + password + '\'' +
                ", cars=" + cars +
                '}';
    }
}
