package com.wallet.transfer.util;


import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JWTUtil {

    private final SecretKey key;
    private final long expirationMs;

    public JWTUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMs = expirationMs;
    }

    public String generateToken(String username) {
       return   Jwts.builder()
                 .setSubject(username)
                 .setIssuedAt(new Date())
                 .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                 .signWith(key, SignatureAlgorithm.HS256)
                 .compact();
    }

    public String extractUsername(String token) {
      Claims body= extractClaims(token);
      return body.getSubject();

    }

    private Claims extractClaims(String token) {
           return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }


    public boolean validateToken(String username, UserDetails userDetails, String token) {
    // TODO to check id username name is same as username in userdetails
        //TODO to check if the token is valid

       return  username.equals(userDetails.getUsername()) && isTokenExpired(token);

    }

    private boolean isTokenExpired(String token) {
        Claims claims=  extractClaims(token);

        return claims.getExpiration().before(new Date());
    }
}
