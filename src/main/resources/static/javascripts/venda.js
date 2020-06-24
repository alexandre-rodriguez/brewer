Brewer.Venda = (function() {
	
	function Venda(tabelaItens) {
		this.tabelaItens = tabelaItens;
		this.valorTotalBox = $('.js-valor-total-box');
		this.valorFreteInput = $('#valorFrete');
		this.valorDescontoInput = $('#valorDesconto');
		this.valorTotalBoxContainer = $('.js-valor-total-box-container');
		
		this.valorTotalItens = this.tabelaItens.valorTotal() == null ? 0 : parseFloat(this.tabelaItens.valorTotal());
		this.valorFrete = this.valorFreteInput.data('valor') == null ? 0 : parseFloat(this.valorFreteInput.data('valor'));
		this.valorDesconto = this.valorDescontoInput.data('valor') == null ? 0 : parseFloat(this.valorDescontoInput.data('valor'));
	}
	
	Venda.prototype.iniciar = function() {
		this.tabelaItens.on('tabela-itens-atualizada', onTabelaItensAtualizada.bind(this));
		this.valorFreteInput.on('keyup', onValorFreteAlterado.bind(this));
		this.valorDescontoInput.on('keyup', onValorDescontoAlterado.bind(this));
		
		this.tabelaItens.on('tabela-itens-atualizada', onValoresAlterados.bind(this));
		this.valorFreteInput.on('keyup', onValoresAlterados.bind(this));
		this.valorDescontoInput.on('keyup', onValoresAlterados.bind(this));
		
		onValoresAlterados.call(this);
	}
	
	function onTabelaItensAtualizada(evento, valorTotalItens) {
		this.valorTotalItens = valorTotalItens == null ? 0 : parseFloat(valorTotalItens);
	}
	
	function onValorFreteAlterado(evento) {
		this.valorFrete = Brewer.recuperarValor($(evento.target).val());
	}
	
	function onValorDescontoAlterado(evento) {
		this.valorDesconto = Brewer.recuperarValor($(evento.target).val());
	}
	
	function onValoresAlterados() {
		console.log(">>> valorTotalItens : " + this.valorTotalItens);
		console.log(">>> valorFrete : " + this.valorFrete);
		console.log(">>> valorDesconto : " + this.valorDesconto);
		
		var valorTotal = numeral(this.valorTotalItens).value() + numeral(this.valorFrete).value() - numeral(this.valorDesconto).value();
		this.valorTotalBox.html(Brewer.formatarMoeda(valorTotal));
		
		console.log(">>> valorTotal : " + valorTotal);
		
		this.valorTotalBoxContainer.toggleClass('negativo', valorTotal < 0);
		
	}
	
	return Venda;
	
}());

$(function() {
	
	var autocomplete = new Brewer.Autocomplete();
	autocomplete.iniciar();
	
	var tabelaItens = new Brewer.TabelaItens(autocomplete);
	tabelaItens.iniciar();
	
	var venda = new Brewer.Venda(tabelaItens);
	venda.iniciar();
	
});