package com.sms.SubscriptionService.model;

import com.sms.SubscriptionService.enums.Status;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceModel {

    private int id;

    @NotNull(message = "Service name cannot be null.")
    @Size(min = 2, max = 100, message = "Service name must be between 2 and 100 characters.")
    @Pattern(regexp = "^[A-Za-z0-9\\s]+$", message = "Service name must contain only letters, numbers, and spaces.")
    private String serviceName;

    @NotNull(message = "Description cannot be null.")
    @Size(min = 10, max = 500, message = "Description must be between 10 and 500 characters.")
    @Pattern(regexp = "^[A-Za-z0-9\\s,.!?'\"-]+$", message = "Description can contain letters, numbers, spaces, and common punctuation.")
    private String description;

    @NotNull(message = "Duration days cannot be null.")
    @Pattern(regexp = "^[1-9]\\d*$", message = "Duration days must be a positive integer.")
    private String durationDays;

    @NotNull(message = "Status cannot be null.")
    @Pattern(regexp = "^(ACTIVE|active|INACTIVE|inactive)$", message = "Status must be either 'ACTIVE' or 'INACTIVE'.")
    private Status dbstatus;

    @NotNull(message = "Created date cannot be null.")
    private LocalDate createdDate;

    @NotNull(message = "Updated date cannot be null.")
    private LocalDate updatedDate;

    private String createdBy;

    private String updatedBy;

}
