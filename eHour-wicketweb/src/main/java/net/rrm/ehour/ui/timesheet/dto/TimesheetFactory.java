/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package net.rrm.ehour.ui.timesheet.dto;

import com.google.common.collect.Lists;
import net.rrm.ehour.config.EhourConfig;
import net.rrm.ehour.data.DateRange;
import net.rrm.ehour.domain.Customer;
import net.rrm.ehour.domain.ProjectAssignment;
import net.rrm.ehour.domain.TimesheetEntry;
import net.rrm.ehour.timesheet.dto.WeekOverview;
import net.rrm.ehour.ui.timesheet.util.TimesheetRowComparator;
import net.rrm.ehour.util.DateUtil;

import java.util.*;

/**
 * Generates the timesheet backing object
 */

public class TimesheetFactory {
    private final EhourConfig config;
    private final WeekOverview weekOverview;

    public TimesheetFactory(EhourConfig config, WeekOverview weekOverview) {
        this.config = config;
        this.weekOverview = weekOverview;
    }

    /**
     * Create timesheet form
     */
    public Timesheet createTimesheet() {
        List<Date> dateSequence = DateUtil.createDateSequence(weekOverview.getWeekRange(), config);
        List<TimesheetDate> timesheetDates = createTimesheetDates(dateSequence, weekOverview.getLockedDays());

        Timesheet timesheet = new Timesheet();
        timesheet.setMaxHoursPerDay(config.getCompleteDayHours());

        List<TimesheetRow> timesheetRows = createTimesheetRows(weekOverview.getAssignmentMap(), timesheetDates, weekOverview.getProjectAssignments(), timesheet);

        timesheet.setCustomers(structureRowsPerCustomer(timesheetRows));
        timesheet.setTimesheetDates(timesheetDates.toArray(new TimesheetDate[7]));
        timesheet.setWeekStart(weekOverview.getWeekRange().getDateStart());
        timesheet.setWeekEnd(weekOverview.getWeekRange().getDateEnd());

        timesheet.setComment(weekOverview.getComment());
        timesheet.setUser(weekOverview.getUser());

        return timesheet;
    }

    private List<TimesheetDate> createTimesheetDates(List<Date> dateSequence, Collection<Date> lockedDays) {
        List<String> formattedLockedDays = formatLockedDays(lockedDays);

        List<TimesheetDate> dates = new ArrayList<TimesheetDate>();

        for (Date date : dateSequence) {
            Calendar calendar = DateUtil.getCalendar(config);
            calendar.setTime(date);
            String formattedDate = weekOverview.formatter.format(date);
            boolean locked = formattedLockedDays.contains(formattedDate);

            dates.add(new TimesheetDate(date, calendar.get(Calendar.DAY_OF_WEEK) - 1, formattedDate, locked));
        }

        return dates;
    }

    private List<String> formatLockedDays(Collection<Date> lockedDays) {
        List<String> formattedLockedDays = Lists.newArrayList();

        for (Date lockedDay : lockedDays) {
            formattedLockedDays.add(weekOverview.formatter.format(lockedDay));
        }

        return formattedLockedDays;
    }

    private SortedMap<Customer, List<TimesheetRow>> structureRowsPerCustomer(List<TimesheetRow> rows) {
        SortedMap<Customer, List<TimesheetRow>> customerMap = new TreeMap<Customer, List<TimesheetRow>>();

        for (TimesheetRow timesheetRow : rows) {
            Customer customer = timesheetRow.getProjectAssignment().getProject().getCustomer();

            List<TimesheetRow> timesheetRows = customerMap.containsKey(customer) ? customerMap.get(customer) : new ArrayList<TimesheetRow>();
            timesheetRows.add(timesheetRow);

            customerMap.put(customer, timesheetRows);
        }

        sortTimesheetRows(customerMap);

        return customerMap;
    }

    private void sortTimesheetRows(SortedMap<Customer, List<TimesheetRow>> rows) {
        Set<Map.Entry<Customer, List<TimesheetRow>>> entries = rows.entrySet();

        for (Map.Entry<Customer, List<TimesheetRow>> entry : entries) {
            Collections.sort(entry.getValue(), TimesheetRowComparator.INSTANCE);
        }
    }

    private List<TimesheetRow> createTimesheetRows(Map<ProjectAssignment, Map<String, TimesheetEntry>> assignmentMap,
                                                   List<TimesheetDate> timesheetDates,
                                                   List<ProjectAssignment> validProjectAssignments,
                                                   Timesheet timesheet) {
        List<TimesheetRow> timesheetRows = new ArrayList<TimesheetRow>();
        Calendar firstDate = DateUtil.getCalendar(config);
        firstDate.setTime(timesheetDates.get(0).getDate());

        for (ProjectAssignment assignment : assignmentMap.keySet()) {
            TimesheetRow timesheetRow = new TimesheetRow(config);
            timesheetRow.setTimesheet(timesheet);
            timesheetRow.setProjectAssignment(assignment);
            timesheetRow.setFirstDayOfWeekDate(firstDate);

            // create a cell for every requested timesheetDate
            for (TimesheetDate timesheetDate : timesheetDates) {
                TimesheetEntry entry = assignmentMap.get(assignment).get(timesheetDate.getFormatted());

                timesheetRow.addTimesheetCell(timesheetDate.getDayInWeek(), createTimesheetCell(assignment, entry, timesheetDate.getDate(), timesheetDate.isLocked(), validProjectAssignments));
            }

            timesheetRows.add(timesheetRow);
        }

        return timesheetRows;
    }

    /**
     * Create timesheet cell, a cell is valid when the timesheetDate is within the assignment valid range
     */
    private TimesheetCell createTimesheetCell(ProjectAssignment assignment,
                                              TimesheetEntry entry,
                                              Date date,
                                              Boolean locked,
                                              List<ProjectAssignment> validProjectAssignments) {
        TimesheetCell cell = new TimesheetCell();

        cell.setTimesheetEntry(entry);
        cell.setValid(isCellValid(assignment, validProjectAssignments, date));

        cell.setLocked(locked);
        cell.setDate(date);

        return cell;
    }

    /**
     * Check if the cell is still valid. Even if they're in the timesheet entries it can be that time allotted
     * assignments are over their budget or default assignments are de-activated
     */
    private boolean isCellValid(ProjectAssignment assignment,
                                List<ProjectAssignment> validProjectAssignments,
                                Date date) {
        // first check if it's in valid project assignments (time allotted can have values
        // but not be valid anymore)
        boolean isValid = validProjectAssignments.contains(assignment);

        DateRange dateRange = new DateRange(assignment.getDateStart(), assignment.getDateEnd());

        isValid = isValid && DateUtil.isDateWithinRange(date, dateRange);

        return isValid;
    }
}
