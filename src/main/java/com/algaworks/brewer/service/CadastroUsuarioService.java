package com.algaworks.brewer.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.algaworks.brewer.model.Usuario;
import com.algaworks.brewer.repository.Usuarios;
import com.algaworks.brewer.service.exception.EmailUsuarioJaCadastradoException;
import com.algaworks.brewer.service.exception.SenhaObrigatoriaUsuarioException;

@Service
public class CadastroUsuarioService {

	@Autowired
	private Usuarios usuarios;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@Transactional
	public void salvar(Usuario usuario) {
		
		validaEmailJaCadastrado(usuario);
		
		if (usuario.isNovo()) {
			validaUsuarioNovo(usuario);
		} else {
			validaUsuarioExistente(usuario);
		}
		
		usuario.setConfirmacaoSenha(usuario.getSenha());
				
		usuarios.save(usuario);
	}

	@Transactional
	public void alterarStatus(Long[] codigos, StatusUsuario statusUsuario) {
		statusUsuario.executar(codigos, usuarios);
	}

	private void validaUsuarioExistente(Usuario usuario) {
		Usuario usuarioExistente = usuarios.findOne(usuario.getCodigo());
		
		if (!StringUtils.isEmpty(usuario.getSenha())) {
			usuario.setSenha(this.passwordEncoder.encode(usuario.getSenha()));
		} else {
			usuario.setSenha(usuarioExistente.getSenha());
		}
		
		if (usuario.getAtivo() == null) {
			usuario.setAtivo(usuarioExistente.getAtivo());
		}
	}

	private void validaUsuarioNovo(Usuario usuario) {
		if (StringUtils.isEmpty(usuario.getSenha())) {
			throw new SenhaObrigatoriaUsuarioException("Senha é obrigatória para novo usuário.");
		}
		
		usuario.setSenha(this.passwordEncoder.encode(usuario.getSenha()));
	}
	
	private void validaEmailJaCadastrado(Usuario usuario) {
		Optional<Usuario> usuarioExistente = usuarios.findByEmail(usuario.getEmail());
		
		if (usuarioExistente.isPresent() && !usuarioExistente.get().equals(usuario)) {
			throw new EmailUsuarioJaCadastradoException("E-mail já cadastrado");
		}	
	}

	
}