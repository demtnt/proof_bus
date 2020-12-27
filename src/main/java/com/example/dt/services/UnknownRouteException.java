package com.example.dt.services;

public class UnknownRouteException extends RuntimeException {

    public UnknownRouteException(String routeName) {
        super("Unknown route: " + routeName);
    }
}