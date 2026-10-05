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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
// Acá está toda la configuración de seguridad del sistema: cómo se cifran
// las contraseñas, cómo se valida el login, y qué rutas necesitan qué rol.
// La puse toda en un solo archivo para no tener reglas de acceso regadas
// por distintos controladores.
public class SecurityConfig {

    // BCrypt es el algoritmo que uso para las contraseñas. La diferencia con
    // algo como MD5 o SHA-256 es que BCrypt es lento a propósito (para que a
    // alguien le tome mucho más tiempo intentar adivinar contraseñas por
    // fuerza bruta) y cada vez que cifra la misma contraseña le da un
    // resultado distinto, porque le agrega una "sal" aleatoria.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(PharmaCoreUserDetailsService userDetailsService,
                                                              PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http

                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )
                .authorizeHttpRequests(auth -> auth
                        // Público: login y recursos estáticos
                        .requestMatchers("/login", "/css/**", "/js/**", "/img/**", "/webjars/**").permitAll()
                        // Estas rutas son las que manejan usuarios/empleados/roles del sistema,
                        // así que las dejo solo para el rol Administrador. El resto de módulos
                        // (ventas, inventario, devoluciones, etc.) cualquier empleado logueado
                        // los puede usar, porque son el trabajo del día a día de la farmacia.
                        .requestMatchers("/view/usuarios/**", "/view/empleados/**", "/view/roles/**")
                            .hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/usuarios/**", "/api/empleados/**", "/api/roles/**")
                            .hasRole("ADMINISTRADOR")
                        // El Vendedor trabaja en el mostrador: solo necesita facturar, ver el
                        // historial de sus ventas, gestionar clientes y consultar fórmulas médicas
                        // (las necesita para saber si puede vender un medicamento controlado).
                        // También le doy Devoluciones y Domicilios, porque en la práctica esas dos
                        // cosas normalmente las atiende quien está en el mostrador en el momento,
                        // no necesariamente el farmacéutico. Eso sí, dentro de DevolucionesView
                        // hay una regla aparte que evita que un Vendedor registre una devolución
                        // A PROVEEDOR (esa sí es decisión del farmacéutico/regente).
                        .requestMatchers("/view/home", "/view/ventas/**", "/view/dispensacion/**",
                                          "/view/clientes/**", "/view/formulas/**",
                                          "/view/devoluciones/**", "/view/domicilios/**")
                            .hasAnyRole("ADMINISTRADOR", "FARMACEUTICO", "VENDEDOR")
                        .requestMatchers("/api/ventas/**", "/api/clientes/**", "/api/formulas/**",
                                          "/api/devoluciones/**", "/api/domicilios/**")
                            .hasAnyRole("ADMINISTRADOR", "FARMACEUTICO", "VENDEDOR")
                        // Toodo lo demás (catálogo/productos, lotes, inventario, compras,
                        // proveedores, devoluciones, domicilios, reportes) es trabajo del
                        // Farmaceutico (Regente) o del Administrador; el Vendedor no entra ahí.
                        .anyRequest().hasAnyRole("ADMINISTRADOR", "FARMACEUTICO")
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
