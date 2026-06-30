package com.perussi.service;

import com.perussi.dto.CarrinhoItemDTO;
import com.perussi.dto.CarrinhoRequestDTO;
import com.perussi.model.CarrinhoItem;
import com.perussi.model.Produto;
import com.perussi.model.ProdutoVariacao;
import com.perussi.model.Usuario;
import com.perussi.repository.CarrinhoItemRepository;
import com.perussi.repository.ProdutoRepository;
import com.perussi.repository.ProdutoVariacaoRepository;
import com.perussi.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class CarrinhoService {

        private final CarrinhoItemRepository carrinhoItemRepository;
        private final UsuarioRepository usuarioRepository;
        private final ProdutoRepository produtoRepository;
        private final ProdutoVariacaoRepository produtoVariacaoRepository;

        public CarrinhoService(
                        CarrinhoItemRepository carrinhoItemRepository,
                        UsuarioRepository usuarioRepository,
                        ProdutoRepository produtoRepository,
                        ProdutoVariacaoRepository produtoVariacaoRepository) {
                this.carrinhoItemRepository = carrinhoItemRepository;
                this.usuarioRepository = usuarioRepository;
                this.produtoRepository = produtoRepository;
                this.produtoVariacaoRepository = produtoVariacaoRepository;
        }

        public CarrinhoItemDTO adicionarItem(String emailUsuario, CarrinhoRequestDTO dto) {
                Usuario usuario = buscarUsuarioPorEmail(emailUsuario);

                Produto produto = produtoRepository.findById(dto.produtoId())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Produto não encontrado"));

                ProdutoVariacao variacao = produtoVariacaoRepository
                                .findByIdAndProdutoId(dto.variacaoId(), dto.produtoId())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Variação não encontrada para este produto"));

                CarrinhoItem item = carrinhoItemRepository
                                .findByUsuarioIdAndProdutoIdAndVariacaoId(
                                                usuario.getId(),
                                                produto.getId(),
                                                variacao.getId())
                                .orElse(null);

                int quantidadeAtual = item == null ? 0 : item.getQuantidade();
                int novaQuantidade = quantidadeAtual + dto.quantidade();

                if (novaQuantidade > variacao.getEstoque()) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Estoque insuficiente. Disponível: " + variacao.getEstoque());
                }

                if (item == null) {
                        item = CarrinhoItem.builder()
                                        .usuario(usuario)
                                        .produto(produto)
                                        .variacao(variacao)
                                        .quantidade(dto.quantidade())
                                        .build();
                } else {
                        item.setQuantidade(novaQuantidade);
                }

                CarrinhoItem salvo = carrinhoItemRepository.save(item);

                return converterParaDTO(salvo);
        }

        public List<CarrinhoItemDTO> listarItens(String emailUsuario) {
                Usuario usuario = buscarUsuarioPorEmail(emailUsuario);

                return carrinhoItemRepository.findByUsuarioId(usuario.getId())
                                .stream()
                                .map(this::converterParaDTO)
                                .toList();
        }

        public CarrinhoItemDTO atualizarQuantidade(
                        String emailUsuario,
                        UUID itemId,
                        Integer quantidade) {
                Usuario usuario = buscarUsuarioPorEmail(emailUsuario);

                CarrinhoItem item = carrinhoItemRepository.findByIdAndUsuarioId(
                                itemId,
                                usuario.getId()).orElseThrow(
                                                () -> new ResponseStatusException(
                                                                HttpStatus.NOT_FOUND,
                                                                "Item do carrinho não encontrado"));

                if (quantidade == null || quantidade <= 0) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Quantidade deve ser maior que zero");
                }

                ProdutoVariacao variacao = item.getVariacao();

                if (quantidade > variacao.getEstoque()) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Estoque insuficiente. Disponível: " + variacao.getEstoque());
                }

                item.setQuantidade(quantidade);

                CarrinhoItem salvo = carrinhoItemRepository.save(item);

                return converterParaDTO(salvo);
        }

        public void removerItem(String emailUsuario, UUID itemId) {
                Usuario usuario = buscarUsuarioPorEmail(emailUsuario);

                CarrinhoItem item = carrinhoItemRepository.findByIdAndUsuarioId(itemId, usuario.getId())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Item do carrinho não encontrado"));

                carrinhoItemRepository.delete(item);
        }

        private Usuario buscarUsuarioPorEmail(String emailUsuario) {
                return usuarioRepository.findByEmail(emailUsuario)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Usuário não encontrado"));
        }

        private CarrinhoItemDTO converterParaDTO(CarrinhoItem item) {
                Integer desconto = item.getProduto().getDesconto() == null
                                ? 0
                                : item.getProduto().getDesconto();

                BigDecimal precoComDesconto = item.getProduto().getPreco()
                                .multiply(BigDecimal.valueOf(1 - desconto / 100.0));
                return new CarrinhoItemDTO(
                                item.getId(),
                                item.getProduto().getId(),
                                item.getProduto().getNome(),
                                item.getProduto().getImagemUrl(),
                                item.getProduto().getPreco(),
                                desconto,
                                precoComDesconto,
                                item.getVariacao().getId(),
                                item.getVariacao().getTamanho(),
                                item.getVariacao().getCor(),
                                item.getQuantidade(),
                                item.getVariacao().getEstoque());
        }
}