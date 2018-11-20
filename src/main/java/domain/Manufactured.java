package domain;

import annotation.*;

import java.util.List;

@Table(name = "manufactured")
public class Manufactured {
    @Id
    private long id;

    @Column(name = "country", unique = true, nullable = false, length = 30)
    private String country;

    @OneToMany(mappedBy = "manufactured", targetEntity = Car.class)
    private List<Car> cars;

   // @OneToOne(mappedBy = "manufactured", targetEntity = Director.class)
    private Director director;

    public Manufactured() {
    }

    public Manufactured(long id, String country, List<Car> cars) {
        this.id = id;
        this.country = country;
        this.cars = cars;
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
        return "Manufactured{" +
                "id=" + id +
                ", country='" + country + '\'' +
                ", cars=" + cars +
                ", director=" + director +
                '}';
    }
}
