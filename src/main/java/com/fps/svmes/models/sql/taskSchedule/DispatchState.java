//package com.fps.svmes.models.sql.taskSchedule;
//
//import lombok.Getter;
//
//import java.util.Arrays;
//
//@Getter
//public enum DispatchState {
//    Active((short) 1),
//    Inactive((short) 2),
//    Expired((short) 3),
//    Exhausted((short) 4),
//    Paused((short) 5),
//    Invalid((short) 6);
//
//    private final short state;
//
//    DispatchState(short state) {
//        this.state = state;
//    }
//
//    public static DispatchState fromValue(int value) {
//        return Arrays.stream(DispatchState.values())
//                .filter(state -> state.getState() == value)
//                .findFirst()
//                .orElseThrow(() -> new IllegalArgumentException("Invalid state value: " + value));
//    }
//
//}
