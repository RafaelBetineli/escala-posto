package br.com.posto.escala.repository;

import br.com.posto.escala.domain.Funcionario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {
    List<Funcionario> findAllByOrderByNomeAscIdAsc();
    List<Funcionario> findByAtivoTrueOrderByNomeAscIdAsc();
}
