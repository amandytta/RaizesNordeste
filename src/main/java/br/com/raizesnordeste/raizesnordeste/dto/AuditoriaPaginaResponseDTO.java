package br.com.raizesnordeste.raizesnordeste.dto;

import java.util.List;

// representa uma resposta paginada de registros de auditoria
public record AuditoriaPaginaResponseDTO(

        // registros encontrados na página atual
        List<AuditoriaResponseDTO> conteudo,

        // número da página atual, iniciando em zero
        int pagina,

        // tamanho solicitado para a página
        int tamanho,

        // quantidade total de registros encontrados
        long totalElementos,

        // quantidade total de páginas disponíveis
        int totalPaginas,

        // indica se esta é a primeira página
        boolean primeira,

        // indica se esta é a última página
        boolean ultima

) {
}