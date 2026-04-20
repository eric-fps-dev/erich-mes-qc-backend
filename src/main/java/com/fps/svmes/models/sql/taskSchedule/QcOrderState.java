//package com.fps.svmes.models.sql.taskSchedule;
//
//import lombok.Getter;
//
//@Getter
//public enum QcOrderState {
//    Active((short) 1),     // At least one associated dispatch is active
//    Inactive((short) 2),   // No active dispatches, but still exists
//    Expired((short) 3),    // All associated dispatches are expired
//    Exhausted((short) 4),  // All dispatches have reached their execution limits
//    Paused((short) 5),     // All dispatches are paused
//    Invalid((short) 6);    // One or more associated dispatches are invalid
//
//    private final Short state;
//
//    QcOrderState(Short state) {
//        this.state = state;
//    }
//}
//
