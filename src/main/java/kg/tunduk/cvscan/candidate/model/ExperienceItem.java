package kg.tunduk.cvscan.candidate.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceItem {

    private String period;
    private String company;
    private String title;
    private String duration;
}
