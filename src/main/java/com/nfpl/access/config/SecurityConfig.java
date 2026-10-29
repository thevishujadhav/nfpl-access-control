package com.nfpl.access.config;

import com.nfpl.access.repo.StaffUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(StaffUserRepository users) {
        return username -> users.findByUsername(username)
            .filter(u -> u.isActive())
            .map(u -> User.withUsername(u.getUsername())
                .password(u.getPasswordHash())
                .roles(u.getAccountType().equals("ADMIN") ? "ADMIN" : "USER")
                .build())
            .orElseThrow(() -> new UsernameNotFoundException("User not found or inactive"));
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            // CSRF is disabled in this assignment prototype to keep REST and form examples simple.
            // Enable CSRF and include tokens in every form before production deployment.
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/images/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/actuator/health").permitAll()
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error")
                .permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
            .httpBasic(Customizer.withDefaults())
            .build();
    }
}
