package kg.tunduk.cvscan.candidate.controller;

import jakarta.validation.Valid;
import kg.tunduk.cvscan.candidate.dto.CandidatePage;
import kg.tunduk.cvscan.candidate.dto.CandidateResponse;
import kg.tunduk.cvscan.candidate.dto.CandidateWriteRequest;
import kg.tunduk.cvscan.candidate.dto.StatusChangeRequest;
import kg.tunduk.cvscan.candidate.dto.StatusHistoryEntry;
import kg.tunduk.cvscan.candidate.model.CandidateStatus;
import kg.tunduk.cvscan.candidate.model.Verdict;
import kg.tunduk.cvscan.candidate.service.CandidateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/candidates")
public class CandidateController {

    private final CandidateService candidateService;

    public CandidateController(CandidateService candidateService) {
        this.candidateService = candidateService;
    }

    @GetMapping
    public CandidatePage listCandidates(
            @RequestParam(required = false) Verdict verdict,
            @RequestParam(required = false) CandidateStatus status,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        return candidateService.list(verdict, status, position, search, page, size, sort);
    }

    @GetMapping("/{id}")
    public CandidateResponse getCandidate(@PathVariable String id) {
        return candidateService.getById(id);
    }

    @PostMapping
    public ResponseEntity<CandidateResponse> createCandidate(
            @Valid @RequestBody CandidateWriteRequest request) {

        CandidateResponse created = candidateService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public CandidateResponse updateCandidate(
            @PathVariable String id,
            @Valid @RequestBody CandidateWriteRequest request) {

        return candidateService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCandidate(@PathVariable String id) {
        candidateService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public CandidateResponse changeStatus(
            @PathVariable String id,
            @Valid @RequestBody StatusChangeRequest request) {

        return candidateService.changeStatus(id, request.status(), request.comment());
    }

    @GetMapping("/{id}/status-history")
    public List<StatusHistoryEntry> getStatusHistory(@PathVariable String id) {
        return candidateService.getStatusHistory(id);
    }
}
