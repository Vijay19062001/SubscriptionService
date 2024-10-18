package com.sms.SubscriptionService.utils;

import com.sms.SubscriptionService.exception.custom.InvalidDateFormatException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateUtils {

    private static final String DATE_FORMAT = "yyyyMMdd"; // Change format as needed
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);

    public static LocalDate convertToLocalDate(String dateString) {
        try {
            return LocalDate.parse(dateString, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new InvalidDateFormatException("Invalid date format. Please use yyyyMMdd format.");
        }
    }

    public static LocalDateTime convertToLocalDateTime(String dateString) {
        LocalDate date = convertToLocalDate(dateString);
        return date.atStartOfDay(); // Convert to LocalDateTime at the start of the day
    }

    public static String localDateToString(LocalDate date) {
        return date.format(DATE_FORMATTER); // Format LocalDate to string using DATE_FORMAT
    }

    public static LocalDate localDateTimeToLocalDate(LocalDateTime dateTime) {
        return dateTime.toLocalDate(); // Convert LocalDateTime back to LocalDate
    }
}
