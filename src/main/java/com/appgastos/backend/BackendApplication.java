package com.appgastos.backend;

import com.appgastos.backend.config.VariablesRequeridas;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		// Antes de levantar el contexto: si falta una variable, el error dice cual
		// en vez de morir mas adelante con un mensaje que no la nombra. Va aca y no
		// en un bean para que los tests, que traen su propia configuracion, no lo
		// necesiten.
		VariablesRequeridas.verificar(System::getenv);

		SpringApplication.run(BackendApplication.class, args);
		System.out.println("Iniciando sistema...");
	}

}
