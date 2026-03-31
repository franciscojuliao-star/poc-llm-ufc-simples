package br.ufc.llm.lesson.service;

import br.ufc.llm.lesson.domain.FileType;
import br.ufc.llm.lesson.domain.Lesson;
import br.ufc.llm.lesson.dto.LessonRequest;
import br.ufc.llm.lesson.dto.LessonResponse;
import br.ufc.llm.lesson.repository.LessonRepository;
import br.ufc.llm.module.domain.Module;
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
public class LessonService {

    private final LessonRepository lessonRepository;
    private final ModuleRepository moduleRepository;
    private final Tika tika = new Tika();

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    public LessonResponse criar(Long moduleId, LessonRequest request, MultipartFile arquivo) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Módulo não encontrado: " + moduleId));

        int ordem = lessonRepository.countByModuleId(moduleId) + 1;

        Lesson lesson = Lesson.builder()
                .name(request.name())
                .orderNum(ordem)
                .contentEditor(request.contentEditor())
                .module(module)
                .build();

        if (arquivo != null && !arquivo.isEmpty()) {
            String filePath = salvarArquivo(arquivo);
            FileType fileType = detectarTipo(arquivo);
            lesson.setFilePath(filePath);
            lesson.setFileType(fileType);
        }

        return LessonResponse.from(lessonRepository.save(lesson));
    }

    public List<LessonResponse> listarPorModulo(Long moduleId) {
        if (!moduleRepository.existsById(moduleId)) {
            throw new RecursoNaoEncontradoException("Módulo não encontrado: " + moduleId);
        }
        return lessonRepository.findByModuleIdOrderByOrderNumAsc(moduleId).stream()
                .map(LessonResponse::from)
                .toList();
    }

    public LessonResponse buscarPorId(Long id) {
        return lessonRepository.findById(id)
                .map(LessonResponse::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aula não encontrada: " + id));
    }

    public Lesson buscarEntidade(Long id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aula não encontrada: " + id));
    }

    public void salvarConteudoGerado(Lesson lesson) {
        lessonRepository.save(lesson);
    }

    private String salvarArquivo(MultipartFile arquivo) {
        try {
            Path dir = Paths.get(uploadDir);
            Files.createDirectories(dir);
            String nomeArquivo = UUID.randomUUID() + "_" + arquivo.getOriginalFilename();
            Path destino = dir.resolve(nomeArquivo);
            arquivo.transferTo(destino);
            return destino.toString();
        } catch (IOException e) {
            throw new RegraDeNegocioException("Erro ao salvar arquivo: " + e.getMessage());
        }
    }

    private FileType detectarTipo(MultipartFile arquivo) {
        try {
            String mimeType = tika.detect(arquivo.getInputStream());
            if (mimeType.equals("application/pdf")) return FileType.PDF;
            if (mimeType.startsWith("video/")) return FileType.VIDEO;
            throw new RegraDeNegocioException("Tipo de arquivo não suportado: " + mimeType);
        } catch (IOException e) {
            throw new RegraDeNegocioException("Erro ao detectar tipo do arquivo");
        }
    }
}
