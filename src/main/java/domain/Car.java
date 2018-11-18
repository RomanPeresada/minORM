package domain;

import annotation.*;
import domain.Person;

import java.util.List;

@Table(name = "car")
public class Car {
    @Id
    private long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "is_changed_color")
    private boolean isChangedColor;

    @ManyToMany(targetEntity = Person.class, mappedBy = "cars")
    private List<Person> owners;

    @ManyToOne(targetEntity = Manufactured.class)
    private Manufactured manufactured;
}
