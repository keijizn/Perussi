package com.perussi.controller;
import com.perussi.dto.UploadResponseDTO;
import com.perussi.service.ImagemStorageService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@RestController
@RequestMapping("/upload")
public class UploadController {
    private final ImagemStorageService storage;
    public UploadController(ImagemStorageService storage) { this.storage = storage; }
    @PostMapping("/imagem")
    public UploadResponseDTO uploadImagem(@RequestParam("file") MultipartFile file) {
        return new UploadResponseDTO(storage.enviar(file));
    }
}
