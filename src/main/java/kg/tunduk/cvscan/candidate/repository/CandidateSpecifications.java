package kg.tunduk.cvscan.candidate.repository;

import kg.tunduk.cvscan.candidate.model.Candidate;
import kg.tunduk.cvscan.candidate.model.CandidateStatus;
import kg.tunduk.cvscan.candidate.model.Verdict;
import org.springframework.data.jpa.domain.Specification;

public final class CandidateSpecifications {

    private CandidateSpecifications() {
    }

    public static Specification<Candidate> hasVerdict(Verdict verdict) {
        return (root, query, cb) ->
                verdict == null ? null : cb.equal(root.get("verdict"), verdict);
    }

    public static Specification<Candidate> hasStatus(CandidateStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Candidate> hasPosition(String position) {
        return (root, query, cb) ->
                (position == null || position.isBlank()) ? null : cb.equal(root.get("position"), position);
    }

    public static Specification<Candidate> nameContains(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return null;
            }
            return cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%");
        };
    }
}
