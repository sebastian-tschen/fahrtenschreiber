package bastel.de.fahrtenschreiber.pojo;

import java.time.LocalDate;

public class TripEntry {


    private final String comment;
    private String driver;
    private Integer odo;
    private LocalDate date;
    private Integer row;


    public TripEntry(String driver, Integer odo, LocalDate date, String comment, Integer row) {
        this.driver = driver;
        this.odo = odo;
        this.date = date;
        this.row = row;
        this.comment = comment;
    }


    public String getDriver() {
        return driver;
    }

    public Integer getOdo() {
        return odo;
    }

    public LocalDate getDate() {
        return date;
    }

    public Integer getRow() {
        return row;
    }

    @Override
    public String toString() {
        return "" + driver +
                " - " + date +
                " - " + odo +
                " - " + comment +
                " - row: " + row;
    }

    public String getComment() {
        return comment;
    }
}
