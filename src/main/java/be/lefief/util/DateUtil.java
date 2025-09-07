package be.lefief.util;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class DateUtil {

    public static final DateTimeFormatter SHORT_TIMEFORMAT = DateTimeFormatter.ofPattern("hh:mm:ss");

    public static java.util.Date toDate(LocalDateTime date){
        if(date == null) return null;
        return java.util.Date.from(date.toInstant(ZoneOffset.UTC));
    }
    public static LocalDateTime toLocalDateTime(java.util.Date dateToConvert) {
        if(dateToConvert == null) return null;
        return dateToConvert.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    public static LocalDate toLocalDate(java.util.Date date){
        if(date == null) return null;
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    public static java.util.Date toDate(LocalDate date){
        if(date == null) return null;
        return java.util.Date.from(date.atStartOfDay().toInstant(ZoneOffset.UTC));
    }

    public static java.sql.Date toSqlDate(LocalDate date){
        if(date == null) return null;
        return java.sql.Date.valueOf(date);
    }

    public static LocalDate toLocalDate(java.sql.Date date){
        if(date == null) return null;
        return date.toLocalDate();
    }

    public static LocalDateTime toLocalDateTime(java.sql.Timestamp timestamp){
        if(timestamp == null) return null;
        return timestamp.toLocalDateTime();
    }

    public static java.sql.Timestamp toTimeStamp(LocalDateTime localDateTime){
        if(localDateTime == null) return null;
        return java.sql.Timestamp.valueOf(localDateTime);
    }
}

