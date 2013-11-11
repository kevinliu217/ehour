package net.rrm.ehour.ui.timesheet.dto;


import java.io.Serializable;
import java.util.Date;

public class TimesheetDate implements Serializable {
    private final Date date;
    private final int dayInWeek;
    private final String formatted;
    private final boolean locked;

    TimesheetDate(Date date, int dayInWeek, String formatted, boolean locked) {
        this.date = date;
        this.dayInWeek = dayInWeek;
        this.formatted = formatted;
        this.locked = locked;
    }

    public Date getDate() {
        return date;
    }

    public int getDayInWeek() {
        return dayInWeek;
    }

    public String getFormatted() {
        return formatted;
    }

    public boolean isLocked() {
        return locked;
    }
}