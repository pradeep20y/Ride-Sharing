package com.ridesharing.project.service.security;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import com.ridesharing.project.entity.User;
import com.ridesharing.project.repository.UserRepository;
@Service
public class CustomUserDetailsService implements UserDetailsService {

    public UserRepository userRepository;
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

@Override
public UserDetails loadUserByUsername(String phoneNumber) throws UsernameNotFoundException {

    Optional<User> user = userRepository.findByPhone(phoneNumber);

    if (user.isEmpty()) {
        throw new UsernameNotFoundException(
                "User not found with phone: " + phoneNumber);
    }

    System.out.println("UserDetailsService called sssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssss");
    System.out.println("Phone = " + phoneNumber);
    System.out.println("DB Hash = " + user.get().getPassword());
        return org.springframework.security.core.userdetails.User
                .builder()
                .username(user.get().getPhone())
                .password(user.get().getPassword())
                .roles(user.get().getUserType())
                .build();
}
}
