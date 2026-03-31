package br.ufc.llm.lesson.domain;

import br.ufc.llm.module.domain.Module;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "lessons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "order_num", nullable = false)
    private int orderNum;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", length = 10)
    private FileType fileType;

    @Column(name = "content_editor", columnDefinition = "TEXT")
    private String contentEditor;

    @Column(name = "content_generated", columnDefinition = "TEXT")
    private String contentGenerated;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
