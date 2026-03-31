package br.ufc.llm.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean sucesso,
        String mensagem,
        T dados,
        LocalDateTime timestamp
) {
    public static <T> ApiResponse<T> ok(String mensagem, T dados) {
        return new ApiResponse<>(true, mensagem, dados, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> ok(T dados) {
        return new ApiResponse<>(true, null, dados, LocalDateTime.now());
    }

    public static ApiResponse<Void> erro(String mensagem) {
        return new ApiResponse<>(false, mensagem, null, LocalDateTime.now());
    }
}
