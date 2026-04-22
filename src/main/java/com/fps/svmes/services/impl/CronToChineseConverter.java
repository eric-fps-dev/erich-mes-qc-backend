//package com.fps.svmes.services.impl;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
//public class CronToChineseConverter {
//
//    public static String convertToChinese(String cronExpression) {
//        if (cronExpression == null || cronExpression.isEmpty()) {
//            return "无效的 Cron 表达式";
//        }
//
//        String[] parts = cronExpression.split(" ");
//        if (parts.length < 6) {
//            return "不支持的 Cron 格式";
//        }
//
//        String second = parseSecond(parts[0]);
//        String minute = parseMinute(parts[1]);
//        String hour = parseHour(parts[2]);
//        String day = parseDay(parts[3]);
//        String month = parseMonth(parts[4]);
//        String dayOfWeek = parseDayOfWeek(parts[5]);
//
//        return formatChineseCron(second, minute, hour, day, month, dayOfWeek);
//    }
//
//    private static String parseSecond(String field) {
//        return parseTimeField(field, "秒");
//    }
//
//    private static String parseMinute(String field) {
//        return parseTimeField(field, "分钟");
//    }
//
//    private static String parseHour(String field) {
//        return parseTimeField(field, "时");
//    }
//
//    private static String parseDay(String field) {
//        return field.equals("?") ? "" : parseTimeField(field, "日");
//    }
//
//    private static String parseMonth(String field) {
//        return field.equals("*") ? "" : field + "月";
//    }
//
//    private static String parseDayOfWeek(String field) {
//        if (field.equals("?") || field.equals("*")) return "";
//
//        List<String> days = Arrays.asList("日", "一", "二", "三", "四", "五", "六");
//        String[] parts = field.split(",");
//
//        List<String> parsedDays = new ArrayList<>();
//        for (String part : parts) {
//            if (part.contains("-")) {
//                // Handle range, e.g., "3-4" -> "三、四"
//                String[] range = part.split("-");
//                int start = Integer.parseInt(range[0]);
//                int end = Integer.parseInt(range[1]);
//
//                List<String> rangeDays = new ArrayList<>();
//                for (int i = start; i <= end; i++) {
//                    rangeDays.add(days.get(i % 7));
//                }
//                parsedDays.add(String.join("、", rangeDays));
//            } else {
//                // Single day
//                parsedDays.add(days.get(Integer.parseInt(part) % 7));
//            }
//        }
//        return "每周" + String.join("、", parsedDays);
//    }
//
//    private static String parseTimeField(String field, String unit) {
//        if (field.equals("*")) return "每" + unit;
//        if (field.startsWith("*/")) return "每 " + field.substring(2) + " " + unit + "执行一次";
//        if (field.contains(",")) {
//            // Handle multiple values, e.g., "10,18" -> "10 时 和 18 时"
//            return Arrays.stream(field.split(","))
//                    .map(value -> value + " " + unit)
//                    .collect(Collectors.joining(" 和 "));
//        }
//        if (field.contains("-")) {
//            // Handle ranges, e.g., "56-58" -> "56 到 58 秒"
//            return field.replace("-", " 到 ") + " " + unit;
//        }
//        return "第 " + field + " " + unit;
//    }
//
//    private static String formatChineseCron(String second, String minute, String hour, String day, String month, String dayOfWeek) {
//        StringBuilder result = new StringBuilder();
//
//        // Handle date first (month, day, day of the week)
//        if (!month.isEmpty()) result.append(month);
//        if (!day.isEmpty()) result.append(day);
//        if (!dayOfWeek.isEmpty()) result.append(dayOfWeek);
//
//        // Handle time (hour, minute, second)
//        if (!hour.isEmpty()) result.append("的").append(hour);
//        if (!minute.isEmpty()) result.append(":").append(minute);
//        if (!second.isEmpty()) result.append(":").append(second);
//
//        result.append(" 运行");
//
//        return result.toString();
//    }
//
//}
