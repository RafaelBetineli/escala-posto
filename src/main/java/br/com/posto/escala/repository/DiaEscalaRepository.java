package br.com.posto.escala.repository;

import br.com.posto.escala.domain.DiaEscala;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiaEscalaRepository extends JpaRepository<DiaEscala, Long> {
    @Query("select d from DiaEscala d join fetch d.funcionario where d.escala.id = :escalaId order by d.funcionario.nome, d.funcionario.id, d.data")
    List<DiaEscala> buscarPorEscala(@Param("escalaId") Long escalaId);

    @Modifying
    @Query("delete from DiaEscala d where d.escala.id = :escalaId")
    int excluirPorEscala(@Param("escalaId") Long escalaId);
}
