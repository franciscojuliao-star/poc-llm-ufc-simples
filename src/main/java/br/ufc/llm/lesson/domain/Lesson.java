package br.ufc.llm.lesson.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("lessons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lesson {

    @Id
    private Long id;

    private String name;

    @Column("order_num")
    private int orderNum;

    @Column("file_path")
    private String filePath;

    @Column("file_type")
    private String fileType;

    @Column("content_editor")
    private String contentEditor;

    @Column("content_generated")
    private String contentGenerated;

    @Column("module_id")
    private Long moduleId;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}
