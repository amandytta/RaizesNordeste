package br.com.raizesnordeste.raizesnordeste.integration;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


// valida os endpoints de consentimento com o banco MySQL de testes
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConsentimentoIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder encoder;

    // mantém somente os identificadores criados por esta classe
    private final List<Long> usuarios = new ArrayList<>();
    private static final String BASE = "/api/consentimentos";
    private static final String SENHA = "SenhaConsentimento@2026!";

    // protege o ambiente de desenvolvimento contra alterações acidentais
    @BeforeEach
    void verificarBanco() {
        assertEquals("raizes_nordeste_test", jdbc.queryForObject("SELECT DATABASE()", String.class));
        assertEquals("raizes_test@localhost", jdbc.queryForObject("SELECT CURRENT_USER()", String.class));
    }

    // cria um cliente isolado para cada cenário
    private Long criarCliente() {
        verificarBanco();
        String email = "consentimento." + UUID.randomUUID() + "@raizes.local";
        jdbc.update("INSERT INTO usuario(nome,email,senha_hash,ativo) VALUES (?,?,?,TRUE)",
                "Cliente Consentimento Teste", email, encoder.encode(SENHA));
        Long id = jdbc.queryForObject("SELECT id FROM usuario WHERE email=?", Long.class, email);
        usuarios.add(id);
        jdbc.update("INSERT INTO usuario_perfil(usuario_id,perfil_id) SELECT ?,id FROM perfil WHERE nome='CLIENTE'", id);
        return id;
    }

    // autentica o cliente pela API real
    private String token(Long id) throws Exception {
        String email = jdbc.queryForObject("SELECT email FROM usuario WHERE id=?", String.class, id);
        String resposta = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.token");
    }

    // envia uma decisão autenticada
    private ResultActions registrar(String token, String finalidade, String versao, boolean concedido) throws Exception {
        return mockMvc.perform(post(BASE).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"finalidade":"%s","versaoTermo":"%s","concedido":%s,"origem":"WEB"}
                """.formatted(finalidade, versao, concedido)));
    }

    // consulta a situação atual sem depender da ordem das finalidades
    private ResultActions atuais(String token) throws Exception {
        return mockMvc.perform(get(BASE + "/meus/atuais").header("Authorization", "Bearer " + token));
    }

    // consulta o histórico pessoal
    private ResultActions historico(String token) throws Exception {
        return mockMvc.perform(get(BASE + "/meus").header("Authorization", "Bearer " + token));
    }

    // conta os registros persistidos para o usuário de teste
    private int contar(Long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM consentimento WHERE usuario_id=?", Integer.class, id);
    }

    // simula um consentimento concedido antes da atualização do termo
    private void inserirAntigo(Long id) {
        verificarBanco();
        jdbc.update("INSERT INTO consentimento(usuario_id,finalidade,versao_termo,concedido,origem) VALUES (?,'MARKETING','0.9',TRUE,'APP')", id);
    }

    // valida a concessão e a persistência
    @Test void deveConceder() throws Exception {
        Long id = criarCliente();
        registrar(token(id), "MARKETING", "1.0", true).andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").value(id.intValue()))
                .andExpect(jsonPath("$.concedido").value(true))
                .andExpect(jsonPath("$.versaoTermo").value("1.0"))
                .andExpect(jsonPath("$.origem").value("WEB"))
                .andExpect(jsonPath("$.dataRegistro").exists());
        assertEquals(1, contar(id));
    }

    // verifica que as decisões anteriores não são sobrescritas
    @Test void devePreservarHistorico() throws Exception {
        Long id = criarCliente(); String t = token(id);
        registrar(t,"MARKETING","1.0",true).andExpect(status().isCreated());
        registrar(t,"MARKETING","1.0",false).andExpect(status().isCreated());
        atuais(t).andExpect(status().isOk()).andExpect(jsonPath("$[?(@.finalidade == 'MARKETING')].concedido").value(org.hamcrest.Matchers.contains(false)));
        registrar(t,"MARKETING","1.0",true).andExpect(status().isCreated());
        historico(t).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].concedido").value(true)).andExpect(jsonPath("$[1].concedido").value(false));
        assertEquals(3, contar(id));
    }

    // ausência de decisão não autoriza tratamento opcional
    @Test void deveNegarPorPadrao() throws Exception {
        atuais(token(criarCliente())).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].concedido").value(org.hamcrest.Matchers.contains(false,false)))
                .andExpect(jsonPath("$[*].possuiRegistro").value(org.hamcrest.Matchers.contains(false,false)));
    }

    // verifica o filtro por finalidade
    @Test void deveFiltrarHistorico() throws Exception {
        String t = token(criarCliente());
        registrar(t,"MARKETING","1.0",true).andExpect(status().isCreated());
        registrar(t,"FIDELIDADE","1.0",false).andExpect(status().isCreated());
        mockMvc.perform(get(BASE+"/meus").param("finalidade","MARKETING").header("Authorization","Bearer "+t))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].finalidade").value("MARKETING"));
    }

    // verifica que cada finalidade possui decisão independente
    @Test void deveSepararFinalidades() throws Exception {
        String t = token(criarCliente());
        registrar(t,"MARKETING","1.0",true).andExpect(status().isCreated());
        registrar(t,"FIDELIDADE","1.0",false).andExpect(status().isCreated());
        atuais(t).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.finalidade == 'MARKETING')].concedido").value(org.hamcrest.Matchers.contains(true)))
                .andExpect(jsonPath("$[?(@.finalidade == 'FIDELIDADE')].concedido").value(org.hamcrest.Matchers.contains(false)));
    }

    // impede concessões para versões antigas
    @Test void deveRejeitarVersaoAntiga() throws Exception {
        Long id = criarCliente();
        registrar(token(id),"MARKETING","0.9",true).andExpect(status().isConflict());
        assertEquals(0, contar(id));
    }

    // permite revogar mesmo após a atualização do termo
    @Test void deveRevogarVersaoAntiga() throws Exception {
        Long id = criarCliente(); inserirAntigo(id);
        registrar(token(id),"MARKETING","0.9",false).andExpect(status().isCreated())
                .andExpect(jsonPath("$.versaoTermo").value("0.9")).andExpect(jsonPath("$.concedido").value(false));
        assertEquals(2, contar(id));
    }

    // uma concessão antiga não autoriza o tratamento na versão vigente
    @Test void deveInvalidarVersaoAntiga() throws Exception {
        Long id = criarCliente(); inserirAntigo(id);
        atuais(token(id)).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.finalidade == 'MARKETING')].concedido").value(org.hamcrest.Matchers.contains(false)))
                .andExpect(jsonPath("$[?(@.finalidade == 'MARKETING')].versaoTermoRegistrada").value(org.hamcrest.Matchers.contains("0.9")));
    }

    // impede a repetição da mesma decisão
    @Test void deveRejeitarDuplicidade() throws Exception {
        Long id = criarCliente(); String t = token(id);
        registrar(t,"MARKETING","1.0",true).andExpect(status().isCreated());
        registrar(t,"MARKETING","1.0",true).andExpect(status().isConflict());
        assertEquals(1, contar(id));
    }

    // exige uma decisão explícita e os demais campos obrigatórios
    @Test void deveValidarCampos() throws Exception {
        Long id = criarCliente(); String t = token(id);
        for (String corpo : List.of("{}", "{\"finalidade\":\"MARKETING\",\"versaoTermo\":\"1.0\",\"origem\":\"WEB\"}")) {
            mockMvc.perform(post(BASE).header("Authorization","Bearer "+t).contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(0, contar(id));
    }

    // exige autenticação em todos os endpoints
    @Test void deveExigirAutenticacao() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE+"/meus")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE+"/meus/atuais")).andExpect(status().isUnauthorized());
    }

    // garante que o histórico não revela decisões de outros clientes
    @Test void deveIsolarUsuarios() throws Exception {
        Long a = criarCliente(), b = criarCliente(); String ta = token(a), tb = token(b);
        registrar(ta,"MARKETING","1.0",true).andExpect(status().isCreated());
        historico(tb).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        assertEquals(0, contar(b));
    }

    // bloqueia alteração e exclusão direta de registros
    @Test void deveBloquearAlteracaoEExclusao() throws Exception {
        Long id = criarCliente(); String t = token(id);
        registrar(t,"MARKETING","1.0",true).andExpect(status().isCreated());
        Long registro = jdbc.queryForObject("SELECT id FROM consentimento WHERE usuario_id=?",Long.class,id);
        mockMvc.perform(put(BASE+"/"+registro).header("Authorization","Bearer "+t)).andExpect(status().isForbidden());
        mockMvc.perform(delete(BASE+"/"+registro).header("Authorization","Bearer "+t)).andExpect(status().isForbidden());
        assertEquals(1, contar(id));
    }

    // verifica duas concessões concorrentes para o mesmo usuário
    @Test void deveSerializarConcessoes() throws Exception {
        Long id = criarCliente(); String t = token(id);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch inicio = new CountDownLatch(1);
        Callable<Integer> tarefa = () -> {
            if (!inicio.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Tempo de espera excedido");
            return registrar(t,"MARKETING","1.0",true).andReturn().getResponse().getStatus();
        };
        try {
            Future<Integer> a = executor.submit(tarefa), b = executor.submit(tarefa);
            inicio.countDown();
            List<Integer> resultados = new ArrayList<>(List.of(a.get(30,TimeUnit.SECONDS), b.get(30,TimeUnit.SECONDS)));
            Collections.sort(resultados);
            assertEquals(List.of(201,409), resultados);
            assertEquals(1, contar(id));
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(30,TimeUnit.SECONDS));
        }
    }

    // remove exclusivamente os dados criados por esta classe
    @AfterEach void limparDados() {
        verificarBanco();
        for (Long id : usuarios) {
            // remove a auditoria do usuário antes de excluí-lo
            jdbc.update("DELETE FROM auditoria WHERE usuario_id=?", id);
            jdbc.update("DELETE FROM consentimento WHERE usuario_id=?", id);
            jdbc.update("DELETE FROM usuario_perfil WHERE usuario_id=?", id);
            jdbc.update("DELETE FROM usuario WHERE id=?", id);
        }
        usuarios.clear();
    }
}