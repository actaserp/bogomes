package mes.domain.entity.bogo;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.sql.Date;

@Entity
@Table(name="tb_as011")
@NoArgsConstructor
@Data
public class tb_as011 {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name="id")
  Integer id;

  @Column(name="asid")
  Integer asid; //AS순번

  @Column(name="vechidno")
  String vechidno;  //차대번호

  @Column(name="regdate")
  Date regdate; //등록일자

  @Column(name="fixdate")
  Date fixdate; //정비일자

  @Column(name="vechregno")
  String vechregno; //등록 번호

  @Column(name="partamt")
  Integer partamt;  //부품합계

  @Column(name="workamt")
  Integer workamt;  //공임합계

  @Column(name="amount")
  Integer amount; //공급가액

  @Column(name="vamt")
  Integer vamt; //부과세

  @Column(name="totamt")
  Integer totamt; //합계

  @Column(name = "pernm")
  String pernm;

  @Column(name="mileage")
  Integer mileage;  //주행거리

}
