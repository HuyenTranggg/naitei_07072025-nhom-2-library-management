package com.group2.library_management.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.group2.library_management.entity.User;
import com.group2.library_management.entity.enums.RoleType;
import com.group2.library_management.repository.UserRepository;
import com.group2.library_management.security.CustomUserDetails;

@Service
public class JpaUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public JpaUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException, AccessDeniedException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("{login.error}" + email));
        if(user.getRole() == RoleType.CLIENT){
            throw new AccessDeniedException("{login.access.denied}");
        }
        
        return new CustomUserDetails(user);
    }

}
