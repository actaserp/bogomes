package mes.domain.entity.bogo;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name="tb_as012")
@NoArgsConstructor
@Data
public class tb_as012 {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name="id")
  Integer id; //순번

  @Column(name="repid")
  Integer repid;  //차량수리순번

  @Column(name="partgroup")
  String partgroup;  //부품구분

  @Column(name="partqty")
  Integer partqty;  //부품수량

  @Column(name="uamt")
  Integer uamt; //부품단가

  @Column(name="totamt")
  Integer totamt; //합계

  @Column(name="fixtext")
  String fixtext; //작업내용

  @Column(name="workpay")
  Integer workpay;  //공임

}
