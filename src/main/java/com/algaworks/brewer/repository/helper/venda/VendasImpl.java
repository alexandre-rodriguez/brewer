package com.algaworks.brewer.repository.helper.venda;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.Year;
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.algaworks.brewer.dto.VendaMes;
import com.algaworks.brewer.dto.VendaOrigem;
import com.algaworks.brewer.model.StatusVenda;
import com.algaworks.brewer.model.TipoPessoa;
import com.algaworks.brewer.model.Venda;
import com.algaworks.brewer.repository.filter.VendaFilter;
import com.algaworks.brewer.repository.paginacao.PaginacaoUtil;

public class VendasImpl implements VendasQueries {

	@PersistenceContext
	private EntityManager manager;
	
	@Autowired
	private PaginacaoUtil paginacaoUtil;
	
	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true)
	@Override
	public Page<Venda> filtrar(VendaFilter filtro, Pageable pageable) {
		CriteriaBuilder builder = manager.getCriteriaBuilder();
		CriteriaQuery<Venda> query = builder.createQuery(Venda.class);
		Root<Venda> venda = query.from(Venda.class);
	
		query.select(venda);
		query.where(adicionarFiltro(filtro, venda));
		
		TypedQuery<Venda> typedQuery =  (TypedQuery<Venda>) paginacaoUtil.preparar(query, venda, pageable);
		
		return new PageImpl<>(typedQuery.getResultList(), pageable, total(filtro));
	}
	
	@Transactional(readOnly = true)
	@Override
	public Venda buscarComItens(Long codigo) {
		CriteriaBuilder builder = manager.getCriteriaBuilder();
		CriteriaQuery<Venda> query = builder.createQuery(Venda.class);
		Root<Venda> venda = query.from(Venda.class);
		
		venda.fetch("itens", JoinType.LEFT);
		
		query.select(venda);
		query.distinct(true);
		query.where(builder.equal(venda.get("codigo"), codigo));
		
		TypedQuery<Venda> typedQuery = manager.createQuery(query);
	
		return typedQuery.getSingleResult();
	}
	
	@Override
	public BigDecimal valorTotalNoAno() {
		Optional<BigDecimal> optional = Optional.ofNullable(
				manager.createQuery("select sum(valorTotal) from Venda where year(dataCriacao) = :ano and status = :status", BigDecimal.class)
					.setParameter("ano", Year.now().getValue())
					.setParameter("status", StatusVenda.EMITIDA)
					.getSingleResult());
		
		return optional.orElse(BigDecimal.ZERO);
	}
	
	@Override
	public BigDecimal valorTotalNoMes() {
		Optional<BigDecimal> optional = Optional.ofNullable(
				manager.createQuery("select sum(valorTotal) from Venda where month(dataCriacao) = :mes and status = :status", BigDecimal.class)
					.setParameter("mes", MonthDay.now().getMonthValue())
					.setParameter("status", StatusVenda.EMITIDA)
					.getSingleResult());
		
		return optional.orElse(BigDecimal.ZERO);
	}
	
	@Override
	public BigDecimal valorTicketMedioNoAno() {
		Optional<BigDecimal> optional = Optional.ofNullable(
				manager.createQuery("select sum(valorTotal)/count(*) from Venda where year(dataCriacao) = :ano and status = :status", BigDecimal.class)
					.setParameter("ano", Year.now().getValue())
					.setParameter("status", StatusVenda.EMITIDA)
					.getSingleResult());
		
		return optional.orElse(BigDecimal.ZERO);
	}
	
	@Override
	public List<VendaMes> totalPorMes() {
		List<VendaMes> vendasMes = manager.createNamedQuery("Vendas.totalPorMes", VendaMes.class).getResultList();
		
		LocalDate hoje = LocalDate.now();
		for (int i = 1; i <= 6; i++) {
			String mesIdeal = String.format("%d/%02d", hoje.getYear(), hoje.getMonthValue());
			
			boolean possuiMes = vendasMes.stream().filter(v -> v.getMes().equals(mesIdeal)).findAny().isPresent();
			
			if (!possuiMes) {
				vendasMes.add(i - 1, new VendaMes(mesIdeal, 0));
			}
			
			hoje = hoje.minusMonths(1);
		}
		
		
		return vendasMes;
	}
	
	@Override
	public List<VendaOrigem> totalPorOrigem() {
		List<VendaOrigem> vendasNacionalidade = manager.createNamedQuery("Vendas.porOrigem", VendaOrigem.class).getResultList();
		
		LocalDate now = LocalDate.now();
		for (int i = 1; i <= 6; i++) {
			String mesIdeal = String.format("%d/%02d", now.getYear(), now.getMonth().getValue());
			
			boolean possuiMes = vendasNacionalidade.stream().filter(v -> v.getMes().equals(mesIdeal)).findAny().isPresent();
			if (!possuiMes) {
				vendasNacionalidade.add(i - 1, new VendaOrigem(mesIdeal, 0, 0));
			}
			
			now = now.minusMonths(1);
		}
		
		return vendasNacionalidade;
	}
	
	private Long total(VendaFilter filtro) {
		CriteriaBuilder criteriaBuilder = manager.getCriteriaBuilder();
		CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
		Root<Venda> venda = query.from(Venda.class);
		
		query.select(criteriaBuilder.count(venda));
		query.where(adicionarFiltro(filtro, venda));
		
		return manager.createQuery(query).getSingleResult();
	}
	
	private Predicate[] adicionarFiltro(VendaFilter filtro, Root<Venda> venda) {
		List<Predicate> predicateList = new ArrayList<>();
		CriteriaBuilder builder = manager.getCriteriaBuilder();
		
		if (filtro != null) {
			if (!StringUtils.isEmpty(filtro.getCodigo())) {
				predicateList.add(builder.equal(venda.get("codigo"), filtro.getCodigo()));
			}
			
			if (filtro.getStatus() != null) {
				predicateList.add(builder.equal(venda.get("status"), filtro.getStatus()));
			}
			
			if (filtro.getDesde() != null) {
				LocalDateTime desde = LocalDateTime.of(filtro.getDesde(), LocalTime.of(0, 0));
				predicateList.add(builder.greaterThanOrEqualTo(venda.get("dataCriacao"), desde));
			}
			
			if (filtro.getAte() != null) {
				LocalDateTime ate = LocalDateTime.of(filtro.getAte(), LocalTime.of(23, 59));
				predicateList.add(builder.lessThanOrEqualTo(venda.get("dataCriacao"), ate));
			}
			
			if (filtro.getValorMinimo() != null) {
				predicateList.add(builder.ge(venda.get("valorTotal"), filtro.getValorMinimo()));
			}
			
			if (filtro.getValorMaximo() != null) {
				predicateList.add(builder.le(venda.get("valorTotal"), filtro.getValorMaximo()));
			}
			
			if (!StringUtils.isEmpty(filtro.getNomeCliente())) {
				predicateList.add(builder.like(venda.get("cliente").get("nome"), "%" + filtro.getNomeCliente() + "%"));
			}
			
			if (!StringUtils.isEmpty(filtro.getCpfOuCnpjCliente())) {
				predicateList.add(builder.equal(venda.get("cliente").get("cpfOuCnpj"), TipoPessoa.removerFormatacao(filtro.getCpfOuCnpjCliente())));
			}
			
		}
		
		Predicate[] predArray = new Predicate[predicateList.size()];
		return predicateList.toArray(predArray);
	}

}