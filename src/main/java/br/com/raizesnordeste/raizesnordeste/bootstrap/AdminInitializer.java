package br.com.raizesnordeste.raizesnordeste.bootstrap;

import br.com.raizesnordeste.raizesnordeste.entity.Perfil;
import br.com.raizesnordeste.raizesnordeste.entity.Usuario;
import br.com.raizesnordeste.raizesnordeste.repository.PerfilRepository;
import br.com.raizesnordeste.raizesnordeste.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(
        name = "app.bootstrap.admin.enabled",
        havingValue = "true"
)
public class AdminInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin.nome}")
    private String nome;

    @Value("${app.bootstrap.admin.email}")
    private String email;

    @Value("${app.bootstrap.admin.senha}")
    private String senha;

    public AdminInitializer(
            UsuarioRepository usuarioRepository,
            PerfilRepository perfilRepository,
            PasswordEncoder passwordEncoder) {

        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {

        if (usuarioRepository.existsByEmail(email)) {
            System.out.println(
                    "Bootstrap ADMIN: usuário já existe. Nenhuma alteração realizada."
            );
            return;
        }

        Perfil perfilAdmin = perfilRepository.findByNome("ADMIN")
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Perfil ADMIN não encontrado no banco."
                        )
                );

        Usuario usuario = new Usuario();

        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuario.setAtivo(true);
        usuario.getPerfis().add(perfilAdmin);

        usuarioRepository.save(usuario);

        System.out.println(
                "Bootstrap ADMIN: usuário criado com sucesso."
        );
    }
}