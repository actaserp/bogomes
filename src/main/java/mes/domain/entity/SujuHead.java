package mes.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.sql.Date;
import java.sql.Timestamp;

@Entity
@Table(name="suju_head")
@NoArgsConstructor
@Data
@EqualsAndHashCode( callSuper=false)
public class SujuHead extends AbstractAuditModel {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	Integer id;
	
	@Column(name="\"JumunDate\"")
	Date JumunDate;

	@Column(name="\"JumunNumber\"")
	String jumunNumber;

	@Column(name="\"TotalPrice\"")
	Double TotalPrice;

	@Column(name="\"ReceivedMoney\"")
	Double ReceivedMoney;

	@Column(name="\"ReceivableMoney\"")
	Double ReceivableMoney;

	@Column(name="\"State\"")
	String State;

	@Column(name="\"ShipmentState\"")
	String ShipmentState;

	@Column(name="\"DeliveryDate\"")
	Date DeliveryDate;

	@Column(name="\"Company_id\"")
	Integer Company_id;

	String spjangcd;

	@Column(name="\"SujuType\"")
	String SujuType;

	@Column(name="\"Description\"")
	String Description;

	@Column(name="contractnm")
	String contractnm;

	@Column(name="transcltnm")
	String transcltnm;	//운수회사

	@Column(name="hpnumber")
	String hpnumber;

	@Column(name="contaddres")
	String contaddres;	//주소

	@Column(name="matcolor")
	String matcolor; 		//색상

	@Column(name="modifytext")
	String modifytext;	//수정 사양
}
