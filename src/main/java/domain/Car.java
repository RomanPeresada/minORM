package domain;

import annotation.*;

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

    @ManyToOne(targetEntity = Manufacturer.class)
    private Manufacturer manufacturer;

    public Car(long id, String name, int year, boolean isChangedColor, List<Person> owners, Manufacturer manufacturer) {
        this.id = id;
        this.name = name;
        this.year = year;
        this.isChangedColor = isChangedColor;
        this.owners = owners;
        this.manufacturer = manufacturer;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public void setIsChangedColor(boolean changedColor) {
        isChangedColor = changedColor;
    }

    public void setOwners(List<Person> owners) {
        this.owners = owners;
    }

    public void setManufacturer(Manufacturer manufacturer) {
        this.manufacturer = manufacturer;
    }

    @Override
    public String toString() {
        return "Car{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", year=" + year +
                ", isChangedColor=" + isChangedColor +
                ", owners=" + owners +
                ", manufacturer=" + manufacturer +
                '}';
    }

    public Car() {
    }

    public void setId(long id) {
        this.id = id;
    }
}
