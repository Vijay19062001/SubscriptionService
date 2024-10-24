package com.sms.SubscriptionService.mapper;

import com.sms.SubscriptionService.entity.ServiceEntity;
import com.sms.SubscriptionService.exception.custom.InvalidDateFormatException;
import com.sms.SubscriptionService.model.ServiceModel;
import org.springframework.stereotype.Component;

@Component
public class ServiceMapper {

    public ServiceEntity toEntity(ServiceModel serviceModel) throws InvalidDateFormatException {

        ServiceEntity service = new ServiceEntity();
        service.setServiceName(serviceModel.getServiceName());
        service.setDescription(serviceModel.getDescription());
        service.setDurationDays(serviceModel.getDurationDays());
        service.setDbstatus(serviceModel.getDbstatus());

        return service;
    }

    public ServiceModel toModel(ServiceEntity service)throws InvalidDateFormatException {

        ServiceModel serviceModel = new ServiceModel();
        serviceModel.setId(service.getId());
        serviceModel.setServiceName(service.getServiceName());
        serviceModel.setDescription(service.getDescription());
        serviceModel.setDurationDays(service.getDurationDays());
        serviceModel.setDbstatus(service.getDbstatus());

        return serviceModel;
    }
}