package com.sms.SubscriptionService.model;


import com.sms.SubscriptionService.enums.Status;
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
    private Status dbstatus;
    private String createdDate;
    private String updatedDate;
    private String createdBy;
    private String updatedBy;
    private String amount;
    private String transactionId;

}
