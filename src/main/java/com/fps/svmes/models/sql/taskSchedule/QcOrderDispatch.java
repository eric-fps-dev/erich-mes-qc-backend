//package com.fps.svmes.models.sql.taskSchedule;
//
//import com.fps.svmes.models.sql.Common;
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//import lombok.NoArgsConstructor;
//
//@EqualsAndHashCode(callSuper = true)
//@Entity
//@Table(name = "qc_order_dispatch", schema = "quality_management")
//@Data
//@NoArgsConstructor
//public class QcOrderDispatch extends Common {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name ="qc_order_id", nullable = false)
//    private QcOrder qcOrder;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name ="dispatch_id", nullable = false)
//    private Dispatch dispatch;
//
//
//    public QcOrderDispatch(QcOrder qcOrder, Dispatch dispatch) {
//        this.qcOrder = qcOrder;
//        this.dispatch = dispatch;
//    }
//}
