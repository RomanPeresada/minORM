package domain;

import annotation.Column;
import annotation.Id;
import annotation.OneToMany;
import annotation.Table;

import java.util.List;

@Table(name = "manufactured")
public class Manufactured {
    @Id
    private long id;

    @Column(name = "country", unique = true, nullable = false, length = 30)
    private String country;

//    @OneToMany(mappedBy = "manufactured", targetEntity = Car.class)
//    private List<Car> cars;
}
