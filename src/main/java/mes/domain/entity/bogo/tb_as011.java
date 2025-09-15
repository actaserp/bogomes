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

  @Column(name="partgroup")
  String partgroup; //부품구분

  @Column(name="partqty")
  Integer partqty;  //부품수량

  @Column(name="uamt")
  Integer uamt;

  @Column(name = "totamt")
  Integer totamt;

  @Column(name="workpay")
  Integer workpay;  //공임

  @Column(name="endflag")
  String endflag;

  @Column(name="pernm")
  String pernm;

  @Column(name="mileage")
  Integer mileage;  //주행거리

  @Column(name="fixtext")
  String fixtext;
}
