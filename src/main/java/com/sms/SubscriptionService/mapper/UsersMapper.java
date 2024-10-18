package com.sms.SubscriptionService.mapper;

import com.sms.SubscriptionService.entity.Users;
import com.sms.SubscriptionService.exception.custom.InvalidDateFormatException;
import com.sms.SubscriptionService.model.UsersModel;
import org.springframework.stereotype.Component;

@Component
public class UsersMapper {

    public Users toEntity(UsersModel usersModel) throws InvalidDateFormatException {

        Users users = new Users();
        users.setUserName(usersModel.getUserName());
        users.setEmail(usersModel.getEmail());
        users.setPassword(usersModel.getPassword());
        users.setAccountNumber(usersModel.getAccountNumber());
        users.setDbstatus(usersModel.getDbstatus());

        return users;
    }

    public UsersModel toModel(Users users) throws InvalidDateFormatException {

        UsersModel usersModel = new UsersModel();
        usersModel.setId(users.getId());
        usersModel.setUserName(users.getUserName());
        usersModel.setEmail(users.getEmail());
        usersModel.setPassword(users.getPassword());
        usersModel.setAccountNumber(users.getAccountNumber());
        usersModel.setDbstatus(users.getDbstatus());

        return usersModel;
    }
}
