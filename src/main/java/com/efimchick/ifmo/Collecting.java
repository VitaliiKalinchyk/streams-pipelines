package com.efimchick.ifmo;

import com.efimchick.ifmo.util.CourseResult;
import com.efimchick.ifmo.util.Person;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public class Collecting {

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

    Collector<CourseResult, ArrayList<CourseResult>, String> printableStringCollector() {

        Function<ArrayList<CourseResult>, String> finisher = courseResults -> {
            courseResults.sort(Comparator.comparing(o -> o.getPerson().getLastName()));
            int longestName = longestName(courseResults);
            TreeMap<String, Integer> disciples = disciples(courseResults);
            int[] modifiers = disciples.keySet().stream().mapToInt(String::length).toArray();

            StringBuilder sb = titleString(longestName, disciples.keySet());
            courseResults.forEach(student -> sb.append(personToString(longestName, modifiers, disciples, student)));
            sb.append(lastString(longestName, modifiers, courseResults));

            return sb.toString();
        };
        return Collector.of(ArrayList::new, ArrayList::add, (e1, e2) -> e2, finisher);
    }

    private String mark(double mark) {
        return mark >= 90 ? "A" : mark >= 83 ?
                            "B" : mark >= 75 ?
                            "C" : mark >= 68 ?
                            "D" : mark >= 60 ?
                            "E" : "F";
    }

    private int longestName(ArrayList<CourseResult> courseResults) {
        return courseResults.stream()
                            .map(CourseResult::getPerson)
                            .map(e -> e.getLastName() + e.getFirstName())
                            .mapToInt(String::length)
                            .max().orElse(0) + 1;
    }

    private TreeMap<String, Integer> disciples(ArrayList<CourseResult> courseResults) {
        return courseResults.stream()
                            .map(CourseResult::getTaskResults)
                            .map(Map::keySet).flatMap(Set::stream)
                            .distinct()
                            .collect(Collectors.toMap(Function.identity(), e -> 0, Integer::sum, TreeMap::new));
    }

    private StringBuilder titleString(int longestName, Set<String> disciples) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(String.format("%-"+longestName+"s | ","Student"));
        disciples.forEach(s -> stringBuilder.append(s).append(" | "));
        stringBuilder.append("Total | Mark |\n");
        return stringBuilder;
    }

    private StringBuilder lastString(int longestName, int[] modifiers, ArrayList<CourseResult> courseResults) {

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(String.format("%-"+longestName+"s | ","Average"));

        TreeMap<String, Double> averageMarksMap = new TreeMap<>(averageScoresPerTask(courseResults.stream()));
        ArrayList<Double> averageMarks = new ArrayList<>(averageMarksMap.values());
        IntStream.range(0, averageMarks.size()).forEach(i ->
                stringBuilder.append(String.format("%"+modifiers[i]+".2f | ", averageMarks.get(i))));

        double total = averageMarks.stream().mapToDouble(Double::valueOf).average().orElse(0);
        stringBuilder.append(String.format("%2.2f | ", total));
        stringBuilder.append(String.format("%4s |", mark(total)));

        return stringBuilder;
    }

    private StringBuilder personToString(int longestName, int[] modifiers,
                                  TreeMap<String, Integer> disciples, CourseResult courseResult) {

        StringBuilder stringBuilder = new StringBuilder();

        Person person = courseResult.getPerson();
        String name = person.getLastName() + " " + person.getFirstName();
        stringBuilder.append(String.format("%-"+longestName+"s | ", name));

        ArrayList<Integer> marks = new ArrayList<>(new TreeMap<String, Integer>(){{
            putAll(disciples);
            putAll(courseResult.getTaskResults());
        }}.values());
        IntStream.range(0, marks.size()).forEach(i ->
                stringBuilder.append(String.format("%"+modifiers[i]+"d | ", marks.get(i))));

        double total = marks.stream().mapToDouble(Double::valueOf).average().orElse(0);
        stringBuilder.append(String.format("%2.2f | ", total));
        stringBuilder.append(String.format("%4s |\n", mark(total)));

        return stringBuilder;
    }
}