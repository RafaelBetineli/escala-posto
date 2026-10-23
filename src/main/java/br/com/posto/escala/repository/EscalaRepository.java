package br.com.posto.escala.repository;

import br.com.posto.escala.domain.Escala;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EscalaRepository extends JpaRepository<Escala, Long> {
    Optional<Escala> findByMesAndAno(int mes, int ano);
    List<Escala> findAllByOrderByAnoDescMesDesc();
}
