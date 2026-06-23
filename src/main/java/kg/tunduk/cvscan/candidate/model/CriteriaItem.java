package kg.tunduk.cvscan.candidate.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CriteriaItem {

    private String key;
    private String result;
    private String comment;
}
