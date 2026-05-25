package br.ufc.llm.lesson.service;

import br.ufc.llm.lesson.domain.Lesson;
import br.ufc.llm.lesson.dto.LessonRequest;
import br.ufc.llm.lesson.dto.LessonResponse;
import br.ufc.llm.lesson.repository.LessonRepository;
import br.ufc.llm.module.repository.ModuleRepository;
import br.ufc.llm.shared.exception.RecursoNaoEncontradoException;
import br.ufc.llm.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final ModuleRepository moduleRepository;
    private final Tika tika = new Tika();

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    public Mono<LessonResponse> criar(Long moduleId, LessonRequest request, FilePart arquivo) {
        return moduleRepository.existsById(moduleId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new RecursoNaoEncontradoException("Módulo não encontrado: " + moduleId));
                    }
                    return lessonRepository.countByModuleId(moduleId);
                })
                .flatMap(count -> {
                    LocalDateTime now = LocalDateTime.now();
                    Lesson lesson = Lesson.builder()
                            .name(request.name())
                            .orderNum(count.intValue() + 1)
                            .contentEditor(request.contentEditor())
                            .moduleId(moduleId)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();

                    if (arquivo != null) {
                        return salvarArquivo(arquivo)
                                .flatMap(result -> {
                                    lesson.setFilePath(result.path());
                                    lesson.setFileType(result.tipo());
                                    return lessonRepository.save(lesson);
                                });
                    }
                    return lessonRepository.save(lesson);
                })
                .map(LessonResponse::from);
    }

    public Mono<List<LessonResponse>> listarPorModulo(Long moduleId) {
        return moduleRepository.existsById(moduleId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new RecursoNaoEncontradoException("Módulo não encontrado: " + moduleId));
                    }
                    return lessonRepository.findByModuleIdOrderByOrderNumAsc(moduleId)
                            .map(LessonResponse::from)
                            .collectList();
                });
    }

    public Mono<LessonResponse> buscarPorId(Long id) {
        return lessonRepository.findById(id)
                .map(LessonResponse::from)
                .switchIfEmpty(Mono.error(new RecursoNaoEncontradoException("Aula não encontrada: " + id)));
    }

    public Mono<Lesson> buscarEntidade(Long id) {
        return lessonRepository.findById(id)
                .switchIfEmpty(Mono.error(new RecursoNaoEncontradoException("Aula não encontrada: " + id)));
    }

    public Mono<Void> salvarConteudoGerado(Lesson lesson) {
        lesson.setUpdatedAt(LocalDateTime.now());
        return lessonRepository.save(lesson).then();
    }

    private record ArquivoSalvo(String path, String tipo) {}

    private Mono<ArquivoSalvo> salvarArquivo(FilePart filePart) {
        return Mono.fromCallable(() -> {
            Path dir = Paths.get(uploadDir);
            Files.createDirectories(dir);
            String nome = UUID.randomUUID() + "_" + filePart.filename();
            return dir.resolve(nome).toString();
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(destPath -> {
            String tipo = detectarTipo(filePart.filename());
            return filePart.transferTo(Paths.get(destPath))
                    .thenReturn(new ArquivoSalvo(destPath, tipo));
        });
    }

    private String detectarTipo(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) return "PDF";
        if (lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".avi")) return "VIDEO";
        return "PDF";
    }
}
