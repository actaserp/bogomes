package mes.domain.entity.commute;

import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;

@Setter
@Getter
@Entity
@NoArgsConstructor
@Table(name = "tb_pb201")
public class TB_PB201 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // PostgreSQL bigserial 대응
    @Column(name = "id")
    private Long id;

    @Column(name = "spjangcd", length = 2, nullable = false)
    private String spjangcd;

    @Column(name = "workym", length = 6, nullable = false)
    private String workym;

    @Column(name = "workday", length = 2, nullable = false)
    private String workday;

    @Column(name = "personid", nullable = false)
    private Integer personid;

    @Column(name = "worknum")
    private Integer worknum;

    @Column(name = "holiyn", length = 1)
    private String holiyn;

    @Column(name = "workyn", length = 1)
    private String workyn;

    @Column(name = "workcd", length = 2)
    private String workcd;

    @Column(name = "starttime", length = 5)
    private String starttime;

    @Column(name = "endtime", length = 5)
    private String endtime;

    @Column(name = "worktime", precision = 5, scale = 2)
    private BigDecimal worktime;

    @Column(name = "nomaltime", precision = 5, scale = 2)
    private BigDecimal nomaltime;

    @Column(name = "overtime", precision = 5, scale = 2)
    private BigDecimal overtime;

    @Column(name = "nighttime", precision = 5, scale = 2)
    private BigDecimal nighttime;

    @Column(name = "holitime", precision = 5, scale = 2)
    private BigDecimal holitime;

    @Column(name = "jitime")
    private int jitime;

    @Column(name = "jotime")
    private int jotime;

    @Column(name = "yuntime")
    private int yuntime;

    @Column(name = "abtime")
    private int abtime;

    @Column(name = "bantime")
    private int bantime;

    @Column(name = "adttime01", precision = 5, scale = 2)
    private BigDecimal adttime01;

    @Column(name = "adttime02", precision = 5, scale = 2)
    private BigDecimal adttime02;

    @Column(name = "adttime03", precision = 5, scale = 2)
    private BigDecimal adttime03;

    @Column(name = "adttime04", precision = 5, scale = 2)
    private BigDecimal adttime04;

    @Column(name = "adttime05", precision = 5, scale = 2)
    private BigDecimal adttime05;

    @Column(name = "adttime06", precision = 5, scale = 2)
    private BigDecimal adttime06;

    @Column(name = "adttime07", precision = 5, scale = 2)
    private BigDecimal adttime07;

    @Column(name = "remark", length = 50)
    private String remark;

    @Column(name = "fixflag", length = 1)
    private String fixflag;

    @Column(name = "inflag", length = 1)
    private String inflag;

    @Column(name = "address")
    private String address;

    @Column(name = "\"JobResNumber\"")
    String jobresNum;

    @Column(name = "\"JumunNumber\"")
    String jumunNum;

    @Column(name = "\"JobResId\"")
    Integer jobResId;

}
