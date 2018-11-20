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

    public Car(long id, String name, int year, boolean isChangedColor, List<Person> owners, Manufactured manufactured) {
        this.id = id;
        this.name = name;
        this.year = year;
        this.isChangedColor = isChangedColor;
        this.owners = owners;
        this.manufactured = manufactured;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public void setChangedColor(boolean changedColor) {
        isChangedColor = changedColor;
    }

    public void setOwners(List<Person> owners) {
        this.owners = owners;
    }

    public void setManufactured(Manufactured manufactured) {
        this.manufactured = manufactured;
    }

    @Override
    public String toString() {
        return "Car{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", year=" + year +
                ", isChangedColor=" + isChangedColor +
                ", owners=" + owners +
                ", manufactured=" + manufactured +
                '}';
    }
}
