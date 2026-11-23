package br.com.raizesnordeste.raizesnordeste.service;

import br.com.raizesnordeste.raizesnordeste.dto.UnidadeRequestDTO;
import br.com.raizesnordeste.raizesnordeste.dto.UnidadeResponseDTO;
import br.com.raizesnordeste.raizesnordeste.entity.Unidade;
import br.com.raizesnordeste.raizesnordeste.repository.UnidadeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class UnidadeService {

    private final UnidadeRepository unidadeRepository;

    public UnidadeService(UnidadeRepository unidadeRepository) {
        this.unidadeRepository = unidadeRepository;
    }

    @Transactional(readOnly = true)
    public List<UnidadeResponseDTO> listarTodas() {
        return unidadeRepository.findAll()
                .stream()
                .map(UnidadeResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public UnidadeResponseDTO buscarPorId(Long id) {
        Unidade unidade = unidadeRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Unidade não encontrada: " + id
                        )
                );

        return UnidadeResponseDTO.fromEntity(unidade);
    }

    @Transactional
    public UnidadeResponseDTO cadastrar(UnidadeRequestDTO dto) {

        if (unidadeRepository.existsByCnpj(dto.cnpj())) {
            throw new IllegalArgumentException(
                    "Já existe uma unidade cadastrada com este CNPJ."
            );
        }

        Unidade unidade = new Unidade();

        unidade.setNome(dto.nome());
        unidade.setCnpj(dto.cnpj());
        unidade.setTelefone(dto.telefone());
        unidade.setEmail(dto.email());
        unidade.setCep(dto.cep());
        unidade.setLogradouro(dto.logradouro());
        unidade.setNumero(dto.numero());
        unidade.setComplemento(dto.complemento());
        unidade.setBairro(dto.bairro());
        unidade.setCidade(dto.cidade());
        unidade.setEstado(dto.estado());

        Unidade unidadeSalva = unidadeRepository.save(unidade);

        return UnidadeResponseDTO.fromEntity(unidadeSalva);
    }
}