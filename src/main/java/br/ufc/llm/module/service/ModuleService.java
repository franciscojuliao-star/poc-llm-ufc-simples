package br.ufc.llm.module.service;

import br.ufc.llm.course.repository.CourseRepository;
import br.ufc.llm.module.domain.Module;
import br.ufc.llm.module.dto.ModuleRequest;
import br.ufc.llm.module.dto.ModuleResponse;
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
public class ModuleService {

    private final ModuleRepository moduleRepository;
    private final CourseRepository courseRepository;
    private final Tika tika = new Tika();

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    public Mono<ModuleResponse> criar(Long courseId, ModuleRequest request, FilePart imagem) {
        return courseRepository.existsById(courseId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new RecursoNaoEncontradoException("Curso não encontrado: " + courseId));
                    }
                    return moduleRepository.countByCourseId(courseId);
                })
                .flatMap(count -> {
                    LocalDateTime now = LocalDateTime.now();
                    Module module = Module.builder()
                            .name(request.name())
                            .orderNum(count.intValue() + 1)
                            .courseId(courseId)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();

                    if (imagem != null) {
                        return salvarImagem(imagem)
                                .flatMap(path -> {
                                    module.setImagePath(path);
                                    return moduleRepository.save(module);
                                });
                    }
                    return moduleRepository.save(module);
                })
                .map(ModuleResponse::from);
    }

    public Mono<List<ModuleResponse>> listarPorCurso(Long courseId) {
        return courseRepository.existsById(courseId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new RecursoNaoEncontradoException("Curso não encontrado: " + courseId));
                    }
                    return moduleRepository.findByCourseIdOrderByOrderNumAsc(courseId)
                            .map(ModuleResponse::from)
                            .collectList();
                });
    }

    public Mono<ModuleResponse> buscarPorId(Long id) {
        return moduleRepository.findById(id)
                .map(ModuleResponse::from)
                .switchIfEmpty(Mono.error(new RecursoNaoEncontradoException("Módulo não encontrado: " + id)));
    }

    private Mono<String> salvarImagem(FilePart filePart) {
        return Mono.fromCallable(() -> {
            Path dir = Paths.get(uploadDir);
            Files.createDirectories(dir);
            return dir.resolve(UUID.randomUUID() + "_" + filePart.filename()).toString();
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(destPath -> filePart.transferTo(Paths.get(destPath)).thenReturn(destPath));
    }
}
