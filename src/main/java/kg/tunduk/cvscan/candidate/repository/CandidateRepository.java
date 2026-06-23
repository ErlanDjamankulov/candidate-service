package kg.tunduk.cvscan.candidate.repository;

import kg.tunduk.cvscan.candidate.model.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface CandidateRepository
        extends JpaRepository<Candidate, String>, JpaSpecificationExecutor<Candidate> {

    Optional<Candidate> findByEmail(String email);

    boolean existsByEmail(String email);
}
