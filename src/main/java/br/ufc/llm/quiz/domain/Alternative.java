package br.ufc.llm.quiz.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("alternatives")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alternative {

    @Id
    private Long id;

    private String text;
    private boolean correct;

    @Column("question_id")
    private Long questionId;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}
