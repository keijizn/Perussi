package com.perussi.service;

import com.perussi.dto.FavoritoDTO;
import com.perussi.model.Favorito;
import com.perussi.model.Produto;
import com.perussi.model.Usuario;
import com.perussi.repository.FavoritoRepository;
import com.perussi.repository.ProdutoRepository;
import com.perussi.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class FavoritoService {

    private final FavoritoRepository favoritoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProdutoRepository produtoRepository;

    public FavoritoService(
            FavoritoRepository favoritoRepository,
            UsuarioRepository usuarioRepository,
            ProdutoRepository produtoRepository
    ) {
        this.favoritoRepository = favoritoRepository;
        this.usuarioRepository = usuarioRepository;
        this.produtoRepository = produtoRepository;
    }

    @Transactional
    public FavoritoDTO adicionarFavorito(String emailUsuario, UUID produtoId) {
        Usuario usuario = buscarUsuarioPorEmail(emailUsuario);

        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Produto não encontrado"
                ));

        return favoritoRepository
                .findByUsuarioIdAndProdutoId(usuario.getId(), produtoId)
                .map(this::converterParaDTO)
                .orElseGet(() -> {
                    Favorito favorito = Favorito.builder()
                            .usuario(usuario)
                            .produto(produto)
                            .criadoEm(LocalDateTime.now())
                            .build();

                    Favorito salvo = favoritoRepository.save(favorito);

                    return converterParaDTO(salvo);
                });
    }

    public List<FavoritoDTO> listarFavoritos(String emailUsuario) {
        Usuario usuario = buscarUsuarioPorEmail(emailUsuario);

        return favoritoRepository.findByUsuarioId(usuario.getId())
                .stream()
                .map(this::converterParaDTO)
                .toList();
    }

    @Transactional
    public void removerFavorito(String emailUsuario, UUID produtoId) {
        Usuario usuario = buscarUsuarioPorEmail(emailUsuario);

        if (!favoritoRepository.existsByUsuarioIdAndProdutoId(usuario.getId(), produtoId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Favorito não encontrado"
            );
        }

        favoritoRepository.deleteByUsuarioIdAndProdutoId(usuario.getId(), produtoId);
    }

    private Usuario buscarUsuarioPorEmail(String emailUsuario) {
        return usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário não encontrado"
                ));
    }

    private FavoritoDTO converterParaDTO(Favorito favorito) {
        Produto produto = favorito.getProduto();

        return new FavoritoDTO(
                favorito.getId(),
                produto.getId(),
                produto.getNome(),
                produto.getDescricao(),
                produto.getPreco(),
                produto.getDesconto() == null ? 0 : produto.getDesconto(),
                produto.getCategoria(),
                produto.getImagemUrl(),
                favorito.getCriadoEm()
        );
    }
}