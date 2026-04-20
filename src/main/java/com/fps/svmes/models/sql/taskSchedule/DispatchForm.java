//package com.fps.svmes.models.sql.taskSchedule;
//
//import com.fasterxml.jackson.annotation.JsonBackReference;
//import com.fps.svmes.models.sql.Common;
//import jakarta.persistence.*;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//@Entity
//@Table(name = "dispatch_form", schema = "quality_management")
//@Data
//@NoArgsConstructor
//public class DispatchForm extends Common {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne
//    @JoinColumn(name = "dispatch_id", nullable = false)
//    @JsonBackReference
//    private Dispatch dispatch;
//
//    @Column(name = "qc_form_tree_node_id", nullable = false)
//    private String qcFormTreeNodeId;
//
//    public DispatchForm(Dispatch dispatch, String formTreeNodeId) {
//        this.setQcFormTreeNodeId(formTreeNodeId);
//        this.dispatch = dispatch;
//    }
//}
