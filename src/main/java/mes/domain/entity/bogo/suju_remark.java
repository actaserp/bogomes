package mes.domain.entity.bogo;

import lombok.Data;
import lombok.NoArgsConstructor;
import mes.domain.entity.AbstractAuditModel;

import javax.persistence.*;

@Entity
@Table(name="suju_remark")
@NoArgsConstructor
@Data
public class suju_remark extends AbstractAuditModel {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;

  @Column(name = "sujuid")
  private Integer sujuId;

  @Column(name = "\"JumunDate\"")
  private String jumunDate;

  @Column(name = "reseq")
  private Integer reseq;

  @Column(name = "sjremark")
  private String sjremark;

  @Column(name = "\"ErrMsg\"")
  private String errMsg;
}