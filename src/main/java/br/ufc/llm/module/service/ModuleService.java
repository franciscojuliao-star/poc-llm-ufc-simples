package br.ufc.llm.module.service;

import br.ufc.llm.course.domain.Course;
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
public class ModuleService {

    private final ModuleRepository moduleRepository;
    private final CourseRepository courseRepository;
    private final Tika tika = new Tika();

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    public ModuleResponse criar(Long courseId, ModuleRequest request, MultipartFile imagem) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Curso não encontrado: " + courseId));

        int ordem = moduleRepository.countByCourseId(courseId) + 1;

        Module module = Module.builder()
                .name(request.name())
                .orderNum(ordem)
                .course(course)
                .build();

        if (imagem != null && !imagem.isEmpty()) {
            validarImagem(imagem);
            module.setImagePath(salvarImagem(imagem));
        }

        return ModuleResponse.from(moduleRepository.save(module));
    }

    public List<ModuleResponse> listarPorCurso(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new RecursoNaoEncontradoException("Curso não encontrado: " + courseId);
        }
        return moduleRepository.findByCourseIdOrderByOrderNumAsc(courseId).stream()
                .map(ModuleResponse::from)
                .toList();
    }

    public ModuleResponse buscarPorId(Long id) {
        return moduleRepository.findById(id)
                .map(ModuleResponse::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Módulo não encontrado: " + id));
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
