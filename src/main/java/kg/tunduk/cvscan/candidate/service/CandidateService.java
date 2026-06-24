package kg.tunduk.cvscan.candidate.service;

import kg.tunduk.cvscan.candidate.dto.CandidatePage;
import kg.tunduk.cvscan.candidate.dto.CandidateResponse;
import kg.tunduk.cvscan.candidate.dto.CandidateWriteRequest;
import kg.tunduk.cvscan.candidate.dto.StatusHistoryEntry;
import kg.tunduk.cvscan.candidate.dto.event.CvParsedEvent;
import kg.tunduk.cvscan.candidate.exception.CandidateNotFoundException;
import kg.tunduk.cvscan.candidate.exception.DuplicateEmailException;
import kg.tunduk.cvscan.candidate.messaging.StatusChangedProducer;
import kg.tunduk.cvscan.candidate.model.Candidate;
import kg.tunduk.cvscan.candidate.model.CandidateStatus;
import kg.tunduk.cvscan.candidate.model.StatusHistory;
import kg.tunduk.cvscan.candidate.model.Verdict;
import kg.tunduk.cvscan.candidate.repository.CandidateRepository;
import kg.tunduk.cvscan.candidate.repository.CandidateSpecifications;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final StatusService statusService;
    private final CandidateMapper mapper;
    private final StatusChangedProducer statusChangedProducer;

    public CandidateService(
            CandidateRepository candidateRepository,
            StatusService statusService,
            CandidateMapper mapper,
            StatusChangedProducer statusChangedProducer
            ) {
        this.candidateRepository = candidateRepository;
        this.statusService = statusService;
        this.mapper = mapper;
        this.statusChangedProducer = statusChangedProducer;
    }

    @Transactional(readOnly = true)
    public CandidatePage list(Verdict verdict, CandidateStatus status, String position,
                               String search, int page, int size, String sort) {

        Specification<Candidate> spec = Specification.allOf();

        if (verdict != null) {
            spec = spec.and(CandidateSpecifications.hasVerdict(verdict));
        }
        if (status != null) {
            spec = spec.and(CandidateSpecifications.hasStatus(status));
        }
        if (position != null && !position.isBlank()) {
            spec = spec.and(CandidateSpecifications.hasPosition(position));
        }
        if (search != null && !search.isBlank()) {
            spec = spec.and(CandidateSpecifications.nameContains(search));
        }

        Sort resolvedSort = parseSort(sort);
        PageRequest pageRequest = PageRequest.of(page, size, resolvedSort);

        var resultPage = candidateRepository.findAll(spec, pageRequest);

        List<CandidateResponse> content = resultPage.getContent().stream()
                .map(mapper::toResponse)
                .toList();

        return new CandidatePage(
                content,
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages()
        );
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String[] parts = sort.split(",");
        String property = mapSortProperty(parts[0].trim());
        Sort.Direction direction = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim()))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }

    private String mapSortProperty(String requested) {
        return switch (requested) {
            case "name" -> "name";
            case "totalExp" -> "totalExp";
            case "createdAt" -> "createdAt";
            case "updatedAt" -> "updatedAt";
            case "status" -> "status";
            case "verdict" -> "verdict";
            default -> "createdAt";
        };
    }

    @Transactional(readOnly = true)
    public CandidateResponse getById(String id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public CandidateResponse create(CandidateWriteRequest request) {
        if (candidateRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        Candidate candidate = new Candidate();
        candidate.setId(generateId(request.name()));
        applyWriteRequest(candidate, request);
        candidate.setStatus(CandidateStatus.NEW);

        Candidate saved = candidateRepository.save(candidate);
        return mapper.toResponse(saved);
    }

    @Transactional
    public CandidateResponse update(String id, CandidateWriteRequest request) {
        Candidate candidate = findOrThrow(id);

        if (!candidate.getEmail().equalsIgnoreCase(request.email())
                && candidateRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        applyWriteRequest(candidate, request);

        Candidate saved = candidateRepository.save(candidate);
        return mapper.toResponse(saved);
    }

    @Transactional
    public void delete(String id) {
        Candidate candidate = findOrThrow(id);
        candidateRepository.delete(candidate);
    }

    @Transactional
    public CandidateResponse changeStatus(String id, CandidateStatus newStatus, String comment) {
        Candidate candidate = findOrThrow(id);
        CandidateStatus current = candidate.getStatus();

        statusService.assertTransitionAllowed(current, newStatus);

        candidate.setStatus(newStatus);
        candidateRepository.save(candidate);

        statusService.recordTransition(id, current, newStatus, comment);
        statusChangedProducer.publish(id, current, newStatus);

        return mapper.toResponse(candidate);
    }

    @Transactional(readOnly = true)
    public List<StatusHistoryEntry> getStatusHistory(String id) {
        findOrThrow(id);
        return statusService.getHistory(id).stream()
                .map(this::toHistoryEntry)
                .toList();
    }

    /**
     * Создание кандидата из Kafka-события cv.parsed.
     * Возвращает true при создании и  false, если событие — повтор,игнорирование.
     */
    @Transactional
    public boolean createFromParsedEvent(CvParsedEvent event) {
        if (candidateRepository.existsById(event.candidateId())) {
            return false;
        }

        Candidate candidate = new Candidate();
        candidate.setId(event.candidateId());
        candidate.setName(event.name());
        candidate.setEmail(event.email());
        candidate.setPhone(event.phone());
        candidate.setPosition(event.position());
        candidate.setPosLabel(event.posLabel());
        candidate.setCity(event.city());
        candidate.setTelegram(event.telegram());
        candidate.setTotalExp(event.totalExp());
        candidate.setStack(event.stack());
        candidate.setEducation(event.education());
        candidate.setVerdict(event.verdict());
        candidate.setSummary(event.summary());
        candidate.setCriteria(event.criteria() != null
                ? mapper.toCriteriaEntity(event.criteria())
                : new ArrayList<>());
        candidate.setExperience(event.experience() != null
                ? mapper.toExperienceEntity(event.experience())
                : new ArrayList<>());
        candidate.setQuestions(event.questions() != null
                ? new ArrayList<>(event.questions())
                : new ArrayList<>());
        candidate.setParsedAt(event.parsedAt());
        candidate.setStatus(CandidateStatus.NEW);

        candidateRepository.save(candidate);
        return true;
    }

    private void applyWriteRequest(Candidate candidate, CandidateWriteRequest request) {
        candidate.setName(request.name());
        candidate.setEmail(request.email());
        candidate.setPhone(request.phone());
        candidate.setPosition(request.position());
        candidate.setPosLabel(request.posLabel());
        candidate.setCity(request.city());
        candidate.setTelegram(request.telegram());
        candidate.setTotalExp(request.totalExp());
        candidate.setStack(request.stack());
        candidate.setEducation(request.education());
        candidate.setVerdict(request.verdict());
        candidate.setSummary(request.summary());
        candidate.setCriteria(mapper.toCriteriaEntity(request.criteria()));
        candidate.setExperience(mapper.toExperienceEntity(request.experience()));
        candidate.setQuestions(new ArrayList<>(request.questions()));
    }

    private Candidate findOrThrow(String id) {
        return candidateRepository.findById(id)
                .orElseThrow(() -> new CandidateNotFoundException(id));
    }

    private StatusHistoryEntry toHistoryEntry(StatusHistory h) {
        return new StatusHistoryEntry(
                h.getId(),
                h.getCandidateId(),
                h.getFromStatus(),
                h.getToStatus(),
                h.getComment(),
                h.getChangedAt()
        );
    }

    private static final java.util.Map<Character, String> CYRILLIC_TO_LATIN = java.util.Map.ofEntries(
            java.util.Map.entry('а', "a"), java.util.Map.entry('б', "b"), java.util.Map.entry('в', "v"),
            java.util.Map.entry('г', "g"), java.util.Map.entry('д', "d"), java.util.Map.entry('е', "e"),
            java.util.Map.entry('ё', "e"), java.util.Map.entry('ж', "zh"), java.util.Map.entry('з', "z"),
            java.util.Map.entry('и', "i"), java.util.Map.entry('й', "i"), java.util.Map.entry('к', "k"),
            java.util.Map.entry('л', "l"), java.util.Map.entry('м', "m"), java.util.Map.entry('н', "n"),
            java.util.Map.entry('о', "o"), java.util.Map.entry('п', "p"), java.util.Map.entry('р', "r"),
            java.util.Map.entry('с', "s"), java.util.Map.entry('т', "t"), java.util.Map.entry('у', "u"),
            java.util.Map.entry('ф', "f"), java.util.Map.entry('х', "h"), java.util.Map.entry('ц', "ts"),
            java.util.Map.entry('ч', "ch"), java.util.Map.entry('ш', "sh"), java.util.Map.entry('щ', "sch"),
            java.util.Map.entry('ъ', ""), java.util.Map.entry('ы', "y"), java.util.Map.entry('ь', ""),
            java.util.Map.entry('э', "e"), java.util.Map.entry('ю', "yu"), java.util.Map.entry('я', "ya")
    );

    private String transliterate(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toLowerCase(Locale.ROOT).toCharArray()) {
            sb.append(CYRILLIC_TO_LATIN.getOrDefault(c, String.valueOf(c)));
        }
        return sb.toString();
    }

    private String generateId(String name) {
        String transliterated = transliterate(name);
        String base = transliterated
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (base.isBlank()) {
            base = "candidate";
        }
        String id = base;
        int suffix = 1;
        while (candidateRepository.existsById(id)) {
            id = base + "-" + suffix++;
        }
        return id;
    }
}
