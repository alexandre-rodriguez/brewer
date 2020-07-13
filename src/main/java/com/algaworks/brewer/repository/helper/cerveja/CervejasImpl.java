package com.algaworks.brewer.repository.helper.cerveja;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.algaworks.brewer.dto.CervejaDTO;
import com.algaworks.brewer.dto.ValorItensEstoque;
import com.algaworks.brewer.model.Cerveja;
import com.algaworks.brewer.repository.filter.CervejaFilter;
import com.algaworks.brewer.repository.paginacao.PaginacaoUtil;
import com.algaworks.brewer.storage.FotoStorage;


public class CervejasImpl implements CervejasQueries{

	@PersistenceContext
	private EntityManager manager;
	
	@Autowired
	private PaginacaoUtil paginacaoUtil;
	
	@Autowired
	private FotoStorage fotoStorage;
	
	@SuppressWarnings("unchecked")
	@Override
	@Transactional(readOnly=true)
	public Page<Cerveja> filtrar(CervejaFilter filtro, Pageable pageable) {
		CriteriaBuilder builder = manager.getCriteriaBuilder();
		CriteriaQuery<Cerveja> query = builder.createQuery(Cerveja.class);
		Root<Cerveja> cerveja = query.from(Cerveja.class);
		
		query.select(cerveja);
		query.where(adicionarFiltro(filtro, cerveja));
		
		TypedQuery<Cerveja> typedQuery =  (TypedQuery<Cerveja>) paginacaoUtil.preparar(query, cerveja, pageable);
		
		return new PageImpl<>(typedQuery.getResultList(), pageable, total(filtro));
	}

	private Predicate[] adicionarFiltro(CervejaFilter filtro, Root<Cerveja> cerveja) {
		List<Predicate> predicateList = new ArrayList<>();
		CriteriaBuilder builder = manager.getCriteriaBuilder();

		
		if (filtro != null) {
			if (!StringUtils.isEmpty(filtro.getSku())) {
				predicateList.add(builder.equal(cerveja.get("sku"), filtro.getSku()));
			}
			
			if (!StringUtils.isEmpty(filtro.getNome())) {
				predicateList.add(builder.like(cerveja.get("nome"), "%" + filtro.getNome() + "%"));
			}

			if (isEstiloPresente(filtro)) {
				predicateList.add(builder.equal(cerveja.get("estilo"), filtro.getEstilo()));
			}

			if (filtro.getSabor() != null) {
				predicateList.add(builder.equal(cerveja.get("sabor"), filtro.getSabor()));
			}

			if (filtro.getOrigem() != null) {
				predicateList.add(builder.equal(cerveja.get("origem"), filtro.getOrigem()));
			}

			if (filtro.getValorDe() != null) {
				predicateList.add(builder.equal(cerveja.get("valor"), filtro.getValorDe()));
			}

			if (filtro.getValorAte() != null) {
				predicateList.add(builder.equal(cerveja.get("valor"), filtro.getValorAte()));
			}
		}
		
		Predicate[] predArray = new Predicate[predicateList.size()];
		return predicateList.toArray(predArray);
		
	}
	
	@Override
	public ValorItensEstoque valorItensEstoque() {
		String query = "select new com.algaworks.brewer.dto.ValorItensEstoque(sum(valor * quantidadeEstoque), sum(quantidadeEstoque)) from Cerveja";
		return manager.createQuery(query, ValorItensEstoque.class).getSingleResult();
	}
		
	private Long total(CervejaFilter filtro) {
		CriteriaBuilder criteriaBuilder = manager.getCriteriaBuilder();
		CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
		Root<Cerveja> cerveja = query.from(Cerveja.class);
		
		query.select(criteriaBuilder.count(cerveja));
		query.where(adicionarFiltro(filtro, cerveja));
		
		return manager.createQuery(query).getSingleResult();
	}

	private boolean isEstiloPresente(CervejaFilter filtro) {
		return filtro.getEstilo() != null && filtro.getEstilo().getCodigo() != null;
	}

	@Override
	public List<CervejaDTO> porSkuOuNome(String skuOuNome) {
		String jpql = "select new com.algaworks.brewer.dto.CervejaDTO(codigo, sku, nome, origem, valor, foto) " +
				"from Cerveja where lower(sku) like lower(:skuOuNome) or lower(nome) like lower(:skuOuNome)";
		List<CervejaDTO> cervejasFiltradas = manager.createQuery(jpql, CervejaDTO.class)
				.setParameter("skuOuNome", skuOuNome + "%")
				.getResultList();
		
		cervejasFiltradas.forEach(c -> c.setUrlThumbnailFoto(fotoStorage.getUrl(FotoStorage.THUMBNAIL_PREFIX + c.getFoto())));
		
		return cervejasFiltradas;
	}

}
