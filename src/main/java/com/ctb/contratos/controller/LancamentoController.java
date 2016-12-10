package com.ctb.contratos.controller;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ctb.Processo;
import com.ctb.contratos.model.Contratado;
import com.ctb.contratos.model.Contrato;
import com.ctb.contratos.model.Fonte;
import com.ctb.contratos.model.Lancamento;
import com.ctb.contratos.model.TipoAditivo;
import com.ctb.contratos.repository.Contratados;
import com.ctb.contratos.repository.Contratos;
import com.ctb.contratos.repository.Lancamentos;
import com.ctb.contratos.repository.Processos;

@Controller
@RequestMapping("/transparenciactb/lancamentos")

public class LancamentoController {
	

	private static final String CADASTRO_VIEW = "/cadastro/CadastroLancamento"; 
	private static final String LANCAMENTOS_VIEW = "/pesquisa/PesquisaLancamentos";
	private static final String PAGAMENTO_VIEW="/cadastro/Pagamento";
	
	@PersistenceContext
	private EntityManager manager;
	
	@Autowired
	private Lancamentos lancamentos;
	@Autowired
	private Processos processos;
	@Autowired
	private Contratos contratos;
	
	
	@RequestMapping("/novo/{id_contrato}")
	public ModelAndView novo(@PathVariable("id_contrato") Integer Id_contrato)
	{
		ModelAndView mv = new ModelAndView(CADASTRO_VIEW);
		mv.addObject(new Lancamento());
		Contrato c = contratos.findOne(Id_contrato);
		mv.addObject("contrato", c);
		return mv;
	}
	@RequestMapping("/gerar_pagamento/{id_lancamento}")
	public ModelAndView gerar_pagamento(@PathVariable("id_lancamento")Integer Id_lancamento)
	{
		ModelAndView mv = new ModelAndView(PAGAMENTO_VIEW);
	   mv.addObject("lancamento", lancamentos.findOne(Id_lancamento));
	   return mv;
	}
	
	@RequestMapping(value= "/pagar/{id_lancamento}", method=RequestMethod.POST)
	public ModelAndView pagar(Lancamento lancamento, @RequestParam("lancamento_id_processo" )Integer Id_processo)
	{
		ModelAndView mv = new ModelAndView(PAGAMENTO_VIEW);
		Lancamento lancamentobd = lancamentos.findOne(lancamento.getId_lancamento());
		//mv.addObject("lancamento", lancamentobd);
		lancamento.setAditivo_n(lancamentobd.getAditivo_n());
		lancamento.setContrato(lancamentobd.getContrato());
		lancamento.setData(lancamentobd.getData());
		lancamento.setMeses_prorrogacao(lancamentobd.getMeses_prorrogacao());
		lancamento.setNumero_nota_fiscal(lancamentobd.getNumero_nota_fiscal());
	lancamento.setObservacao(lancamentobd.getObservacao());
	lancamento.setPossui_aditivo(lancamentobd.getPossui_aditivo());
	lancamento.setSaldo_contrato(lancamentobd.getSaldo_contrato());
	lancamento.setTipoAditivo(lancamentobd.getTipoAditivo());
	lancamento.setValor(lancamentobd.getValor());
	lancamento.setValor_aditivo(lancamentobd.getValor_aditivo());
	lancamento.setProcesso(processos.findOne(Id_processo));
	lancamento.setLiquidado(true);

		lancamentos.save(lancamento);
		mv.addObject("mensagem", "Pagamento registrado com sucesso!");
		return mv;
	}
	

	
	@RequestMapping(method = RequestMethod.POST)
	public String salvar(Lancamento lancamento, @RequestParam Integer lancamento_id_contrato, RedirectAttributes attributes)
	{
		ModelAndView mv = new ModelAndView(CADASTRO_VIEW);
		Contrato c = contratos.findOne(lancamento_id_contrato);
		lancamento.setLiquidado(false);
		/*
		if(lancamento_id_processo != null)
		{
			Processo processo = processos.findOne(lancamento_id_processo);
			lancamento.setProcesso(processo);
		}
		*/
		//System.out.println(lancamento_id_contrato);
	
		if(lancamento_id_contrato != null)
		{
			Contrato contrato = contratos.findOne(lancamento_id_contrato);
			//contrato.setSaldo_contrato(contrato.getSaldo_contrato() - lancamento.getValor());
			lancamento.setContrato(contrato);
			contratos.save(contrato);
		}
		if(lancamento.getPossui_aditivo() && lancamento.getMeses_prorrogacao() != null)
		{
			Calendar cal = Calendar.getInstance();
			cal.setTime(lancamento.getContrato().getData_vencimento());
			cal.add(Calendar.MONTH, lancamento.getMeses_prorrogacao());
	
			
			
			c.setDuracao_meses(c.getDuracao_meses()+ lancamento.getMeses_prorrogacao());
			c.setMeses_vencimento(c.getMeses_vencimento()+ lancamento.getMeses_prorrogacao());
			c.setData_vencimento(cal.getTime());
			
		}
		BigDecimal resultop1 = new BigDecimal(c.getSaldo_contrato().toString());
		BigDecimal resultop2 = new BigDecimal("0");
		if(lancamento.getValor_aditivo() != null)
		{
			resultop2 = new BigDecimal(lancamento.getValor_aditivo().toString());
		}
		BigDecimal resultop3 = new BigDecimal(lancamento.getValor().toString());
		BigDecimal  resultop4 = resultop1.add(resultop2);
		BigDecimal resultop5 = resultop4.subtract(resultop3);
		lancamento.setSaldo_contrato(resultop5);
		c.setSaldo_contrato(resultop5 );
		
		contratos.save(c);
		lancamentos.save(lancamento);		
		attributes.addFlashAttribute("mensagem", "Lancamento salvo com sucesso!");	
		return "redirect:/transparenciactb/lancamentos/pesquisar/" + lancamento.getContrato().getId_contrato();
	}

	
	@ModelAttribute("todosProcessos")
	public List<Processo> todosProcessos()
	{
		return processos.findAll();
	}
	
	
	
	@RequestMapping(value="/pesquisar/{id_contrato}")
	public ModelAndView pesquisar(@PathVariable("id_contrato") Integer Id_contrato, String busca, String nome, String setor, @PageableDefault(size=15) Pageable pageable) throws ParseException
	{
		ModelAndView mv = new ModelAndView("/pesquisa/PesquisaLancamentos");
		Contrato c = contratos.findOne(Id_contrato);
		Criteria criteria = manager.unwrap(Session.class).createCriteria(Lancamento.class);
		int paginaAtual = pageable.getPageNumber();
		int totalRegistrosPorPagina = pageable.getPageSize();
		int primeiroRegistro = paginaAtual * totalRegistrosPorPagina;
		
		
		criteria.setFirstResult(primeiroRegistro);
        criteria.setMaxResults(totalRegistrosPorPagina);
    	criteria.add(Restrictions.eq("contrato", c));
        
        Comparator<Lancamento> cmp = new Comparator<Lancamento>() {
	        public int compare(Lancamento l1, Lancamento l2) {
	          return l2.getData().compareTo(l1.getData());
	        }
	    };
List<Lancamento> lancamentosOrdenados = criteria.list();
lancamentosOrdenados.sort(cmp);
		//criteria.add(Restrictions.eq("contrato", c));
		
		List<Lancamento> lanc= c.getLancamentos();
		Page<Lancamento> pags = new PageImpl<Lancamento>(lancamentosOrdenados, pageable, lanc.size());
		//Page<Lancamento> lancamentos = criteria.list();
		System.out.println(criteria);
		//c.getLancamentos();
		mv.addObject("todosLancamentos",pags);
		mv.addObject("contrato", c);
	
	return mv;
	}
	
	@RequestMapping("/{id_contrato}/{id_lancamento}")
	public ModelAndView edicao(@PathVariable("id_lancamento") Lancamento lancamento, @PathVariable("id_contrato") Contrato contrato)
	{
		
		
		ModelAndView mv = new ModelAndView(CADASTRO_VIEW);
		mv.addObject("lancamento", lancamento);
		mv.addObject(lancamento);
		mv.addObject("contrato", contrato);
		return mv;
	}
	
	@ModelAttribute("todosContratos")
	public List<Contrato> todosContratos()
	{
		return contratos.findAll();
	}
	
	@RequestMapping(value="/remove/{id_lancamento}")
	public String excluir(@PathVariable Integer id_lancamento, RedirectAttributes attributes)
	{
		Lancamento lancamento = lancamentos.findOne(id_lancamento);
		//attributes.addFlashAttribute("mensagem", "Contrato excluído com sucesso com sucesso!");	
		Contrato c = contratos.findOne(lancamento.getContrato().getId_contrato());
		BigDecimal resultop1 = new BigDecimal(c.getSaldo_contrato().toString());
		BigDecimal resultop2 = new BigDecimal(lancamento.getValor().toString());
		BigDecimal resultop3 = new BigDecimal(lancamento.getValor_aditivo().toString());
		c.setSaldo_contrato(resultop1.add(resultop2));
		if(lancamento.getPossui_aditivo() == true)
		{
			
			c.setSaldo_contrato(resultop1.subtract(resultop3));
			//c.setSaldo_contrato(c.getSaldo_contrato() - lancamento.getValor_aditivo());
			
		}
		contratos.save(c);
		//usuarios.delete(id_usuario);
		//desvincularLancamento(lancamento);
		lancamentos.delete(id_lancamento);
		return "redirect:/transparenciactb/lancamentos/pesquisar/"+ lancamento.getContrato().getId_contrato();	
	
	}
	
public void desvincularLancamento(Lancamento lancamento)
	
	{
	
	if(lancamento.getContrato() != null)
	{
		desvincularContratoLancamento(lancamento.getContrato());
	}
	
	
	
	/*
	Iterator it = contratosLancamentos.iterator();
	
	while(it.hasNext())
	{
		Lancamento obj = (Lancamento) it.next();
		System.out.println("Entrou aqui nesse laço");
		System.out.println(obj.getContrato().getNumero());
		if(obj.getContrato().getNumero() == contrato.getNumero()) {
			obj.setContrato(null);
			lancamentos.save(obj);
			}
		}
		*/
		
	}

public void desvincularContratoLancamento(Contrato contrato)
{
	List<Lancamento> contratosLancamentos = contrato.getLancamentos();
	System.out.println(contratosLancamentos);
	//System.out.println(contratosLancamentos.size());
	if(contratosLancamentos != null) {
		Iterator it = contratosLancamentos.iterator();
	
	while(it.hasNext())
	{
		Lancamento obj = (Lancamento) it.next();
		System.out.println("Entrou aqui nesse laço");
		System.out.println(obj.getContrato().getNumero());
		if(obj.getContrato().getNumero() == contrato.getNumero()) {
			obj.setContrato(null);
			lancamentos.save(obj);
			}
		}
	}
	
}

@ModelAttribute("todosTipos")
public List<TipoAditivo> todosAditivos() {
	return Arrays.asList(TipoAditivo.values());
}

private Long total(Criteria criteria) {
	criteria.setProjection(Projections.rowCount());
	return (Long) criteria.uniqueResult();
}

}
