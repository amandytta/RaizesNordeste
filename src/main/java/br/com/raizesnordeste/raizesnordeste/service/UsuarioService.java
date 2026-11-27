package br.com.raizesnordeste.raizesnordeste.service;

import br.com.raizesnordeste.raizesnordeste.repository.UsuarioRepository;
import br.com.raizesnordeste.raizesnordeste.repository.PerfilRepository;
import br.com.raizesnordeste.raizesnordeste.dto.UsuarioCadastroRequestDTO;
import br.com.raizesnordeste.raizesnordeste.dto.UsuarioResponseDTO;
import br.com.raizesnordeste.raizesnordeste.dto.FuncionarioCadastroRequestDTO;
import br.com.raizesnordeste.raizesnordeste.entity.Usuario;
import br.com.raizesnordeste.raizesnordeste.entity.Perfil;
import br.com.raizesnordeste.raizesnordeste.exception.DadosInvalidosException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;

import java.nio.charset.StandardCharsets;

import java.util.Locale;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;


// centraliza as regras de negócio relacionadas aos usuarios
@Service
public class UsuarioService {

    // responsavel pelas consultas e persistencia dos usuarios
    // serviço responsável pela auditoria transacional
    private final AuditoriaService auditoriaService;

    private final UsuarioRepository usuarioRepository;

    // responsavel pela consulta dos perfis de acesso
    private final PerfilRepository perfilRepository;

    // responsavel pela geracao segura do hash das senhas
    private final PasswordEncoder passwordEncoder;

    // recebe as dependencias por injeção de construtor
    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PerfilRepository perfilRepository,
            PasswordEncoder passwordEncoder,
            AuditoriaService auditoriaService
    ) {
        this.auditoriaService = auditoriaService;
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // realiza o cadastro publico de um novo cliente
    // a transação garante a consistência dos dados persistidos
    @Transactional
    public UsuarioResponseDTO cadastrarCliente(
            UsuarioCadastroRequestDTO dto
    ) {

        // remove os espaços das extremidades do nome
        String nome = dto.nome().strip();

        // padroniza o e-mail para evitar cadastros duplicados causados por diferenças entre maiusculas e minusculas
        String email = dto.email()
                .strip()
                .toLowerCase(Locale.ROOT);

        // verifica se o e-mail já está cadastrado
        if (usuarioRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "Já existe um usuário cadastrado com este e-mail."
            );
        }

        // valida o limite de 72 bytes suportado pelo BCrypt
        // caracteres especiais podem ocupar mais de um byte
        // retorna HTTP 400 quando a senha ultrapassa o limite
        if (dto.senha().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new DadosInvalidosException(
                    "A senha deve possuir no máximo 72 bytes."
            );
        }

        // recupera o perfil CLIENTE cadastrado pelas migrations
        // a ausência desse perfil representa uma falha interna
        Perfil perfilCliente = perfilRepository
                .findByNome("CLIENTE")
                .orElseThrow(() -> new IllegalStateException(
                        "Perfil CLIENTE não encontrado."
                ));

        // cria uma nova entidade de usuario
        Usuario usuario = new Usuario();

        // preenche os dados normalizados
        usuario.setNome(nome);
        usuario.setEmail(email);

        // gera o hash da senha antes de armazenar no banco
        usuario.setSenhaHash(
                passwordEncoder.encode(dto.senha())
        );

        // define a conta como ativa no momento do cadastro
        usuario.setAtivo(true);

        // atribui exclusivamente o perfil CLIENTE
        // impede a escolha de privilégios pelo cadastro publico
        usuario.setPerfis(
                new HashSet<>(Set.of(perfilCliente))
        );

        // persiste o usuario e seu relacionamento com o perfil
        Usuario usuarioSalvo = usuarioRepository.save(usuario);


        // registra o evento de auditoria na mesma transação
        auditoriaService.registrar(
                usuarioSalvo, "CADASTRAR_CLIENTE", "USUARIO", usuarioSalvo.getId(),
                Map.of("perfil", "CLIENTE")
        );

        // extrai somente os nomes dos perfis para a resposta
        Set<String> perfis = usuarioSalvo.getPerfis()
                .stream()
                .map(Perfil::getNome)
                .collect(Collectors.toSet());

        // retorna os dados permitidos, sem expor a senha ou seu hash
        return new UsuarioResponseDTO(
                usuarioSalvo.getId(),
                usuarioSalvo.getNome(),
                usuarioSalvo.getEmail(),
                usuarioSalvo.getAtivo(),
                perfis
        );
    }

    // converte uma entidade Usuario em um DTO de resposta
    private UsuarioResponseDTO converterParaResponseDTO(Usuario usuario) {

        // extrai os nomes dos perfis associados ao usuário
        Set<String> perfis = usuario.getPerfis()
                .stream()
                .map(Perfil::getNome)
                .collect(Collectors.toSet());

        // retorna exclusivamente os dados permitidos na resposta da API
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getAtivo(),
                perfis
        );
    }

    // lista todos os usuários cadastrados no sistema
    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarTodos() {

        // consulta os usuários e converte cada entidade em um DTO
        return usuarioRepository.findAll()
                .stream()
                .map(this::converterParaResponseDTO)
                .toList();
    }

    // busca um usuário pelo seu identificador
    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorId(Long id) {

        // consulta o usuário ou lança uma exceção caso não exista
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Usuário não encontrado."));

        // retorna os dados do usuário sem expor sua senha
        return converterParaResponseDTO(usuario);
    }

    // realiza o cadastro administrativo de atendentes e gerentes
    @Transactional
    public UsuarioResponseDTO cadastrarFuncionario(
            FuncionarioCadastroRequestDTO dto) {

        // normaliza o nome e o endereço de e-mail
        String nome = dto.nome().strip();
        String email = dto.email().strip().toLowerCase(Locale.ROOT);

        // garante que somente perfis de funcionários sejam permitidos
        if (!Set.of("ATENDENTE", "GERENTE").contains(dto.perfil())) {
            throw new DadosInvalidosException(
                    "O perfil deve ser ATENDENTE ou GERENTE."
            );
        }

        // verifica se o e-mail já está cadastrado
        if (usuarioRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "Já existe um usuário cadastrado com este e-mail."
            );
        }

        // valida o limite de bytes suportado pelo BCrypt
        if (dto.senha().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new DadosInvalidosException(
                    "A senha deve possuir no máximo 72 bytes."
            );
        }

        // busca o perfil de funcionário no banco de dados
        Perfil perfil = perfilRepository.findByNome(dto.perfil())
                .orElseThrow(() -> new IllegalStateException(
                        "Perfil de funcionário não encontrado."
                ));

        // cria a entidade que representa o novo funcionário
        Usuario usuario = new Usuario();

        // define os dados normalizados do funcionário
        usuario.setNome(nome);
        usuario.setEmail(email);

        // codifica a senha antes de armazená-la
        usuario.setSenhaHash(passwordEncoder.encode(dto.senha()));

        // define o funcionário como ativo
        usuario.setAtivo(true);

        // associa exclusivamente o perfil autorizado
        usuario.setPerfis(new HashSet<>(Set.of(perfil)));

        // persiste o funcionário no banco de dados
        Usuario usuarioSalvo = usuarioRepository.save(usuario);


        // registra o evento de auditoria na mesma transação
        auditoriaService.registrar(
                recuperarAdministradorAuditoria(), "CADASTRAR_FUNCIONARIO", "USUARIO", usuarioSalvo.getId(),
                Map.of("perfil", dto.perfil())
        );

        // retorna os dados do funcionário cadastrado
        return converterParaResponseDTO(usuarioSalvo);
    }

    // identifica o administrador autenticado sem utilizar dados do corpo da requisição
    private Usuario recuperarAdministradorAuditoria() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !autenticacao.isAuthenticated()
                || autenticacao.getAuthorities().stream().noneMatch(
                        autoridade -> "ROLE_ADMIN".equals(autoridade.getAuthority()))) {
            throw new AccessDeniedException("Acesso administrativo obrigatório.");
        }
        return usuarioRepository.findByEmail(autenticacao.getName())
                .filter(usuario -> Boolean.TRUE.equals(usuario.getAtivo()))
                .orElseThrow(() -> new AccessDeniedException("Administrador não encontrado ou inativo."));
    }
}
