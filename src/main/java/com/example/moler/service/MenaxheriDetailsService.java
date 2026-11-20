
package com.example.moler.service;

import com.example.moler.model.menaxheri;
import com.example.moler.repository.MenaxheriRepository;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service("menaxheriDetailsService")
public class MenaxheriDetailsService implements UserDetailsService {

    private final MenaxheriRepository menaxheriRepository;

    public MenaxheriDetailsService(
            MenaxheriRepository menaxheriRepository) {
        this.menaxheriRepository = menaxheriRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        menaxheri menaxheri = menaxheriRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Menaxheri not found"
                        )
                );

        return User.builder()
                .username(menaxheri.getUsername())
                .password(menaxheri.getPassword())
                .roles("MENAXHER")
                .build();
    }
}
