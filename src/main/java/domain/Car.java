package domain;

import annotation.Column;
import annotation.Id;
import annotation.Table;

@Table(name = "car")
public class Car {
    @Id
    private int id;

    @Column(name = "name",nullable = false)
    private String name;

    @Column(name = "year",nullable = false)
    private int year;

    @Column(name = "is_changed_color")
    private boolean isChangedColor;

}
