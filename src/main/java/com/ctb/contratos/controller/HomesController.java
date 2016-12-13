package com.ctb.contratos.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Months;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.ctb.contratos.model.Contratado;
import com.ctb.contratos.model.Contrato;
import com.ctb.contratos.model.Lancamento;
import com.ctb.contratos.model.Usuario;
import com.ctb.contratos.repository.Contratados;
import com.ctb.contratos.repository.Contratos;
import com.ctb.contratos.repository.Lancamentos;
import com.ctb.contratos.repository.Usuarios;
import com.ctb.security.AppUserDetailsService;

@Controller
@RequestMapping("/transparenciactb/")
public class HomesController {
	private String HOME_VIEW = "/home/PaginaInicial";
	
	@Autowired
	private Usuarios usuarios;
	
	@Autowired
	private Contratos contratos;
	
	@Autowired
	private Lancamentos lancamentos;
	@Autowired
	private Contratados contratadas;
	
	@RequestMapping(method=RequestMethod.GET)
	public ModelAndView index()
	{
		ModelAndView mv = new ModelAndView(HOME_VIEW);		
		mv.addObject(new HomesController());
		List<Contrato> contratosgeridos = contratosGeridos();
		Iterator it = contratosgeridos.iterator();
		BigDecimal acumuladoValorJaneiro = new BigDecimal("0");
		BigDecimal acumuladoValorFevereiro = new BigDecimal("0");
		BigDecimal acumuladoValorMarco = new BigDecimal("0");
		BigDecimal acumuladoValorAbril = new BigDecimal("0");
		BigDecimal acumuladoValorMaio = new BigDecimal("0");
		BigDecimal acumuladoValorJunho = new BigDecimal("0");
		BigDecimal acumuladoValorJulho = new BigDecimal("0");
		BigDecimal acumuladoValorAgosto = new BigDecimal("0");
		BigDecimal acumuladoValorSetembro = new BigDecimal("0");
		BigDecimal acumuladoValorOutubro = new BigDecimal("0");
		BigDecimal acumuladoValorNovembro = new BigDecimal("0");
		BigDecimal acumuladoValorDezembro = new BigDecimal("0");
		int  flagmes;
		String periodoAComparar = new String();
		String ano = new String();
		DateTime date = new DateTime();
		ano = Integer.toString(date.getYear());
		
		while(it.hasNext())
		{
			Contrato obj = (Contrato) it.next();
			List<Lancamento> lancamentos = obj.getLancamentos();
			
			for(int i=1; i < 13; i++)
			{
				flagmes = 0;
				if(i > 0 && i < 10) {
					periodoAComparar = "-0"+ Integer.toString(i)+"-";
				}else {periodoAComparar =  "-"+ Integer.toString(i)+"-";}
				Iterator it2 = lancamentos.iterator();
				while(it2.hasNext()) {
				
					Lancamento obj2 = (Lancamento) it2.next();
				
					
					if(obj2.getData().toString().contains(periodoAComparar))
					{ 
						//lagmes = 1;
						switch(i)
						{
							case 1:
								acumuladoValorJaneiro = acumuladoValorJaneiro.add(obj2.getValor());
								break;
							case 2:
								acumuladoValorFevereiro = acumuladoValorFevereiro.add(obj2.getValor());
								break;
							case 3:
								acumuladoValorMarco = acumuladoValorMarco.add(obj2.getValor());
								break;
							case 4:
								acumuladoValorAbril = acumuladoValorAbril.add(obj2.getValor());
								break;
							case 5:
								acumuladoValorMaio = acumuladoValorMaio.add(obj2.getValor());
								break;
							case 6:
								acumuladoValorJunho = acumuladoValorJunho.add(obj2.getValor());
								break;
							case 7:
								acumuladoValorJulho = acumuladoValorJulho.add(obj2.getValor());
								break;
							case 8:
								acumuladoValorAgosto = acumuladoValorAgosto.add(obj2.getValor());
								System.out.println("Valor de AGosto: " + acumuladoValorAgosto);
								break;
							case 9:
								acumuladoValorSetembro = acumuladoValorSetembro.add(obj2.getValor());
								break;
							case 10:
								acumuladoValorOutubro = acumuladoValorOutubro.add(obj2.getValor());
								System.out.println("Valor de Outubro: " + acumuladoValorOutubro);
								break;
							case 11:
								acumuladoValorNovembro = acumuladoValorNovembro.add(obj2.getValor());
								break;
							case 12:
								acumuladoValorDezembro = acumuladoValorDezembro.add(obj2.getValor());
								break;
						}				
					}	
				}
		}
	}
		mv.addObject("valorjaneiro", acumuladoValorJaneiro);
		mv.addObject("valorfevereiro", acumuladoValorFevereiro);
		mv.addObject("valormarco", acumuladoValorMarco);
		mv.addObject("valorabril", acumuladoValorAbril);
		mv.addObject("valormaio", acumuladoValorMaio);
		mv.addObject("valorjunho", acumuladoValorJunho);
		mv.addObject("valorjulho", acumuladoValorJulho);
		mv.addObject("valoragosto", acumuladoValorAgosto);
		mv.addObject("valorsetembro", acumuladoValorSetembro);
		mv.addObject("valoroutubro", acumuladoValorOutubro);
		mv.addObject("valornovembro", acumuladoValorNovembro);
		mv.addObject("valordezembro", acumuladoValorDezembro);
		mv.addObject("ntotal_contratos", contratos.count());
		mv.addObject("ntotal_empresas", contratadas.count());
		mv.addObject("ntotal_gestores", usuarios.count());
		mv.addObject("ntotal_lancamentos", lancamentos.count());
	
		return mv;
}	
	@ModelAttribute("permissao")
	public boolean temPermissao() {
		return AppUserDetailsService.cusuario.getAuthorities().toString().contains("ROLE_CADASTRAR_CONTRATO");
	}
	
	@ModelAttribute("home_gestor_contratos")
	public boolean homeGestor() {
		return AppUserDetailsService.cusuario.getAuthorities().toString().contains("ROLE_HOME_GESTOR_CONTRATOS");
	}
	
	@ModelAttribute("contratosgeridos")
	public List<Contrato> contratosGeridos()
	{
		List<Contrato> todosContratos = contratos.findAll();
		List<Contrato> contratosGeridos = new ArrayList<Contrato>();
		Usuario gestor = usuarios.findByEmail(AppUserDetailsService.cusuario.getUsername());
		Iterator it = todosContratos.iterator();
	
		while(it.hasNext())
		{
			Contrato obj = (Contrato) it.next();
			
			if(obj.getGestor().getId_usuario() == gestor.getId_usuario()) {
				contratosGeridos.add(obj);
			}
		}
		
	    
		return contratosGeridos;
	
	}
	
	@ModelAttribute("ultimoslancamentos")
	public List<Lancamento> ultimosLancamentos()
	{
		List<Contrato> contratosGeridos = contratosGeridos();
		List<Lancamento> lancamentos = new ArrayList<Lancamento>();
		Iterator it = contratosGeridos.iterator();
	
		while(it.hasNext())
		{
			Contrato obj = (Contrato) it.next();
			List<Lancamento> laux = obj.getLancamentos();
			Iterator it2 = laux.iterator();
			while(it2.hasNext())
			{
				Lancamento l = (Lancamento) it2.next();
				
				lancamentos.add(l);
			}
			
			
		}
		Comparator<Lancamento> cmp = new Comparator<Lancamento>() {
	        public int compare(Lancamento l1, Lancamento l2) {
	          return l2.getData().compareTo(l1.getData());
	        }
	    
		};
		Collections.sort(lancamentos, cmp);
		
		List<Lancamento> novaListaLimitada = new ArrayList<Lancamento>();
		int contador = 0; 
		for (Lancamento l : lancamentos)
		{
			if(contador < 10 ) {
				novaListaLimitada.add(l);
				contador++;
			}
			else {break;}
		}
		
		
		return novaListaLimitada;
	
	}
	
	
	@ModelAttribute("ultimoslancamentos_todos_contratos")
	public List<Lancamento> ultimosLancamentosTodosContratos()
	{
		List<Lancamento> todos = lancamentos.findAll();
		Comparator<Lancamento> cmp = new Comparator<Lancamento>() {
	        public int compare(Lancamento l1, Lancamento l2) {
	          return l2.getData().compareTo(l1.getData());
	        }
	    
		};
		Collections.sort(todos, cmp);
		
		List<Lancamento> novaListaLimitada = new ArrayList<Lancamento>();
		int contador = 0; 
		for (Lancamento l : todos)
		{
			if(contador < 15 ) {
				novaListaLimitada.add(l);
				contador++;
			}
			else {break;}
		}
		
		
		return novaListaLimitada;
	
	}
	
	
	@ModelAttribute("n_contratosgeridos")
	public int ncontratosGeridos()
	{
		List<Contrato> todosContratos = contratos.findAll();
		Usuario gestor = usuarios.findByEmail(AppUserDetailsService.cusuario.getUsername());
		int n_geridos = 0;
		Iterator it = todosContratos.iterator();
	
		while(it.hasNext())
		{
			Contrato obj = (Contrato) it.next();
			
			if(obj.getGestor().getId_usuario() == gestor.getId_usuario()) {
				n_geridos++;
			}
		}
		return n_geridos;
	
	}
	
	@ModelAttribute("gestor")
	public Usuario gestor()
	{
		 Usuario gestor = usuarios.findByEmail(AppUserDetailsService.cusuario.getUsername());
		 return gestor;
	}
	
	@ModelAttribute("vencimento90dias")
	public List<Contrato> vencimento90()
	{
		List<Contrato> contratosgeridos = contratosGeridos();
		List<Contrato> contratosavencer = new ArrayList<Contrato>();
		Iterator it = contratosgeridos.iterator();
		while(it.hasNext())
		{
			Contrato obj = (Contrato) it.next();
			DateTime inicio = new DateTime();
			DateTime fim = new DateTime(obj.getData_vencimento());
			Days d = Days.daysBetween(inicio, fim);
			if(d.getDays() <= 90 && d.getDays() >0)
			{
				contratosavencer.add(obj);
			}
		}
		return contratosavencer;
	}
	
	@ModelAttribute("vencimento90dias_todos")
	public List<Contrato> vencimento90_todos()
	{
		List<Contrato> contratosgeridos = contratos.findAll();
		List<Contrato> contratosavencer = new ArrayList<Contrato>();
		Iterator it = contratosgeridos.iterator();
		while(it.hasNext())
		{
			Contrato obj = (Contrato) it.next();
			DateTime inicio = new DateTime();
			DateTime fim = new DateTime(obj.getData_vencimento());
			Days d = Days.daysBetween(inicio, fim);
			if(d.getDays() <= 90 && d.getDays() >0)
			{
				contratosavencer.add(obj);
			}
		}
		return contratosavencer;
	}
	
	@ModelAttribute("acumladovalor")
	public BigDecimal acumuladovalor()
	{
		List<Contrato> contratosgeridos = contratosGeridos();
		Iterator it = contratosgeridos.iterator();
		BigDecimal acumuladoValor = new BigDecimal("0"); 
		while(it.hasNext())
		{
			Contrato obj = (Contrato) it.next();
			List<Lancamento> lancamentos = obj.getLancamentos();
			
			Iterator it2 = lancamentos.iterator();
			
			while(it2.hasNext())
			{
				Lancamento l = (Lancamento) it2.next();
				acumuladoValor = acumuladoValor.add(l.getValor());
			}
		
		}
		return acumuladoValor;
	}
	
	@ModelAttribute("acumladoaditivo")
	public BigDecimal acumuladoaditivo()
	{
		List<Contrato> contratosgeridos = contratosGeridos();
		Iterator it = contratosgeridos.iterator();
		BigDecimal acumuladoValorAditivo = new BigDecimal("0"); 
		while(it.hasNext())
		{
			Contrato obj = (Contrato) it.next();
			List<Lancamento> lancamentos = obj.getLancamentos();
			
			Iterator it2 = lancamentos.iterator();
			
			while(it2.hasNext())
			{
				Lancamento l = (Lancamento) it2.next();
				if(l.getPossui_aditivo() == true)
				{
					acumuladoValorAditivo = acumuladoValorAditivo.add(l.getValor_aditivo()); 
				}
				//acumuladoValor = acumuladoValor.add(l.getValor());
			}
		
		}
		return acumuladoValorAditivo;
	}
	//Otimizar isso aqui!!! muita função duplicada q pode se transformar em uma fnção só com flag
	@ModelAttribute("lancamentospagos")
	public BigDecimal Lancamentospagos()
	{
		List<Contrato> contratosgeridos = contratosGeridos();
		Iterator it = contratosgeridos.iterator();
		BigDecimal acumuladoPago = new BigDecimal("0"); 
		while(it.hasNext())
		{
			Contrato obj = (Contrato) it.next();
			List<Lancamento> lancamentos = obj.getLancamentos();
			
			Iterator it2 = lancamentos.iterator();
			
			while(it2.hasNext())
			{
				Lancamento l = (Lancamento) it2.next();
				if(l.getProcesso()  !=  null) 
				{
					acumuladoPago = acumuladoPago.add(l.getValor());
				}
				//acumuladoValor = acumuladoValor.add(l.getValor());
			}
		
		}
		return acumuladoPago;
	}

	@ModelAttribute("lancamentosnpagos")
	public BigDecimal LancamentosNaopagos()
	{
		List<Contrato> contratosgeridos = contratosGeridos();
		Iterator it = contratosgeridos.iterator();
		BigDecimal acumuladoNaoPago = new BigDecimal("0"); 
		while(it.hasNext())
		{
			Contrato obj = (Contrato) it.next();
			List<Lancamento> lancamentos = obj.getLancamentos();
			
			Iterator it2 = lancamentos.iterator();
			
			while(it2.hasNext())
			{
				Lancamento l = (Lancamento) it2.next();
				if(l.getProcesso()  ==  null) 
				{
					acumuladoNaoPago = acumuladoNaoPago.add(l.getValor());
				}
				//acumuladoValor = acumuladoValor.add(l.getValor());
			}
		
		}
		return acumuladoNaoPago;
	}
	
	@ModelAttribute("nomes_empresas")
	public List<String> pegarNomeEmpresas()
	{
		List<Contratado> empresas = contratadas.findAll();
		List<String> nomes = new ArrayList<String>();
		Iterator it = empresas.iterator();
		while (it.hasNext())
		{
			Contratado c = (Contratado) it.next();
			nomes.add(c.getNome());
		}
		return nomes;
	}
	
}
