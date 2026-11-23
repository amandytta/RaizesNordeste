package br.com.raizesnordeste.raizesnordeste.dto;

import br.com.raizesnordeste.raizesnordeste.entity.Unidade;

public record UnidadeResponseDTO(
        Long id,
        String nome,
        String cnpj,
        String telefone,
        String email,
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String estado,
        Boolean ativa
) {

    public static UnidadeResponseDTO fromEntity(Unidade unidade) {
        return new UnidadeResponseDTO(
                unidade.getId(),
                unidade.getNome(),
                unidade.getCnpj(),
                unidade.getTelefone(),
                unidade.getEmail(),
                unidade.getCep(),
                unidade.getLogradouro(),
                unidade.getNumero(),
                unidade.getComplemento(),
                unidade.getBairro(),
                unidade.getCidade(),
                unidade.getEstado(),
                unidade.getAtiva()
        );
    }
}