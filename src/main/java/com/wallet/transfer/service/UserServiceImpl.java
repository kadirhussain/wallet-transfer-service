package com.wallet.transfer.service;


import com.wallet.transfer.api.dto.request.CreateUser;
import com.wallet.transfer.api.dto.request.UpdateUser;
import com.wallet.transfer.api.dto.response.UserResponse;
import com.wallet.transfer.domain.entity.User;
import com.wallet.transfer.domain.enums.Role;
import com.wallet.transfer.domain.exception.DuplicateUserException;
import com.wallet.transfer.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl (UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository=userRepository;
        this.passwordEncoder=passwordEncoder;
    }


    @Override
    public UserResponse findUserById(UUID id) {

        User user= userRepository.findById(id).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return toResponse(user);

    }

    @Override
    public UserResponse findUserByEmail(String email) {

       User user= userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found"));
       return toResponse(user);
    }

    @Override
    public UserResponse createUser(CreateUser createUser) {

        if (userRepository.findByEmail(createUser.email()).isPresent()) {
            throw new DuplicateUserException("Email already registered: " + createUser.email());
        }

        if (userRepository.findByMobile(createUser.mobile()).isPresent()) {
            throw new DuplicateUserException("Mobile already registered: " + createUser.mobile());
        }

        User user = new User();
        user.setEmail(createUser.email());
        user.setName(createUser.name());
        user.setMobile(createUser.mobile());
        user.setPasswordHash(passwordEncoder.encode(createUser.password()));
        user.setRoles(mapRoles(createUser.roles()));

        User savedUser = userRepository.save(user);
        return toResponse(savedUser);
    }

    @Override
    public UserResponse updateUser(UUID id ,UpdateUser updateUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + id));

        user.setName(updateUser.name());
        user.setMobile(updateUser.mobile());

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    @Override
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new UsernameNotFoundException("User not found: " + id);
        }
        userRepository.deleteById(id);
    }

    private Set<Role> mapRoles(java.util.List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return Set.of(Role.ROLE_USER); // sensible default
        }
        return roleNames.stream()
                .map(String::toUpperCase)
                .map(Role::valueOf)
                .collect(Collectors.toSet());
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getMobile());
    }
}
