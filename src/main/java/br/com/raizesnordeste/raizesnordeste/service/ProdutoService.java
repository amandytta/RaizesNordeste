package br.com.raizesnordeste.raizesnordeste.service;

import br.com.raizesnordeste.raizesnordeste.dto.ProdutoRequestDTO;
import br.com.raizesnordeste.raizesnordeste.dto.ProdutoResponseDTO;
import br.com.raizesnordeste.raizesnordeste.entity.Categoria;
import br.com.raizesnordeste.raizesnordeste.entity.Produto;
import br.com.raizesnordeste.raizesnordeste.repository.CategoriaRepository;
import br.com.raizesnordeste.raizesnordeste.repository.ProdutoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;

    public ProdutoService(
            ProdutoRepository produtoRepository,
            CategoriaRepository categoriaRepository
    ) {
        this.produtoRepository = produtoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponseDTO> listarTodos() {
        return produtoRepository.findAll()
                .stream()
                .map(ProdutoResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProdutoResponseDTO buscarPorId(Long id) {

        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Produto não encontrado: " + id
                ));

        return ProdutoResponseDTO.fromEntity(produto);
    }

    @Transactional
    public ProdutoResponseDTO cadastrar(ProdutoRequestDTO dto) {

        Categoria categoria = categoriaRepository
                .findById(dto.categoriaId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Categoria não encontrada: " + dto.categoriaId()
                ));

        if (produtoRepository.existsByNomeAndCategoriaId(
                dto.nome(), dto.categoriaId())) {

            throw new IllegalArgumentException(
                    "Já existe um produto com este nome nesta categoria."
            );
        }

        Produto produto = new Produto();

        produto.setCategoria(categoria);
        produto.setNome(dto.nome());
        produto.setDescricao(dto.descricao());
        produto.setImagemUrl(dto.imagemUrl());

        Produto produtoSalvo = produtoRepository.save(produto);

        return ProdutoResponseDTO.fromEntity(produtoSalvo);
    }
}