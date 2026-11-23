package com.trokr.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Endpoint de "Hello World" só para validar rapidamente, no Postman/Insomnia,
// que a aplicação está de pé e respondendo — sem precisar de banco de dados.
@Hidden // Aula 14: ferramenta de aula, fora do contrato público
@RestController
public class HelloController {

    @GetMapping("/hello")
    public String helloWorld() {
        return "Hello, Trokr!";
    }

    /**
     * Ferramenta de aula (Aula 11): força um erro inesperado para conferir o
     * catch-all do GlobalExceptionHandler — resposta 500 com codigo
     * ERRO_INTERNO, sem detalhe interno, e o mesmo id de "ocorrencia" em
     * GET /logs?tipo=ERRO_SISTEMA.
     */
    @GetMapping("/demo/erro-inesperado")
    public String erroInesperado() {
        String nada = null;
        return nada.trim(); // NullPointerException proposital
    }
}
