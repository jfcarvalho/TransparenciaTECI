package com.ctb.contratos.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ctb.contratos.model.Contratado;
import com.ctb.contratos.model.Contrato;
import com.ctb.contratos.model.Garantia;
import com.ctb.contratos.model.Usuario;

public interface Garantias extends JpaRepository<Garantia, Integer>{
	
	
	
}
