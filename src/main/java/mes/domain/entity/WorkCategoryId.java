package mes.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkCategoryId implements Serializable {

    @Column(name = "\"JobResId\"", nullable = false)
    private Integer jobResId;

    @Column(name = "\"WorkCode\"", nullable = false)
    private String workCode;
}
