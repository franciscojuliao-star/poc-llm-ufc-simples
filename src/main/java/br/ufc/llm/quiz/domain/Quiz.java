package br.ufc.llm.quiz.domain;

import br.ufc.llm.module.domain.Module;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quizzes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false, unique = true)
    private Module module;

    @Column(name = "show_wrong_answers", nullable = false)
    private boolean showWrongAnswers;

    @Column(name = "show_correct_answers", nullable = false)
    private boolean showCorrectAnswers;

    @Column(name = "show_points", nullable = false)
    private boolean showPoints;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderNum ASC")
    @Builder.Default
    private List<Question> questions = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
