package bastel.de.fahrtenschreiber.pojo;

import java.time.LocalDate;

public class TripEntry {


    private String driver;
    private Integer odo;
    private LocalDate date;
    private Integer row;


    public TripEntry(String driver, Integer odo, LocalDate date, Integer row) {
        this.driver = driver;
        this.odo = odo;
        this.date = date;
        this.row = row;
    }


    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public Integer getOdo() {
        return odo;
    }

    public void setOdo(Integer odo) {
        this.odo = odo;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getRow() {
        return row;
    }

    public void setRow(Integer row) {
        this.row = row;
    }

    @Override
    public String toString() {
        return "" + driver +
                " - " + date +
                " - " + odo +
                " - row: " + row;
    }
}
