package com.algaworks.brewer.repository.helper.usuario;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.algaworks.brewer.model.Grupo;
import com.algaworks.brewer.model.Usuario;
import com.algaworks.brewer.model.UsuarioGrupo;
import com.algaworks.brewer.repository.filter.UsuarioFilter;
import com.algaworks.brewer.repository.paginacao.PaginacaoUtil;

public class UsuariosImpl implements UsuariosQueries {

	@PersistenceContext
	private EntityManager manager;
	
	@Autowired
	private PaginacaoUtil paginacaoUtil;
	
	@Override
	public Optional<Usuario> porEmailEAtivo(String email) {
		return manager
				.createQuery("from Usuario where lower(email) = lower(:email) and ativo = true", Usuario.class)
				.setParameter("email", email)
				.getResultList().stream().findFirst();
	}

	@Override
	public List<String> permissoes(Usuario usuario) {
		return manager
				.createQuery("Select distinct p.nome from Usuario u inner join u.grupos g inner join g.permissoes p where u = :usuario", String.class)
				.setParameter("usuario", usuario)
				.getResultList();
	}

	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true)
	@Override
	public Page<Usuario> filtrar(UsuarioFilter filtro, Pageable pageable) {
		CriteriaBuilder builder = manager.getCriteriaBuilder();
		CriteriaQuery<Usuario> query = builder.createQuery(Usuario.class);
		Root<Usuario> usuario = query.from(Usuario.class);
		
		query.select(usuario);
		query.where(adicionarFiltro(filtro, query, usuario));
		
		TypedQuery<Usuario> typedQuery =  (TypedQuery<Usuario>) paginacaoUtil.preparar(query, usuario, pageable);
		
		List<Usuario> filtrados = typedQuery.getResultList();
		
		filtrados.forEach(u -> Hibernate.initialize(u.getGrupos()));
		
		return new PageImpl<>(filtrados, pageable, total(filtro));
	}
	
	@Transactional(readOnly = true)
	@Override
	public Usuario buscarComGrupos(Long codigo) {
		CriteriaBuilder builder = manager.getCriteriaBuilder();
		CriteriaQuery<Usuario> query = builder.createQuery(Usuario.class);
		Root<Usuario> usuario = query.from(Usuario.class);
		
		usuario.fetch("grupos", JoinType.LEFT);
		
		query.select(usuario);
		query.distinct(true);
		query.where(builder.equal(usuario.get("codigo"), codigo));
		
		TypedQuery<Usuario> typedQuery = manager.createQuery(query);
	
		return typedQuery.getSingleResult();	
	}
	
	private Long total(UsuarioFilter filtro) {
		CriteriaBuilder criteriaBuilder = manager.getCriteriaBuilder();
		CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
		Root<Usuario> usuario = query.from(Usuario.class);
		
		query.select(criteriaBuilder.count(usuario));
		query.where(adicionarFiltro(filtro, query, usuario));
		
		return manager.createQuery(query).getSingleResult();
	}

	private Predicate[] adicionarFiltro(UsuarioFilter filtro, CriteriaQuery<?> query, Root<Usuario> usuario) {
		List<Predicate> predicateList = new ArrayList<>();
		CriteriaBuilder builder = manager.getCriteriaBuilder();

		
		if (filtro != null) {
			if (!StringUtils.isEmpty(filtro.getNome())) {
				predicateList.add(builder.like(usuario.get("nome"), "%" + filtro.getNome() + "%"));
			}
			
			if (!StringUtils.isEmpty(filtro.getEmail())) {
				predicateList.add(builder.like(usuario.get("email"), filtro.getEmail() + "%"));
			}
			
			if(filtro.getGrupos() != null && !filtro.getGrupos().isEmpty() ) {
				
				for(Long codigoGrupo : filtro.getGrupos().stream().mapToLong(Grupo::getCodigo).toArray()) {
					Subquery<Integer> subquery = query.subquery(Integer.class);
					Root<UsuarioGrupo> subqueryRoot = subquery.from(UsuarioGrupo.class);
					
					subquery.select(subqueryRoot.get("id").get("usuario").get("codigo"));
					subquery.where(builder.equal(subqueryRoot.get("id").get("grupo").get("codigo"), codigoGrupo));
					
					predicateList.add(usuario.get("codigo").in(subquery));
				}
			}
			
		}
		
		Predicate[] predArray = new Predicate[predicateList.size()];
		return predicateList.toArray(predArray);
	}
	
	

}
