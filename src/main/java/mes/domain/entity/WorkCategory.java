package mes.domain.entity;


import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import javax.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "work_category", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkCategory {

    @EmbeddedId
    private WorkCategoryId id;

    @Column(name = "\"JobResId\"", insertable = false, updatable = false)
    private int jobResId;

    @Column(name = "\"WorkCode\"", insertable = false, updatable = false)
    private String workCode;

    @Column(name = "\"StartTime\"")
    private String startTime;

    @Column(name = "\"EndTime\"")
    private String endTime;

    @Column(name = "\"WorkName\"", nullable = false)
    private String workName;

    @Column(name = "\"JobResNum\"", nullable = false)
    private String jobResNum;

    @Column(name = "\"JumunNum\"", nullable = false)
    private String jumunNum;

    @Column(name = "\"Desciption\"")
    private String description;

    @Column(name = "\"ProjectName\"", nullable = false)
    private String projectName;

    @Column(name = "spjangcd")
    private String spjangcd;

    @Column(name = "company_id")
    private Integer company_id;

}
