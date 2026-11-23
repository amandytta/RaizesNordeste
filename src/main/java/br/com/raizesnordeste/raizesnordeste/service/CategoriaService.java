package br.com.raizesnordeste.raizesnordeste.service;

import br.com.raizesnordeste.raizesnordeste.dto.CategoriaRequestDTO;
import br.com.raizesnordeste.raizesnordeste.dto.CategoriaResponseDTO;
import br.com.raizesnordeste.raizesnordeste.entity.Categoria;
import br.com.raizesnordeste.raizesnordeste.repository.CategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponseDTO> listarTodas() {
        return categoriaRepository.findAll()
                .stream()
                .map(CategoriaResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponseDTO buscarPorId(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Categoria não encontrada: " + id
                ));

        return CategoriaResponseDTO.fromEntity(categoria);
    }

    @Transactional
    public CategoriaResponseDTO cadastrar(CategoriaRequestDTO dto) {

        if (categoriaRepository.existsByNome(dto.nome())) {
            throw new IllegalArgumentException(
                    "Já existe uma categoria cadastrada com este nome."
            );
        }

        Categoria categoria = new Categoria();
        categoria.setNome(dto.nome());
        categoria.setDescricao(dto.descricao());

        Categoria categoriaSalva = categoriaRepository.save(categoria);

        return CategoriaResponseDTO.fromEntity(categoriaSalva);
    }
}