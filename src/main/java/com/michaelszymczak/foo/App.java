package com.michaelszymczak.foo;


public class App {
    public static void main(String[] args) {
        System.out.println(new App().getGreetings());
    }

    public String getGreetings() {
        return "Welcome!";
    }
}
