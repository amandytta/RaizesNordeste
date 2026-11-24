package br.com.raizesnordeste.raizesnordeste.service;

import br.com.raizesnordeste.raizesnordeste.dto.ProdutoUnidadeRequestDTO;
import br.com.raizesnordeste.raizesnordeste.dto.ProdutoUnidadeResponseDTO;
import br.com.raizesnordeste.raizesnordeste.entity.Produto;
import br.com.raizesnordeste.raizesnordeste.entity.Unidade;
import br.com.raizesnordeste.raizesnordeste.entity.ProdutoUnidade;
import br.com.raizesnordeste.raizesnordeste.repository.ProdutoRepository;
import br.com.raizesnordeste.raizesnordeste.repository.UnidadeRepository;
import br.com.raizesnordeste.raizesnordeste.repository.ProdutoUnidadeRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ProdutoUnidadeService {

    private final ProdutoUnidadeRepository produtoUnidadeRepository;
    private final ProdutoRepository produtoRepository;
    private final UnidadeRepository unidadeRepository;

    public ProdutoUnidadeService(
            ProdutoUnidadeRepository produtoUnidadeRepository,
            ProdutoRepository produtoRepository,
            UnidadeRepository unidadeRepository
    ) {
        this.produtoUnidadeRepository = produtoUnidadeRepository;
        this.produtoRepository = produtoRepository;
        this.unidadeRepository = unidadeRepository;
    }

    @Transactional(readOnly = true)
    public List<ProdutoUnidadeResponseDTO> listarTodos() {

        return produtoUnidadeRepository.findAll()
                .stream()
                .map(ProdutoUnidadeResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProdutoUnidadeResponseDTO buscarPorId(Long id) {

        ProdutoUnidade produtoUnidade = produtoUnidadeRepository
                .findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Associação de produto e unidade não encontrada: " + id
                ));

        return ProdutoUnidadeResponseDTO.fromEntity(produtoUnidade);
    }

    @Transactional(readOnly = true)
    public List<ProdutoUnidadeResponseDTO> listarCardapioPorUnidade(
            Long unidadeId
    ) {

        Unidade unidade = unidadeRepository.findById(unidadeId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Unidade não encontrada: " + unidadeId
                ));

        if (!Boolean.TRUE.equals(unidade.getAtiva())) {
            throw new IllegalArgumentException(
                    "A unidade está inativa."
            );
        }

        return produtoUnidadeRepository
                .findByUnidadeIdAndDisponivelTrue(unidadeId)
                .stream()
                .filter(item -> Boolean.TRUE.equals(
                        item.getProduto().getAtivo()
                ))
                .map(ProdutoUnidadeResponseDTO::fromEntity)
                .toList();
    }

    @Transactional
    public ProdutoUnidadeResponseDTO cadastrar(
            ProdutoUnidadeRequestDTO dto
    ) {

        Produto produto = produtoRepository.findById(dto.produtoId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Produto não encontrado: " + dto.produtoId()
                ));

        Unidade unidade = unidadeRepository.findById(dto.unidadeId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Unidade não encontrada: " + dto.unidadeId()
                ));

        if (!Boolean.TRUE.equals(produto.getAtivo())) {
            throw new IllegalArgumentException(
                    "Não é possível associar um produto inativo."
            );
        }

        if (!Boolean.TRUE.equals(unidade.getAtiva())) {
            throw new IllegalArgumentException(
                    "Não é possível associar produtos a uma unidade inativa."
            );
        }

        if (produtoUnidadeRepository.existsByProdutoIdAndUnidadeId(
                dto.produtoId(), dto.unidadeId()
        )) {
            throw new IllegalArgumentException(
                    "Este produto já está cadastrado nesta unidade."
            );
        }

        ProdutoUnidade produtoUnidade = new ProdutoUnidade();

        produtoUnidade.setProduto(produto);
        produtoUnidade.setUnidade(unidade);
        produtoUnidade.setPreco(dto.preco());
        produtoUnidade.setDisponivel(dto.disponivel());
        produtoUnidade.setEstoque(0);

        ProdutoUnidade produtoUnidadeSalvo =
                produtoUnidadeRepository.save(produtoUnidade);

        return ProdutoUnidadeResponseDTO.fromEntity(
                produtoUnidadeSalvo
        );
    }
}