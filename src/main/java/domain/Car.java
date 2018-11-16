package domain;

import annotation.*;
import domain.Person;

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

    @OneToOne(targetEntity = Person.class)
    private Person owner;
}
