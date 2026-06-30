package com.perussi.config;

import com.perussi.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers("/auth/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/produtos/**").permitAll()

                        .requestMatchers(HttpMethod.POST, "/carrinho/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/carrinho/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/carrinho/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/carrinho/**").authenticated()

                        .requestMatchers(HttpMethod.POST, "/pedidos/finalizar").authenticated()
                        .requestMatchers(HttpMethod.GET, "/pedidos/meus").authenticated()

                        .requestMatchers(HttpMethod.POST, "/produtos/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/produtos/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/produtos/**").hasAuthority("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/variacoes/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/variacoes/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/variacoes/**").hasAuthority("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/favoritos/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/favoritos/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/favoritos/**").authenticated()

                        .requestMatchers(HttpMethod.GET, "/usuarios/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/usuarios/me").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/usuarios/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/usuarios/me/endereco").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/usuarios/me/endereco").authenticated()
                        .requestMatchers(HttpMethod.POST, "/upload/imagem").hasAuthority("ADMIN")

                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/api/uploads/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/pedidos/*/meu").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/pedidos/*/pagamento").authenticated()

                        .requestMatchers(HttpMethod.GET, "/pedidos/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/pedidos/*/status").hasAuthority("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/pagamentos/criar").authenticated()
                        
                        .requestMatchers("/webhooks/**").permitAll()
                        
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

}