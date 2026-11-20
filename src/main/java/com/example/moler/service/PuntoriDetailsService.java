
package com.example.moler.service;

import com.example.moler.model.puntori;
import com.example.moler.repository.PuntoriRepository;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service("puntoriDetailsService")
public class PuntoriDetailsService implements UserDetailsService {

    private final PuntoriRepository puntoriRepository;

    public PuntoriDetailsService(
            PuntoriRepository puntoriRepository) {
        this.puntoriRepository = puntoriRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        puntori puntori = puntoriRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Puntori not found"
                        )
                );

        return User.builder()
                .username(puntori.getUsername())
                .password(puntori.getPassword())
                .roles("PUNTOR")
                .build();
    }
}
