package com.trokr.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Endpoint de "Hello World" só para validar rapidamente, no Postman/Insomnia,
// que a aplicação está de pé e respondendo — sem precisar de banco de dados.
@RestController
public class HelloController {

    @GetMapping("/hello")
    public String helloWorld() {
        return "Hello, Trokr!";
    }
}
