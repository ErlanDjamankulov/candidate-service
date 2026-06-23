package kg.tunduk.cvscan.candidate.exception;

public class CandidateNotFoundException extends RuntimeException {

    private final String candidateId;

    public CandidateNotFoundException(String candidateId) {
        super("Кандидат '" + candidateId + "' не найден");
        this.candidateId = candidateId;
    }

    public String getCandidateId() {
        return candidateId;
    }
}
