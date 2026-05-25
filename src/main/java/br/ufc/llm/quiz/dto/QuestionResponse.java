package br.ufc.llm.quiz.dto;

import br.ufc.llm.quiz.domain.Question;

import java.util.List;

public record QuestionResponse(
        Long id,
        String statement,
        int points,
        int orderNum,
        List<AlternativeResponse> alternatives
) {
    public static QuestionResponse from(Question q, List<AlternativeResponse> alternatives) {
        return new QuestionResponse(q.getId(), q.getStatement(), q.getPoints(), q.getOrderNum(), alternatives);
    }
}
