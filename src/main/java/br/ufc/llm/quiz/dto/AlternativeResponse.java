package br.ufc.llm.quiz.dto;

import br.ufc.llm.quiz.domain.Alternative;

public record AlternativeResponse(
        Long id,
        String text,
        boolean correct
) {
    public static AlternativeResponse from(Alternative a) {
        return new AlternativeResponse(a.getId(), a.getText(), a.isCorrect());
    }
}
