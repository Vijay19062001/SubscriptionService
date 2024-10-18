package com.sms.SubscriptionService.model;


import com.sms.SubscriptionService.enums.Status;
import com.sms.SubscriptionService.enums.SubscriptionStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionModel {


    private String id;
    private String userId;
    private String serviceId;
    private String startDate;
    private String endDate;
    @NotNull(message = "Status cannot be null.")
    private SubscriptionStatus status;
    @NotNull(message = "dbStatus cannot be null.")
    private Status dbstatus;
    private String createdDate;
    private String updatedDate;
    private String createdBy;
    private String updatedBy;


}
