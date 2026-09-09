package com.wallet.transfer.service;


import com.wallet.transfer.api.dto.request.CreateUser;
import com.wallet.transfer.api.dto.request.UpdateUser;
import com.wallet.transfer.api.dto.response.UserResponse;

import java.util.UUID;

public interface UserService {

    UserResponse findUserById(UUID is);

    UserResponse findUserByEmail(String email);

    UserResponse createUser(CreateUser createUser);

    UserResponse updateUser(UUID id, UpdateUser updateUser);

    void deleteUser(UUID id);




}
