package com.perussi.controller;

import com.perussi.dto.UploadResponseDTO;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@RestController
@RequestMapping("/upload")
@CrossOrigin("*")
public class UploadController {

    @PostMapping("/imagem")
    public UploadResponseDTO uploadImagem(
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        if (file.isEmpty()) {
            throw new RuntimeException("Arquivo vazio");
        }

        String nomeOriginal = file.getOriginalFilename();

        if (nomeOriginal == null) {
            nomeOriginal = "imagem";
        }

        String nomeArquivo = UUID.randomUUID() + "_" + nomeOriginal.replaceAll("\\s+", "_");

        Path pastaUploads = Path.of("uploads").toAbsolutePath();

        Files.createDirectories(pastaUploads);

        Path destino = pastaUploads.resolve(nomeArquivo);

        file.transferTo(destino.toFile());

        return new UploadResponseDTO(
                "http://localhost:8080/api/uploads/" + nomeArquivo
        );
    }
}