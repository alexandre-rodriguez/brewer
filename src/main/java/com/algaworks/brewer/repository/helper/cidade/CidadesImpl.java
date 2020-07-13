package com.algaworks.brewer.repository.helper.cidade;

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

import com.algaworks.brewer.model.Cidade;
import com.algaworks.brewer.repository.filter.CidadeFilter;
import com.algaworks.brewer.repository.paginacao.PaginacaoUtil;

public class CidadesImpl implements CidadesQueries {

	@PersistenceContext
	private EntityManager manager;
	
	@Autowired
	private PaginacaoUtil paginacaoUtil;
	
	@SuppressWarnings("unchecked")
	@Override
	@Transactional(readOnly = true)
	public Page<Cidade> filtrar(CidadeFilter filtro, Pageable pageable) {
		CriteriaBuilder builder = manager.getCriteriaBuilder();
		CriteriaQuery<Cidade> query = builder.createQuery(Cidade.class);
		Root<Cidade> cidade = query.from(Cidade.class);
		
		query.select(cidade);
		query.where(adicionarFiltro(filtro, cidade));
		
		TypedQuery<Cidade> typedQuery =  (TypedQuery<Cidade>) paginacaoUtil.preparar(query, cidade, pageable);
		
		return new PageImpl<>(typedQuery.getResultList(), pageable, total(filtro));
	}
	
	private Long total(CidadeFilter filtro) {
		CriteriaBuilder criteriaBuilder = manager.getCriteriaBuilder();
		CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
		Root<Cidade> cidade = query.from(Cidade.class);
		
		query.select(criteriaBuilder.count(cidade));
		query.where(adicionarFiltro(filtro, cidade));
		
		return manager.createQuery(query).getSingleResult();
	}

	private Predicate[] adicionarFiltro(CidadeFilter filtro, Root<Cidade> cidade) {
		List<Predicate> predicateList = new ArrayList<>();
		CriteriaBuilder builder = manager.getCriteriaBuilder();

		
		if (filtro != null) {
			if (!StringUtils.isEmpty(filtro.getNome())) {
				predicateList.add(builder.like(cidade.get("nome"), "%" + filtro.getNome() + "%"));
			}
			
			if (filtro.getEstado() != null) {
				predicateList.add(builder.equal(cidade.get("estado"), filtro.getEstado()));
			}
		}
		
		Predicate[] predArray = new Predicate[predicateList.size()];
		return predicateList.toArray(predArray);
	}

}
