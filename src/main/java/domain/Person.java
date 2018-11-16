package domain;

import annotation.Column;
import annotation.Id;
import annotation.OneToMany;
import annotation.Table;

import java.util.List;

@Table(name = "person")
public class Person {
    @Id
    private long id;

    @Column(name = "login", unique = true, nullable = false, length = 30)
    String login;

    @Column(name = "password", nullable = false, length = 40)
    private String password;

//    @OneToMany(targetEntity = Car.class)
//    private List<Car> cars;
}
