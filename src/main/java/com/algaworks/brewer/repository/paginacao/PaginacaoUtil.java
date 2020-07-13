package com.algaworks.brewer.repository.paginacao;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Root;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class PaginacaoUtil {
	
	@PersistenceContext
	private EntityManager manager;
	
	public TypedQuery<?> preparar(CriteriaQuery<?> query, Root<?> fromEntity, Pageable pageable) {
		int paginaAtual = pageable.getPageNumber();
		int totalRegistrosPorPagina = pageable.getPageSize();
		int primeiroRegistro = paginaAtual * totalRegistrosPorPagina;
		
		CriteriaBuilder builder = manager.getCriteriaBuilder();
		
		Sort sort = pageable.getSort();
		if (sort != null && sort.isSorted()) {
			
			Sort.Order sortOrder = sort.iterator().next();
			String property = sortOrder.getProperty();

			Order order = sortOrder.isAscending() ? builder.asc(fromEntity.get(property))
					: builder.desc(fromEntity.get(property));
			query.orderBy(order);
		}

		TypedQuery<?> typedQuery = manager.createQuery(query);
		
		typedQuery.setFirstResult(primeiroRegistro);
		typedQuery.setMaxResults(totalRegistrosPorPagina);	
		
		return typedQuery;
	}
}
