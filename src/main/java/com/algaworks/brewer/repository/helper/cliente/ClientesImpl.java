package com.algaworks.brewer.repository.helper.cliente;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.algaworks.brewer.model.Cliente;
import com.algaworks.brewer.repository.filter.ClienteFilter;
import com.algaworks.brewer.repository.paginacao.PaginacaoUtil;

public class ClientesImpl implements ClientesQueries {

	@PersistenceContext
	private EntityManager manager;
	
	@Autowired
	private PaginacaoUtil paginacaoUtil;
	
	@SuppressWarnings("unchecked")
	@Override
	@Transactional(readOnly = true)
	public Page<Cliente> filtrar(ClienteFilter filtro, Pageable pageable) {
		CriteriaBuilder builder = manager.getCriteriaBuilder();
		CriteriaQuery<Cliente> query = builder.createQuery(Cliente.class);
		Root<Cliente> cliente = query.from(Cliente.class);
		
		cliente.fetch("endereco", JoinType.LEFT)
			.fetch("cidade", JoinType.LEFT)
			.fetch("estado", JoinType.LEFT);
		
		query.select(cliente);
		query.where(adicionarFiltro(filtro, cliente));
		
		TypedQuery<Cliente> typedQuery =  (TypedQuery<Cliente>) paginacaoUtil.preparar(query, cliente, pageable);
		
		return new PageImpl<>(typedQuery.getResultList(), pageable, total(filtro));
	}
	
	private Long total(ClienteFilter filtro) {
		CriteriaBuilder criteriaBuilder = manager.getCriteriaBuilder();
		CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
		Root<Cliente> cliente = query.from(Cliente.class);
		
		query.select(criteriaBuilder.count(cliente));
		query.where(adicionarFiltro(filtro, cliente));
		
		return manager.createQuery(query).getSingleResult();
	}

	private Predicate[] adicionarFiltro(ClienteFilter filtro, Root<Cliente> cliente) {
		List<Predicate> predicateList = new ArrayList<>();
		CriteriaBuilder builder = manager.getCriteriaBuilder();

		
		if (filtro != null) {
			if (!StringUtils.isEmpty(filtro.getNome())) {
				predicateList.add(builder.like(cliente.get("nome"), "%" + filtro.getNome() + "%"));
			}
			
			if (!StringUtils.isEmpty(filtro.getCpfOuCnpj())) {
				predicateList.add(builder.equal(cliente.get("cpfOuCnpj"), filtro.getCpfOuCnpjSemFormatacao()));
			}
		}
		
		Predicate[] predArray = new Predicate[predicateList.size()];
		return predicateList.toArray(predArray);
	}

}
