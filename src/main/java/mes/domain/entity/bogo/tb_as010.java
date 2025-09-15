package mes.domain.entity.bogo;

import io.micrometer.core.annotation.Counted;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.sql.Date;

@Entity
@Table(name="tb_as010")
@NoArgsConstructor
@Data
public class tb_as010 { //as 차량관리

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name="id")
  Integer id;

  @Column(name="spcmngno")
  String spcmngno;  //제원관리번호

  @Column(name="vechidno")
  String vechidno;  //차대번호

  @Column(name="inputdate")
  Date inputdate; //입고일자

  @Column(name="outdate")
  Date outdate; //출고일자

  @Column(name="itemcode")
  String itemcode;  //품목코드

  @Column(name="owner")
  String owner; //차주

  @Column(name="pernm")
  String pernm;   //차량수리담당

  @Column(name="endflag")
  String endflag;

  @Column(name = "fixtext")
  String fixtext;

  @Column(name="regno")
  String regno;

}
