package com.pharmacore.pharmacore.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

//Seguridad básica: login + roles (Administrador / Farmaceutico / Cajero).
// el CSRF de Spring Security está desactivado (.csrf(disable)). Si se activa,
// TODOS los formularios existentes (compras, ventas, lotes, inventario, fórmulas,
// proveedores, etc.) dejarían de funcionar porque ninguno envía un token CSRF.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(PharmaCoreUserDetailsService userDetailsService,
                                                              PasswordEncoder passwordEncoder) {
        // Desde Spring Security 6.3, DaoAuthenticationProvider ya no tiene constructor vacío
        // ni setUserDetailsService(): el UserDetailsService se pasa directo en el constructor.
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Público: login y recursos estáticos
                        .requestMatchers("/login", "/error/**", "/errores/**", "/css/**", "/js/**", "/img/**", "/webjars/**").permitAll()
                        // Solo Administrador: gestión de usuarios, empleados y roles del sistema
                        .requestMatchers("/view/usuarios/**", "/view/empleados/**", "/view/roles/**")
                            .hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/usuarios/**", "/api/empleados/**", "/api/roles/**")
                            .hasRole("ADMINISTRADOR")
                        // Todo lo demás requiere haber iniciado sesión
                        .anyRequest().authenticated()
                )
                    .exceptionHandling(exception -> exception
                        .accessDeniedPage("/error/403")
                    )
                .formLogin((FormLoginConfigurer<HttpSecurity> form) -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/view/home", true)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );
        return http.build();
    }
}
