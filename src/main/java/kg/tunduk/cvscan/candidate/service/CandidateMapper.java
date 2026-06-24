package kg.tunduk.cvscan.candidate.service;

import kg.tunduk.cvscan.candidate.dto.CandidateResponse;
import kg.tunduk.cvscan.candidate.dto.CriteriaItemDto;
import kg.tunduk.cvscan.candidate.dto.ExperienceItemDto;
import kg.tunduk.cvscan.candidate.model.Candidate;
import kg.tunduk.cvscan.candidate.model.CriteriaItem;
import kg.tunduk.cvscan.candidate.model.ExperienceItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CandidateMapper {

    public CandidateResponse toResponse(Candidate c) {
        return new CandidateResponse(
                c.getId(),
                c.getName(),
                c.getEmail(),
                c.getPhone(),
                c.getPosition(),
                c.getPosLabel(),
                c.getCity(),
                c.getTelegram(),
                c.getTotalExp(),
                c.getStack(),
                c.getEducation(),
                c.getVerdict(),
                c.getSummary(),
                toCriteriaDto(c.getCriteria()),
                toExperienceDto(c.getExperience()),
                c.getQuestions(),
                c.getStatus(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }

    private List<CriteriaItemDto> toCriteriaDto(List<CriteriaItem> items) {
        return items.stream()
                .map(i -> new CriteriaItemDto(i.getKey(), i.getResult(), i.getComment()))
                .toList();
    }

    private List<ExperienceItemDto> toExperienceDto(List<ExperienceItem> items) {
        return items.stream()
                .map(i -> new ExperienceItemDto(i.getPeriod(), i.getCompany(), i.getTitle(), i.getDuration()))
                .toList();
    }

    public List<CriteriaItem> toCriteriaEntity(List<CriteriaItemDto> items) {
        return items.stream()
                .map(i -> new CriteriaItem(i.key(), i.result(), i.comment()))
                .toList();
    }

    public List<ExperienceItem> toExperienceEntity(List<ExperienceItemDto> items) {
        return items.stream()
                .map(i -> new ExperienceItem(i.period(), i.company(), i.title(), i.duration()))
                .toList();
    }
}
