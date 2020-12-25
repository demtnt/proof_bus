package com.example.dt.api;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class PingController implements PingApi {

    @Override
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong " + System.currentTimeMillis());
    }
}
