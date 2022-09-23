package com.efimchick.ifmo;

import com.efimchick.ifmo.util.CourseResult;
import com.efimchick.ifmo.util.Person;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public class Collecting {

    private boolean isPrograming;

    private int longestName;

    int sum(IntStream stream) {
        return stream.sum();
    }

    int production(IntStream stream) {
        return stream.reduce(Math::multiplyExact).orElse(0);
    }
    int oddSum(IntStream stream) {
        return stream.filter(e -> e % 2 != 0).sum();
    }

    Map<Integer, Integer> sumByRemainder(int divisor, IntStream stream) {
        return stream.boxed().collect(Collectors.groupingBy(e -> e % divisor,Collectors.summingInt(e -> e)));
       //return stream.boxed().collect(Collectors.toMap(e -> e % divisor, Function.identity(), Integer::sum));
    }

    Map<Person, Double> totalScores(Stream<CourseResult> stream) {
        List<CourseResult> courseResults = stream.collect(Collectors.toList());
        long count = courseResults.stream()
                                  .map(CourseResult::getTaskResults)
                                  .flatMap(e -> e.keySet().stream())
                                  .distinct()
                                  .count();
        return courseResults.stream().collect(Collectors.toMap(CourseResult::getPerson,
                courseResult -> courseResult.getTaskResults()
                                            .values()
                                            .stream()
                                            .mapToDouble(Double::valueOf)
                                            .sum() / count));
    }

    Double averageTotalScore(Stream<CourseResult> stream) {
        List<CourseResult> courseResults = stream.collect(Collectors.toList());
        long persons = courseResults.stream().map(CourseResult::getPerson).count();
        long disciples = courseResults.stream()
                                      .map(CourseResult::getTaskResults)
                                      .flatMap(e -> e.keySet().stream())
                                      .distinct()
                                      .count();
        return courseResults.stream()
                            .map(CourseResult::getTaskResults)
                            .flatMap(e -> e.values().stream())
                            .mapToDouble(Double::valueOf).sum() / (persons * disciples);
    }

    Map<String, Double> averageScoresPerTask(Stream<CourseResult> stream) {
        List<CourseResult> courseResults = stream.collect(Collectors.toList());
        double persons = courseResults.stream().map(CourseResult::getPerson).count();
        return courseResults.stream().flatMap(courseResult -> courseResult.getTaskResults().entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, entrySet -> entrySet.getValue() / persons, Double::sum));
    }

    Map<Person, String> defineMarks(Stream<CourseResult> stream) {
        List<CourseResult> list = stream.collect(Collectors.toList());
        long count = list.stream().map(CourseResult::getTaskResults)
                                  .flatMap(e -> e.keySet().stream())
                                  .distinct()
                                  .count();
        return list.stream().collect(Collectors.toMap(CourseResult::getPerson,
                                        courseResult -> mark(courseResult.getTaskResults()
                                                                         .values()
                                                                         .stream()
                                                                         .mapToDouble(Double::valueOf)
                                                                         .sum() / count)));
    }

    String easiestTask(Stream<CourseResult> stream) {
        return stream.flatMap(courseResult -> courseResult.getTaskResults().entrySet().stream())
                     .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Integer::sum))
                     .entrySet()
                     .stream()
                     .max(Comparator.comparingInt(Map.Entry::getValue))
                     .orElseThrow()
                     .getKey();
    }

    Collector<CourseResult, ArrayList<ArrayList<String>>, String> printableStringCollector() {

        longestName = 0;
        isPrograming = false;

        final String programmingString = "Lab 1. Figures | Lab 2. War and Peace | Lab 3. File Tree | Total | Mark |\n";
        final String historyString = "Phalanxing | Shieldwalling | Tercioing | Wedging | Total | Mark |\n";

        final int[] programmingStringModifiers = {14, 20, 16, 5, 4};
        final int[] historyStringModifiers = {10, 13, 9, 7, 5, 4};


        BiConsumer<ArrayList<ArrayList<String>>, CourseResult> accumulator = (arrayList, courseResult) -> {

            ArrayList<String> currentList = new ArrayList<>();

            Person person = courseResult.getPerson();
            String name = person.getLastName() + " " + person.getFirstName();
            longestName = Math.max(longestName, name.length());
            currentList.add(name);

            Map<String, Integer> map = courseResult.getTaskResults();
            LinkedHashMap<String, Integer> currentMap;
            isPrograming = map.containsKey("Lab 1. Figures");
            currentMap = returnMap(isPrograming);
            currentMap.putAll(map);

            int[] modifiers = isPrograming ? programmingStringModifiers : historyStringModifiers;
            ArrayList<Integer> list = new ArrayList<>(currentMap.values());
            IntStream.range(0, list.size()).forEach(i ->
                                                currentList.add(String.format("%"+modifiers[i]+"d | ", list.get(i))));

            double total = list.stream().mapToDouble(Double::valueOf).average().orElse(0);
            currentList.add(String.format("%2.2f | ", total));
            currentList.add(String.format("%4s |\n", mark(total)));
            arrayList.add(currentList);
        };

        Function<ArrayList<ArrayList<String>>, String> finisher = arrayLists -> {
            StringBuilder stringBuilder = new StringBuilder();
            String first = String.format("%-"+longestName+"s | ","Student") +
                    (isPrograming ? programmingString : historyString);
            stringBuilder.append(first);

            arrayLists.sort(Comparator.comparing(o -> o.get(0)));
            for (ArrayList<String> arrayList : arrayLists) {
                stringBuilder.append(String.format("%-"+longestName+"s | ", arrayList.get(0)));
                for (int i = 1; i < arrayList.size(); i++) {
                    stringBuilder.append(arrayList.get(i));
                }
            }

            stringBuilder.append(String.format("%-"+longestName+"s | ","Average"));
            int[] modifiers = isPrograming ? programmingStringModifiers : historyStringModifiers;

            double z = 0;
            int count = isPrograming ? 3 : 4;
            for (int i = 0; i < count; i++) {
                double x = 0;
                for (ArrayList<String> arrayList : arrayLists) {
                    x += Integer.parseInt(arrayList.get(i + 1).replaceAll("\\D", ""));
                }
                x /= 3;
                z += x;
                stringBuilder.append(String.format("%"+modifiers[i]+".2f | ", x));
            }
            double total = z / count;
            stringBuilder.append(String.format("%"+modifiers[++count]+".2f | ", total));
            stringBuilder.append(String.format("%4s |", mark(total)));

            return stringBuilder.toString();
        };
        return Collector.of(ArrayList::new, accumulator, (e1, e2) -> e2, finisher);
    }

    private String mark(double mark) {
        return mark >= 90 ? "A" : mark >= 83 ?
                            "B" : mark >= 75 ?
                            "C" : mark >= 68 ?
                            "D" : mark >= 60 ?
                            "E" : "F";
    }

    private LinkedHashMap<String, Integer> returnMap(boolean isPrograming) {
        return isPrograming ? new LinkedHashMap<>(){{
                                    put("Lab 1. Figures", 0);
                                    put("Lab 2. War and Peace", 0);
                                    put("Lab 3. File Tree", 0);
                              }}
                            : new LinkedHashMap<>(){{
                                    put("Phalanxing", 0);
                                    put("Shieldwalling", 0);
                                    put("Tercioing", 0);
                                    put("Wedging", 0);
                              }};
    }
}