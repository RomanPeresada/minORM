package domain;

import annotation.Column;
import annotation.Id;
import annotation.OneToOne;
import annotation.Table;

import java.time.LocalDate;

@Table(name = "director")
public class Director {
    @Id
    private long id;

    @Column(name = "ddate")
    private LocalDate date;

    @Column(name = "age")
    private Integer age;

    @OneToOne(targetEntity = Manufacturer.class)
    private Manufacturer manufacturer;

    @Column(name = "salary")
    private double salary;

    @Column(name = "job")
    private String job;


    public Director() {
    }

    public Director(long id, LocalDate date, Integer age, Manufacturer manufacturer, double salary, String job) {
        this.id = id;
        this.date = date;
        this.age = age;
        this.manufacturer = manufacturer;
        this.salary = salary;
        this.job = job;
    }

    public Director(long id, LocalDate date, Integer age, double salary, String job) {
        this.id = id;
        this.date = date;
        this.age = age;
        this.salary = salary;
        this.job = job;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public void setSalary(double salary) {
        this.salary = salary;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public void setManufacturer(Manufacturer manufacturer) {
        this.manufacturer = manufacturer;
    }

    @Override
    public String toString() {
        return "Director{" +
                "id=" + id +
                ", date=" + date +
                ", age=" + age +
                ", manufacturer=" + manufacturer +
                ", salary=" + salary +
                ", job='" + job + '\'' +
                '}';
    }
}
