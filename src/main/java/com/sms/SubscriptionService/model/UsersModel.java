package com.sms.SubscriptionService.model;

import com.sms.SubscriptionService.enums.Status;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsersModel {

    private int id;

    @NotNull(message = "Username cannot be null.")
    @Pattern(regexp = "^[A-Za-z\\s]+$", message = "Username must contain only letters and spaces.")
    @Size(min = 2, max = 50, message = "Username must be between 2 and 50 characters.")
    private String userName;

    @NotNull(message = "Email cannot be null.")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z]{2,}$",
            message = "Email should be in a valid format.")
    private String email;

    @NotNull(message = "Password cannot be null.")
    @Size(min = 8, message = "Password must be at least 8 characters long.")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*])[A-Za-z\\d!@#$%^&*]{8,}$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character.")
    private String password;

    @NotNull(message = "Account number cannot be null.")
    @Pattern(regexp = "^[A-Z]{3}\\d{9}$", message = "Account number must be in the format 'XXX123456789' (3 letters followed by 9 digits).")
    private String accountNumber;

    @NotNull(message = "Status cannot be null.")
    @Pattern(regexp = "^(ACTIVE|active|INACTIVE|inactive)$", message = "Status must be either 'ACTIVE' or 'INACTIVE'.")
    private Status dbstatus;

    @NotNull(message = "Created date cannot be null.")
    private String createdDate;

    @NotNull(message = "Updated date cannot be null.")
    private String updatedDate;

    private String createdBy;

    private String updatedBy;

}
