package com.wallet.transfer.api.controller;

import com.wallet.transfer.api.dto.request.AuthRequest;
import com.wallet.transfer.util.JWTUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JWTUtil  jwtUtil;

    @PostMapping("/authenticate")
    public String generateToken(@RequestBody AuthRequest authRequest) {

        try{
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
            );

            System.out.println(" authenticate success");
            System.out.println(jwtUtil.generateToken(authRequest.getUsername()));
            return jwtUtil.generateToken(authRequest.getUsername());

        }catch(Exception e){
            return e.getMessage();
        }
    }

}
