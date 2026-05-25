package br.ufc.llm.course.service;

import br.ufc.llm.course.domain.Course;
import br.ufc.llm.course.dto.CourseRequest;
import br.ufc.llm.course.dto.CourseResponse;
import br.ufc.llm.course.repository.CourseRepository;
import br.ufc.llm.shared.exception.RecursoNaoEncontradoException;
import br.ufc.llm.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository repository;
    private final Tika tika = new Tika();

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    public Mono<CourseResponse> criar(CourseRequest request, FilePart imagem) {
        LocalDateTime now = LocalDateTime.now();
        Course course = Course.builder()
                .title(request.title())
                .category(request.category())
                .description(request.description())
                .createdAt(now)
                .updatedAt(now)
                .build();

        if (imagem != null) {
            return salvarImagem(imagem)
                    .flatMap(path -> {
                        course.setImagePath(path);
                        return repository.save(course);
                    })
                    .map(CourseResponse::from);
        }

        return repository.save(course).map(CourseResponse::from);
    }

    public Mono<List<CourseResponse>> listar(int page, int perPage) {
        return repository.findAllBy(PageRequest.of(page - 1, perPage))
                .map(CourseResponse::from)
                .collectList();
    }

    public Mono<CourseResponse> buscarPorId(Long id) {
        return repository.findById(id)
                .map(CourseResponse::from)
                .switchIfEmpty(Mono.error(new RecursoNaoEncontradoException("Curso não encontrado: " + id)));
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
