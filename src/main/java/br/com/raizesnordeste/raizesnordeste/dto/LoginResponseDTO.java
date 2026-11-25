package br.com.raizesnordeste.raizesnordeste.dto;

// representa a resposta retornada apos uma autenticacao bem-sucedida
public record LoginResponseDTO(

        // token JWT utilizado para acessar os endpoints protegidos
        String token,

        // tipo de autenticacao utilizado nas requisicoes
        String tipo,

        // tempo de validade do token em segundos
        long expiraEm

) {

    // evita a exposicao acidental do token JWT em logs
    @Override
    public String toString() {
        return "LoginResponseDTO[token protegido, tipo="
                + tipo + ", expiraEm=" + expiraEm + "]";
    }
}