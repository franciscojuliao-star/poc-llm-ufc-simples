package br.ufc.llm.quiz.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("quizzes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quiz {

    @Id
    private Long id;

    @Column("module_id")
    private Long moduleId;

    @Column("show_wrong_answers")
    private boolean showWrongAnswers;

    @Column("show_correct_answers")
    private boolean showCorrectAnswers;

    @Column("show_points")
    private boolean showPoints;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}
