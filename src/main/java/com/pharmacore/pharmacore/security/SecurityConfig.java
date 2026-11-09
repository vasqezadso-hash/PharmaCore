package com.pharmacore.pharmacore.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
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
    public SecurityFilterChain filterChain(HttpSecurity http, PharmaCoreUserDetailsService userDetailsService) throws Exception {
        http

                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )
                .authorizeHttpRequests(auth -> auth
                        // Público: login y recursos estáticos
                        .requestMatchers("/login", "/css/**", "/js/**", "/img/**", "/webjars/**").permitAll()

                        // Módulo de Usuarios, Personal y Roles
                        .requestMatchers("/view/usuarios/**", "/view/empleados/**", "/view/roles/**",
                                          "/api/usuarios/**", "/api/empleados/**", "/api/roles/**")
                            .hasAnyAuthority("ROLE_ADMINISTRADOR", "MOD_USUARIOS")

                        // Mostrador y Ventas
                        .requestMatchers("/view/ventas/nueva/**", "/view/ventas/nueva/guardar")
                            .hasAnyAuthority("ROLE_ADMINISTRADOR", "MOD_POS")
                        .requestMatchers("/view/dispensacion/**", "/view/ventas/detalle/**")
                            .hasAnyAuthority("ROLE_ADMINISTRADOR", "MOD_VENTAS_HISTORIAL", "MOD_POS")
                        .requestMatchers("/api/ventas/**")
                            .hasAnyAuthority("ROLE_ADMINISTRADOR", "MOD_POS", "MOD_VENTAS_HISTORIAL")

                        // Clientes
                        .requestMatchers("/view/clientes/**", "/api/clientes/**")
                            .hasAnyAuthority("ROLE_ADMINISTRADOR", "MOD_CLIENTES")

                        // Devoluciones y Domicilios
                        .requestMatchers("/view/devoluciones/**", "/api/devoluciones/**")
                            .hasAnyAuthority("ROLE_ADMINISTRADOR", "MOD_DEVOLUCIONES")
                        .requestMatchers("/view/domicilios/**", "/api/domicilios/**")
                            .hasAnyAuthority("ROLE_ADMINISTRADOR", "MOD_DOMICILIOS")

                        // Catálogo y Stock (Productos, Categorías, Lotes, Kardex)
                        .requestMatchers("/view/medicamentos/**", "/api/medicamentos/**")
                            .hasAnyAuthority("ROLE_ADMINISTRADOR", "MOD_PRODUCTOS")
                        .requestMatchers("/view/categorias/**", "/api/categorias/**")
                            .hasAnyAuthority("ROLE_ADMINISTRADOR", "MOD_CATEGORIAS")
                        .requestMatchers("/view/lotes/**", "/view/vencimientos/**", "/api/lotes/**")
                            .hasAnyAuthority("ROLE_ADMINISTRADOR", "MOD_LOTES")
                        .requestMatchers("/view/inventario/**", "/api/inventario/**")
                            .hasAnyAuthority("ROLE_ADMINISTRADOR", "MOD_INVENTARIO")

                        // Compras y Proveedores
                        .requestMatchers("/view/ordenes-compra/**", "/api/compras/**")
                            .hasAnyAuthority("ROLE_ADMINISTRADOR", "MOD_COMPRAS")
                        .requestMatchers("/view/proveedores/**", "/api/proveedores/**")
                            .hasAnyAuthority("ROLE_ADMINISTRADOR", "MOD_PROVEEDORES")

                        // Finanzas y Reportes
                        .requestMatchers("/view/finanzas/**", "/api/finanzas/**")
                            .hasAnyAuthority("ROLE_ADMINISTRADOR", "MOD_FINANZAS")
                        .requestMatchers("/view/reportes/**", "/api/reportes/**")
                            .hasAnyAuthority("ROLE_ADMINISTRADOR", "MOD_REPORTES")

                        // Inicio y rutas base
                        .requestMatchers("/", "/view/home").authenticated()

                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(sessionFixation -> sessionFixation.changeSessionId())
                        .maximumSessions(-1)
                )
                .rememberMe(remember -> remember
                        .key("PharmaCoreSecretKey2026")
                        .tokenValiditySeconds(86400 * 30)
                        .userDetailsService(userDetailsService)
                        .rememberMeParameter("remember-me")
                        .alwaysRemember(true)
                )
                .formLogin((FormLoginConfigurer<HttpSecurity> form) -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/view/home", false)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll()
                );
        return http.build();
    }
}
