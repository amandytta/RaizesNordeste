package br.com.raizesnordeste.raizesnordeste.service;

import br.com.raizesnordeste.raizesnordeste.repository.UsuarioRepository;
import br.com.raizesnordeste.raizesnordeste.repository.PerfilRepository;
import br.com.raizesnordeste.raizesnordeste.dto.UsuarioCadastroRequestDTO;
import br.com.raizesnordeste.raizesnordeste.dto.UsuarioResponseDTO;
import br.com.raizesnordeste.raizesnordeste.entity.Usuario;
import br.com.raizesnordeste.raizesnordeste.entity.Perfil;
import br.com.raizesnordeste.raizesnordeste.exception.DadosInvalidosException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


// centraliza as regras de negócio relacionadas aos usuarios
@Service
public class UsuarioService {

    // responsavel pelas consultas e persistencia dos usuarios
    private final UsuarioRepository usuarioRepository;

    // responsavel pela consulta dos perfis de acesso
    private final PerfilRepository perfilRepository;

    // responsavel pela geracao segura do hash das senhas
    private final PasswordEncoder passwordEncoder;

    // recebe as dependencias por injeção de construtor
    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PerfilRepository perfilRepository,
            PasswordEncoder passwordEncoder
    ) {
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
}