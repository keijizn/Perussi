package com.perussi.service;

import com.perussi.dto.ProdutoDTO;
import com.perussi.dto.ProdutoRequestDTO;
import com.perussi.dto.ProdutoVariacaoDTO;
import com.perussi.dto.ProdutoVariacaoRequestDTO;
import com.perussi.model.Produto;
import com.perussi.model.ProdutoVariacao;
import com.perussi.repository.ProdutoRepository;
import com.perussi.repository.ProdutoVariacaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ProdutoService {

        private final ProdutoRepository produtoRepository;
        private final ProdutoVariacaoRepository produtoVariacaoRepository;

        public ProdutoService(
                        ProdutoRepository produtoRepository,
                        ProdutoVariacaoRepository produtoVariacaoRepository) {
                this.produtoRepository = produtoRepository;
                this.produtoVariacaoRepository = produtoVariacaoRepository;
        }

        public ProdutoDTO criar(ProdutoRequestDTO dto) {
                validarDesconto(dto.desconto());
                Produto produto = Produto.builder()
                                .nome(dto.nome())
                                .descricao(dto.descricao())
                                .preco(dto.preco())
                                .desconto(dto.desconto() == null ? 0 : dto.desconto())
                                .categoria(dto.categoria())
                                .imagemUrl(dto.imagemUrl())
                                .destaque(dto.destaque())
                                .variacoes(new ArrayList<>())
                                .build();

                if (dto.variacoes() != null) {
                        dto.variacoes().forEach(variacaoDTO -> {
                                ProdutoVariacao variacao = ProdutoVariacao.builder()
                                                .tamanho(variacaoDTO.tamanho())
                                                .cor(variacaoDTO.cor())
                                                .estoque(variacaoDTO.estoque())
                                                .produto(produto)
                                                .build();

                                produto.getVariacoes().add(variacao);
                        });
                }

                Produto salvo = produtoRepository.save(produto);

                return converterParaDTO(salvo);
        }

        public ProdutoVariacaoDTO criarVariacao(ProdutoVariacaoRequestDTO dto) {
                Produto produto = produtoRepository.findById(dto.produtoId())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Produto não encontrado"));

                ProdutoVariacao variacao = ProdutoVariacao.builder()
                                .produto(produto)
                                .tamanho(dto.tamanho())
                                .cor(dto.cor())
                                .estoque(dto.estoque())
                                .build();

                ProdutoVariacao salva = produtoVariacaoRepository.save(variacao);

                return new ProdutoVariacaoDTO(
                                salva.getId(),
                                salva.getTamanho(),
                                salva.getCor(),
                                salva.getEstoque());
        }

        public List<ProdutoDTO> listarTodos() {
                return produtoRepository.findAll()
                                .stream()
                                .map(this::converterParaDTO)
                                .toList();
        }

        public ProdutoDTO buscarPorId(UUID id) {
                Produto produto = produtoRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Produto não encontrado"));

                return converterParaDTO(produto);
        }

        public ProdutoVariacao buscarVariacao(UUID produtoId, UUID variacaoId) {
                return produtoVariacaoRepository
                                .findByIdAndProdutoId(variacaoId, produtoId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Variação não encontrada"));
        }

        public ProdutoDTO atualizar(UUID id, ProdutoRequestDTO dto) {
                Produto produto = produtoRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Produto não encontrado"));

                produto.setNome(dto.nome());
                produto.setDescricao(dto.descricao());
                produto.setPreco(dto.preco());
                validarDesconto(dto.desconto());
                produto.setDesconto(dto.desconto() == null ? 0 : dto.desconto());
                produto.setCategoria(dto.categoria());
                produto.setImagemUrl(dto.imagemUrl());
                produto.setDestaque(dto.destaque());

                Produto salvo = produtoRepository.save(produto);

                return converterParaDTO(salvo);
        }

        public void excluir(UUID id) {
                Produto produto = produtoRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Produto não encontrado"));

                produtoRepository.delete(produto);
        }

        public ProdutoVariacaoDTO atualizarVariacao(
                        UUID id,
                        ProdutoVariacaoRequestDTO dto) {
                ProdutoVariacao variacao = produtoVariacaoRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Variação não encontrada"));

                variacao.setTamanho(dto.tamanho());
                variacao.setCor(dto.cor());
                variacao.setEstoque(dto.estoque());

                ProdutoVariacao salva = produtoVariacaoRepository.save(variacao);

                return new ProdutoVariacaoDTO(
                                salva.getId(),
                                salva.getTamanho(),
                                salva.getCor(),
                                salva.getEstoque());
        }

        public void excluirVariacao(UUID id) {
                ProdutoVariacao variacao = produtoVariacaoRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Variação não encontrada"));

                produtoVariacaoRepository.delete(variacao);
        }

        private ProdutoDTO converterParaDTO(Produto produto) {
                List<ProdutoVariacaoDTO> variacoes = produto.getVariacoes()
                                .stream()
                                .map(variacao -> new ProdutoVariacaoDTO(
                                                variacao.getId(),
                                                variacao.getTamanho(),
                                                variacao.getCor(),
                                                variacao.getEstoque()))
                                .toList();

                return new ProdutoDTO(
                                produto.getId(),
                                produto.getNome(),
                                produto.getDescricao(),
                                produto.getPreco(),
                                produto.getDesconto() == null ? 0 : produto.getDesconto(),
                                produto.getCategoria(),
                                produto.getImagemUrl(),
                                produto.getDestaque(),
                                variacoes);
        }

        private void validarDesconto(Integer desconto) {
                if (desconto != null && (desconto < 0 || desconto > 100)) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Desconto deve estar entre 0 e 100");
                }
        }
}