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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository repository;
    private final Tika tika = new Tika();

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    public CourseResponse criar(CourseRequest request, MultipartFile imagem) {
        Course course = Course.builder()
                .title(request.title())
                .category(request.category())
                .description(request.description())
                .build();

        if (imagem != null && !imagem.isEmpty()) {
            validarImagem(imagem);
            course.setImagePath(salvarImagem(imagem));
        }

        return CourseResponse.from(repository.save(course));
    }

    public List<CourseResponse> listar() {
        return repository.findAll().stream()
                .map(CourseResponse::from)
                .toList();
    }

    public CourseResponse buscarPorId(Long id) {
        return repository.findById(id)
                .map(CourseResponse::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Curso não encontrado: " + id));
    }

    private void validarImagem(MultipartFile imagem) {
        try {
            String mimeType = tika.detect(imagem.getInputStream());
            if (!mimeType.startsWith("image/")) {
                throw new RegraDeNegocioException("Arquivo não é uma imagem válida: " + mimeType);
            }
        } catch (IOException e) {
            throw new RegraDeNegocioException("Erro ao validar imagem");
        }
    }

    private String salvarImagem(MultipartFile imagem) {
        try {
            Path dir = Paths.get(uploadDir);
            Files.createDirectories(dir);
            String nomeArquivo = UUID.randomUUID() + "_" + imagem.getOriginalFilename();
            Path destino = dir.resolve(nomeArquivo);
            imagem.transferTo(destino);
            return destino.toString();
        } catch (IOException e) {
            throw new RegraDeNegocioException("Erro ao salvar imagem: " + e.getMessage());
        }
    }
}
