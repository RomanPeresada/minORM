package domain;

import annotation.*;

import java.util.List;

@Table(name = "manufacturer")
public class Manufacturer {
    @Id
    private long id;

    @Column(name = "country", unique = true, nullable = false, length = 30)
    private String country;

    @OneToMany(mappedBy = "manufacturer", targetEntity = Car.class)
    private List<Car> cars;

    @OneToOne(mappedBy = "manufacturer", targetEntity = Director.class)
    private Director director;

    public Manufacturer() {
    }

    public Manufacturer(long id, String country, List<Car> cars, Director director) {
        this.id = id;
        this.country = country;
        this.cars = cars;
        this.director = director;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setCars(List<Car> cars) {
        this.cars = cars;
    }

    public void setDirector(Director director) {
        this.director = director;
    }

    @Override
    public String toString() {
        return "Manufacturer{" +
                "id=" + id +
                ", country='" + country + '\'' +
                ", cars=" + cars +
                ", director=" + director +
                '}';
    }
}
