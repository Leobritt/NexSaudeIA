package br.ucsal.nexsaude.common;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ApiExceptionHandlerTest.TestController.class)
@Import({ApiExceptionHandler.class, ApiExceptionHandlerTest.TestController.class})
@WithMockUser
class ApiExceptionHandlerTest {

    @Autowired
    MockMvc mvc;

    record Corpo(@NotBlank String nome) {}

    @RestController
    static class TestController {
        @GetMapping("/teste/regra")
        void regra() { throw new RegraNegocioException("x"); }

        @GetMapping("/teste/nao-encontrado")
        void naoEncontrado() { throw new NaoEncontradoException("Paciente", 7); }

        @GetMapping("/teste/nao-implementado")
        void naoImplementado() { throw new UnsupportedOperationException(); }

        @PostMapping("/teste/validacao")
        void validacao(@Valid @RequestBody Corpo corpo) {}
    }

    @Test
    void regraNegocioViraStatus422ComDetail() throws Exception {
        mvc.perform(get("/teste/regra"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("x"));
    }

    @Test
    void naoEncontradoVira404() throws Exception {
        mvc.perform(get("/teste/nao-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Paciente 7 não encontrado"));
    }

    @Test
    void naoImplementadoVira501() throws Exception {
        mvc.perform(get("/teste/nao-implementado"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    void validacaoFalhaVira400ComCampos() throws Exception {
        mvc.perform(post("/teste/validacao").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nome").exists());
    }
}
