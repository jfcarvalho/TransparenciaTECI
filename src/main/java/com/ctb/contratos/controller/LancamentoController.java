package com.ctb.contratos.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Months;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ctb.Mailer;
import com.ctb.Processo;
import com.ctb.TipoProcesso;
import com.ctb.contratos.model.Contratado;
import com.ctb.contratos.model.Contrato;
import com.ctb.contratos.model.Fonte;
import com.ctb.contratos.model.Lancamento;
import com.ctb.contratos.model.TipoAditivo;
import com.ctb.contratos.model.Usuario;
import com.ctb.contratos.repository.Contratados;
import com.ctb.contratos.repository.Contratos;
import com.ctb.contratos.repository.Lancamentos;
import com.ctb.contratos.repository.LancamentosQueries;
import com.ctb.contratos.repository.Processos;
import com.ctb.contratos.repository.Usuarios;
import com.ctb.security.AppUserDetailsService;

@Controller
@RequestMapping("/transparenciactb/lancamentos")

public class LancamentoController {
	

	private static final String CADASTRO_VIEW = "/cadastro/CadastroLancamento"; 
	private static final String LANCAMENTOS_VIEW = "/pesquisa/PesquisaLancamentos";
	private static final String PAGAMENTO_VIEW="/cadastro/Pagamento";
	private static final String EDICAO_VIEW="/edicao/EdicaoLancamento";
	
	
	@PersistenceContext
	private EntityManager manager;
	
	@Autowired
	private Lancamentos lancamentos;

	private LancamentosQueries lancamentosQ;
	@Autowired
	private Processos processos;
	@Autowired
	private Contratos contratos;
	@Autowired
	private Usuarios usuarios;
	@Autowired
	private Mailer mailer;
	
	
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
	   Lancamento l = lancamentos.findOne(Id_lancamento);
	   Processo p = l.getProcesso();
	   mv.addObject("lancamento", l);
	   mv.addObject("processo", p);
	   
	   return mv;	
	}
	
	public void checaGastoContrato(Contrato contrato, BigDecimal gastomedio, BigDecimal valorgasto)
	{
		DateTime inicio = new DateTime();
		DateTime fim = new DateTime(contrato.getData_vencimento());
		Days d = Days.daysBetween(inicio, fim);
		Integer dias  = d.getDays();
		BigDecimal valorEstimado = new BigDecimal("0");
		if(dias <= 90 && contrato.getRiscofinanceiro() == false)
		{
			Months m = Months.monthsBetween(inicio, fim);
			BigDecimal meses = new BigDecimal (Integer.toString(m.getMonths()));
			valorEstimado = contrato.getSaldo_contrato().divide(meses);
			int comp = valorgasto.compareTo(gastomedio);
			int comp2 = valorgasto.compareTo(valorEstimado);
			if(comp == 1 && comp2 ==1) //Ou seja se o valor gasto estiver acima do valor médio e for maior que o valorEstimado
			{
				contrato.setRiscofinanceiro(true);
			}
		}
	}
	
	public BigDecimal calcularAditivos(List<Lancamento> lancamentos)
	{
		BigDecimal acumulador =  BigDecimal.ZERO;
		Iterator it = lancamentos.iterator();
		
		while(it.hasNext())
		{
			Lancamento obj = (Lancamento) it.next();
			if(obj.getPossui_aditivo())
			{
				acumulador = acumulador.add(obj.getValor_aditivo()); 
			}
			
		}
		return acumulador;
	}

	
	@RequestMapping(value= "/pagar/{id_lancamento}", method=RequestMethod.POST)
	public ModelAndView pagar(Lancamento lancamento, @RequestParam @DateTimeFormat(pattern= "dd/MM/yyyy") Date data_pagamento, @RequestParam boolean pago )
	{
		ModelAndView mv = new ModelAndView(PAGAMENTO_VIEW);
		Lancamento lancbd = lancamentos.findOne(lancamento.getId_lancamento());
		lancbd.setLiquidado(true);
		lancbd.setDataliquidacao(lancamento.getDataliquidacao());
		Processo p = lancbd.getProcesso();
		p.setData_pagamento(data_pagamento);
		p.setPago(pago);
		processos.save(p);
		lancamentos.save(lancbd);
		mv.addObject(lancbd);
		mv.addObject("mensagem", "Pagamento registrado com sucesso!");
		mailer.enviar_pagamento_contrato(lancbd, p, lancbd.getContrato().getGestor().getEmail());
		return mv;
		
		
	}
	@RequestMapping(value= "/aditivo/{id_lancamento}", method=RequestMethod.POST)
	public ModelAndView aditivo(Lancamento lancamento, @RequestParam("lancamento_id_processo" )Integer Id_processo, @RequestParam Integer lancamento_id_processo)
	{
		ModelAndView mv = new ModelAndView(PAGAMENTO_VIEW);
		Lancamento lancamentobd = lancamentos.findOne(lancamento.getId_lancamento());
		lancamento.setAditivo_n(lancamentobd.getAditivo_n());
		lancamento.setContrato(lancamentobd.getContrato());
		lancamento.setData(lancamentobd.getData());
		lancamento.setMeses_prorrogacao(lancamentobd.getMeses_prorrogacao());
		lancamento.setNumero_nota_fiscal(lancamentobd.getNumero_nota_fiscal());
		lancamento.setObservacao(lancamentobd.getObservacao());
		lancamento.setPossui_aditivo(lancamentobd.getPossui_aditivo());
		lancamento.setSaldo_contrato(lancamentobd.getSaldo_contrato());
		lancamento.setHora(lancamentobd.getHora());
		lancamento.setMedicao(lancamentobd.getMedicao());
		lancamento.setTipoAditivo(lancamentobd.getTipoAditivo());
		lancamento.setValor(lancamentobd.getValor());
		lancamento.setValor_aditivo(lancamentobd.getValor_aditivo());
		lancamento.setDoe_aditivo(lancamentobd.getDoe_aditivo());
		lancamento.setCompetencia(lancamentobd.getCompetencia());
		Processo p = processos.findOne(Id_processo);
		
		lancamento.setProcesso(p);
		p.setLancamento(lancamento);
		processos.save(p);
		lancamento.setLiquidado(true);

		lancamentos.save(lancamento);
		mv.addObject(lancamento);
		
		mv.addObject("mensagem", "Pagamento registrado com sucesso!");
		return mv;
	}
	

	
	@RequestMapping(method = RequestMethod.POST)
	public String salvar(Lancamento lancamento, @RequestParam Integer lancamento_id_contrato, String tipo_processo ,String numero_processo , String ano_processo, 
	@DateTimeFormat(pattern= "dd/MM/yyyy") Date data_processo,RedirectAttributes attributes)
	{
		//ModelAndView mv = new ModelAndView(CADASTRO_VIEW);
		Contrato c = contratos.findOne(lancamento_id_contrato);
		//mv.addObject("contrato",c);
		lancamento.setLiquidado(false);
		
		DateTime inicio = new DateTime();
		DateTime fim = new DateTime(c.getData_vencimento());
		Months m = Months.monthsBetween(inicio, fim);
		Days d = Days.daysBetween(inicio, fim);
		Processo p = new Processo();
		int tproc = processos.findAll().size();
		tproc++;
		p.setId_processo(tproc);
		String numero_completo = numero_processo + "/" + ano_processo;
		p.setNumero_processo(numero_completo);
		p.setData_abertura(data_processo);
		switch(tipo_processo)
		{
			case "Pagamento":
				p.setTipo_processo(TipoProcesso.Pagamento);
			break;
			case "Renovacao":
				p.setTipo_processo(TipoProcesso.Renovacao);
				c.setProcesso(p);
				
			break;
		}
	//	Usuario user = usuarios.findByEmail(AppUserDetailsService.cusuario.getUsername());
		Usuario user = new Usuario();
		Object usuarioLogado = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		user = usuarios.findByEmail(((UserDetails) usuarioLogado).getUsername());
		p.setUsuario(user);
		processos.save(p);
		lancamento.setProcesso(p);
		
		//if( d.getDays() >= 0 ) { 
			if(lancamento_id_contrato != null)
			{
				Contrato contrato = contratos.findOne(lancamento_id_contrato);
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
			BigDecimal resultop3 = new BigDecimal(BigDecimal.ZERO.toString());
			if(lancamento.getValor() != null)
			{
				resultop3 = new BigDecimal(lancamento.getValor().toString());
					
			}
			else{lancamento.setValor(BigDecimal.ZERO);
				 }
		
			
		
			BigDecimal  resultop4 = resultop1.add(resultop2);
			BigDecimal resultop5 = resultop4.subtract(resultop3);
			lancamento.setSaldo_contrato(resultop5);
			c.setSaldo_contrato(resultop5 );
			
			c.setUltima_atualizacao(inicio.toDate());
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
			Date hora = Calendar.getInstance().getTime(); // Ou qualquer outra forma que tem
			String datas[] = lancamento.getData().toString().split(" ");
			String ano = datas[5];
			lancamento.setCompetencia(lancamento.getCompetencia()+"/"+ ano);
			String dataFormatada = sdf.format(hora);
			lancamento.setHora(dataFormatada);
			mailer.enviar_lancamento_gestor(lancamento,"anderson.araujo@ctb.ba.gov.br");
			contratos.save(c);
			lancamentos.save(lancamento);
			return "redirect:/transparenciactb/lancamentos/pesquisar/" +lancamento.getContrato().getId_contrato();
		//	mv.addObject("mensagem", "Lancamento salvo com sucesso!");
		//	return mv;
	//	}
	/*	else {
				  mv.addObject("vencido", true);
				  mv.addObject("mensagem", "Este contrato está vencido!, não será mais possivel fazer lancamentos nele");
				  return mv;
		}
		*/
	}

	
	@ModelAttribute("todosProcessos")
	public List<Processo> todosProcessos()
	{
		List <Processo> p = processos.findAll();
		List <Processo> np = new ArrayList<Processo>();
		for(Processo proc:p)
		{
			if(proc.getTipo_processo().getTipo().equals("Pagamento") && proc.getLancamento() == null)
			{
				np.add(proc);
			}
		}
		return np;
	}
	
	@ModelAttribute("todosProcessosUsuario")
	public List<Processo> todosProcessosUsuario()
	{
		List <Processo> np = new ArrayList<Processo>();
		//Usuario user = usuarios.findByEmail(AppUserDetailsService.cusuario.getUsername());
		Usuario user = new Usuario();
		Object usuarioLogado = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		user = usuarios.findByEmail(((UserDetails) usuarioLogado).getUsername());
		
		if(user.getProcessos() != null) {
		for(Processo proc:user.getProcessos())
		{
			if(proc.getTipo_processo().getTipo().equals("Pagamento") && proc.getLancamento() == null)
			{
				np.add(proc);
			}
			}
		}
		return np;
	}
	
	public BigDecimal gastoMedio(Contrato contrato)
	{
		BigDecimal valor = new BigDecimal("0");
		Integer quantidadeLancamentos = 0;
		
		for(Lancamento l:contrato.getLancamentos())
			{
				valor = valor.add(l.getValor());
				quantidadeLancamentos++;
			}
		
	if(contrato.getLancamentos().size() > 0) {
		BigDecimal divisor = new BigDecimal(quantidadeLancamentos.toString());
		valor = valor.divide(divisor, RoundingMode.HALF_UP);
		return valor;
	}
	return BigDecimal.ZERO;
	}
	
	
	
	@RequestMapping(value="/pesquisar/{id_contrato}")
	public ModelAndView pesquisar(@PathVariable("id_contrato") Integer Id_contrato, String busca, String numero, String data, @PageableDefault(size=10) Pageable pageable) throws ParseException
	{
		ModelAndView mv = new ModelAndView("/pesquisa/PesquisaLancamentos");
		Contrato c = contratos.findOne(Id_contrato);
		mv.addObject("contrato", c);
		Criteria criteria = manager.unwrap(Session.class).createCriteria(Lancamento.class);
		int paginaAtual = pageable.getPageNumber();
		int totalRegistrosPorPagina = pageable.getPageSize();
		int primeiroRegistro = paginaAtual * totalRegistrosPorPagina;
		//Usuario us = usuarios.findByEmail(AppUserDetailsService.cusuario.getUsername());
		Usuario us = new Usuario();
		Object usuarioLogado = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		us = usuarios.findByEmail(((UserDetails) usuarioLogado).getUsername());
		
		criteria.setFirstResult(primeiroRegistro);
        criteria.setMaxResults(totalRegistrosPorPagina);
    	criteria.add(Restrictions.eq("contrato", c));
    	criteria.addOrder(Order.desc("data"));
    	if(numero != null) {
			if(busca != null && numero.equals("on")) {
				Contrato contratos_usuario = contratos.findOne(Id_contrato);
				List<Lancamento> lancamentos_limitados = new ArrayList<Lancamento>();
					for(Lancamento l: contratos_usuario.getLancamentos())
					{
						
						if(l.getNumero_nota_fiscal() != null) {
							if(l.getNumero_nota_fiscal().contains(busca))
								
							{
								lancamentos_limitados.add(l);
							}
						}
					}
				
				mv.addObject("todosLancamentos", lancamentos_limitados);
				
				
				return mv;
			}
		}
    	
		else if(data != null) {
			if(busca != null && data.equals("on")) {
				Contrato contratos_usuario = contratos.findOne(Id_contrato);
				List<Lancamento> lancamentos_limitados = new ArrayList<Lancamento>();
					for(Lancamento l: contratos_usuario.getLancamentos())
					{
					
						if(l.getData() != null) {
							if(l.getData().toString().contains(busca))
								
							{
								lancamentos_limitados.add(l);
							}
						}
					}
				
				mv.addObject("todosLancamentos", lancamentos_limitados);
				return mv;
			}
		}
        
        Comparator<Lancamento> cmp = new Comparator<Lancamento>() {
	        public int compare(Lancamento l1, Lancamento l2) {
	          return l2.getData().compareTo(l1.getData());
	        }
	    };
	    
	    Comparator<Lancamento> cmp2 = new Comparator<Lancamento>() {
	        public int compare(Lancamento l1, Lancamento l2) {
	          return l1.getId_lancamento().compareTo(l2.getId_lancamento());
	        }
	    };

	    List<Lancamento> lancamentosOrdenados = criteria.list();
	
		Page<Lancamento> pags = new PageImpl<Lancamento>(lancamentosOrdenados, pageable, lancamentosOrdenados.size());
	
	
		mv.addObject("todosLancamentos",pags);
		mv.addObject("contrato", c);
		return mv;
	}
	
	@RequestMapping("/gerar_edicao_lancamento/{id_lancamento}")
	public ModelAndView gerar_edicao(@PathVariable("id_lancamento") Lancamento lancamento)
	{
		ModelAndView mv = new ModelAndView(EDICAO_VIEW);
		mv.addObject(new LancamentoController());
		Lancamento lanc = lancamentos.findOne(lancamento.getId_lancamento());
		Contrato c = contratos.findOne(lanc.getContrato().getId_contrato());
		Processo p = processos.findOne(lancamento.getProcesso().getId_processo());
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		if(lancamento.getPossui_aditivo() && lancamento.getMeses_prorrogacao() != null)
		{
			Calendar cal = Calendar.getInstance();
			cal.setTime(lancamento.getContrato().getData_vencimento());
			cal.add(Calendar.MONTH, lancamento.getMeses_prorrogacao());
	
			
			
			c.setDuracao_meses(c.getDuracao_meses()+ lancamento.getMeses_prorrogacao());
			c.setMeses_vencimento(c.getMeses_vencimento()+ lancamento.getMeses_prorrogacao());
			c.setData_vencimento(cal.getTime());
			
		}
		lancamento.setContrato(c);
		String[] competencia = lanc.getCompetencia().split("/");
		contratos.save(c);
		switch(competencia[0].toUpperCase())
		{
			case "JANEIRO":
				mv.addObject("mes", "Janeiro");
			break;
			case "FEVEREIRO":
				mv.addObject("mes", "Fevereiro");
			break;
			case "MARÇO":
				mv.addObject("mes", "Março");
			break;
			case "ABRIL":
				mv.addObject("mes", "Abril");
			break;
			case "MAIO":
				mv.addObject("mes", "Maio");
			break;
			case "JUNHO":
				mv.addObject("mes", "Junho");
			break;
			case "JULHO":
				mv.addObject("mes", "Julho");
			break;
			case "AGOSTO":
				mv.addObject("mes", "Agosto");
			break;
			case "SETEMBRO":
				mv.addObject("mes", "Setembro");
			break;
			case "OUTUBRO":
				mv.addObject("mes", "Outubro");
			break;
			case "NOVEMBRO":
				mv.addObject("mes", "Novembro");
			break;
			case "DEZEMBRO":
				mv.addObject("mes", "Dezembro");
			break;
		}
		mv.addObject("data_processo", sdf.format(lanc.getProcesso().getData_abertura()));
		
		mv.addObject(lancamento);
		mv.addObject("contrato", c);
		mv.addObject("processo", p);
		if(p.getNumero_processo().contains("/"))
		{
			String [] numero = p.getNumero_processo().split("/");
			mv.addObject("n_p", numero[0]);
			mv.addObject("a_p", numero[1]);
		}
	
		return mv;
		
	}
	

	@RequestMapping(value = "/editar_lancamento/{id_lancamento}", method=RequestMethod.POST)
	public ModelAndView edicao(Lancamento lancamento, Contrato contrato, String numero_processo, String ano_processo, String tipo_processo, @DateTimeFormat(pattern= "dd/MM/yyyy") Date data_processo)
	{
		ModelAndView mv = new ModelAndView(EDICAO_VIEW);
		Lancamento lancbd = lancamentos.findOne(lancamento.getId_lancamento());
		lancbd.setAditivo_n(lancamento.getAditivo_n());
		lancbd.setData(lancamento.getData());
		//lancbd.setDataliquidacao(lancamento.getDataliquidacao());
		lancbd.setMeses_prorrogacao(lancamento.getMeses_prorrogacao());
		lancbd.setNumero_nota_fiscal(lancamento.getNumero_nota_fiscal());
		lancbd.setObservacao(lancamento.getObservacao());
		lancbd.setPossui_aditivo(lancamento.getPossui_aditivo());
		lancbd.setObservacao(lancamento.getObservacao());
		lancbd.setTipoAditivo(lancamento.getTipoAditivo());
		lancbd.setMedicao(lancamento.getMedicao());
		
		String numero_completo = numero_processo + "/" + ano_processo;
		Processo p = processos.findOne(lancbd.getProcesso().getId_processo());
		if(p.getId_processo() != 1)
		{
			p.setNumero_processo(numero_completo);
			p.setData_abertura(data_processo);
			switch(tipo_processo)
			{
				case "Pagamento":
					p.setTipo_processo(TipoProcesso.Pagamento);
				break;
				case "Renovacao":
					p.setTipo_processo(TipoProcesso.Renovacao);
				break;
			}
		}
		
		
		if(lancamento.getValor_aditivo() == null)
		{
			lancbd.setValor_aditivo(BigDecimal.ZERO);
		}
		else {
			lancbd.setValor_aditivo(lancamento.getValor_aditivo());
		}
		if(lancamento.getValor() == null) {
			lancbd.setValor(BigDecimal.ZERO);
		}
		else {lancbd.setValor(lancamento.getValor());}
		
		lancbd.setDoe_aditivo(lancamento.getDoe_aditivo());
		
		
		Contrato c = contratos.findOne(lancbd.getContrato().getId_contrato());
		DateTime inicio = new DateTime();
		c.setUltima_atualizacao(inicio.toDate());
		String datas[] = lancamento.getData().toString().split(" ");
		System.out.println(lancamento.getData().toString());
		String ano = datas[5];
		lancbd.setCompetencia(lancamento.getCompetencia()+"/"+ ano);
		processos.save(p);
		lancamentos.save(lancbd);
		recalcularSaldos(lancbd.getContrato());
		mv.addObject("mensagem", "Lançamento editado com sucesso");
		mv.addObject("contrato", c);
		return mv;
	}
	
	@ModelAttribute("todosContratos")
	public List<Contrato> todosContratos()
	{
		return contratos.findAll();
	}
	public void zerarLancamentos(List<Lancamento> l)
	{
		Iterator it = l.iterator();
		
		while(it.hasNext())
		{
			Lancamento lanc = (Lancamento) it.next();
			lanc.setSaldo_contrato(BigDecimal.ZERO);
		}
		lancamentos.save(l);
	}
	
	public void recalcularSaldos(Contrato c)
	{
		List<Lancamento> lanc = c.getLancamentos();
		zerarLancamentos(lanc);
		c.setSaldo_contrato(c.getValor_contrato());
		BigDecimal saldo_corrente = new BigDecimal(c.getValor_contrato().toString());
		for(Lancamento aux:lanc)
		{
			saldo_corrente = saldo_corrente.subtract(aux.getValor());
			if(aux.getPossui_aditivo())
			{
				saldo_corrente = saldo_corrente.add(aux.getValor_aditivo());
			}
			aux.setSaldo_contrato(saldo_corrente);
		
		}
		lancamentos.save(lanc);
		c.setSaldo_contrato(saldo_corrente);
		contratos.save(c);
	}
	
	@RequestMapping(value="/remove/{id_lancamento}")
	public String excluir(@PathVariable Integer id_lancamento, RedirectAttributes attributes)
	{
		Lancamento lancamento = lancamentos.findOne(id_lancamento);	
		Contrato c = contratos.findOne(lancamento.getContrato().getId_contrato());
		BigDecimal resultop1 = new BigDecimal(c.getSaldo_contrato().toString());
		BigDecimal resultop2 = new BigDecimal(lancamento.getValor().toString());
		BigDecimal resultop3 = new BigDecimal(BigDecimal.ZERO.toString());
		if(lancamento.getValor_aditivo() != null) {
			resultop3 = resultop3.add(lancamento.getValor_aditivo());
		}
		DateTime inicio = new DateTime();
		c.setUltima_atualizacao(inicio.toDate());
		c.setSaldo_contrato(resultop1.add(resultop2));
		if(lancamento.getProcesso() != null) {
			Processo p = processos.findOne(lancamento.getProcesso().getId_processo());
			p.setLancamento(null);
			processos.save(p);
		}
		lancamento.setProcesso(null);
		lancamento.setContrato(null);
		
		if(lancamento.getPossui_aditivo() == true)
		{
			c.setSaldo_contrato(resultop1.subtract(resultop3));
		}
		recalcularSaldos(c);
		contratos.save(c);
		lancamentos.delete(lancamento);
		return "redirect:/transparenciactb/lancamentos/pesquisar/"+ c.getId_contrato();	
	
	}
	
public void desvincularLancamento(Lancamento lancamento)
	
	{
	
	if(lancamento.getContrato() != null)
	{
		desvincularContratoLancamento(lancamento.getContrato());
	}

		
	}

public void desvincularContratoLancamento(Contrato contrato)
{
	List<Lancamento> contratosLancamentos = contrato.getLancamentos();
	//System.out.println(contratosLancamentos.size());
	if(contratosLancamentos != null) {
		Iterator it = contratosLancamentos.iterator();
	
	while(it.hasNext())
	{
		Lancamento obj = (Lancamento) it.next();
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

@ModelAttribute("permissao")
public boolean temPermissao() {
	Object usuarioLogado = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	return ((UserDetails)usuarioLogado).getAuthorities().toString().contains("ROLE_CADASTRAR_CONTRATO");
}

}
