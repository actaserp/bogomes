package mes.domain.entity.bogo;

import lombok.Data;
import lombok.NoArgsConstructor;
import mes.domain.entity.AbstractAuditModel;

import javax.persistence.Entity;
import javax.persistence.*;

@Entity
@Table(name="suju_option")
@NoArgsConstructor
@Data
public class suju_option extends AbstractAuditModel {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;

  @Column(name = "sujuid")
  private Integer sujuId;

  @Column(name = "jumundate")
  private String jumunDate;

  @Column(name = "reseq")
  private Integer reseq;

  @Column(name = "sjoption")
  private String sjOption;

  @Column(name = "optamt")
  private Integer optAmt;

  @Column(name = "errmsg")
  private String errMsg;
}
