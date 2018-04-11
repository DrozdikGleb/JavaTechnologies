package ru.ifmo.rain.drozdov.student;

import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentQuery;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by Gleb on 04.03.2018
 */
public class StudentDB implements StudentQuery {

    private static final Comparator<Student> NAME_COMPARATOR = Comparator.comparing(Student::getLastName)
            .thenComparing(Student::getFirstName)
            .thenComparing(Student::getId);

    private List<String> getList(Function<Student, String> function, List<Student> students) {
        return students.stream()
                .map(function)
                .collect(Collectors.toList());
    }

    private List<Student> getListAfterFind(Predicate<Student> predicate, Collection<Student> students) {
        return students.stream()
                .filter(predicate)
                .sorted(NAME_COMPARATOR)
                .collect(Collectors.toList());
    }

    private List<Student> sortStudents(Comparator<Student> comparator, Collection<Student> students) {
        return students.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return getList(Student::getFirstName, students);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return getList(Student::getLastName, students);
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return getList(Student::getGroup, students);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return getList(student -> student.getFirstName() + " " + student.getLastName(), students);
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return students.stream()
                .map(Student::getFirstName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return students.stream()
                .min(Student::compareTo)
                .map(Student::getFirstName)
                .orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortStudents(Student::compareTo, students);
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortStudents(NAME_COMPARATOR, students);
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return getListAfterFind(student -> student.getFirstName().equals(name), students);
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return getListAfterFind(student -> student.getLastName().equals(name), students);
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return getListAfterFind(student -> student.getGroup().equals(group), students);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return students.stream()
                .filter(student -> student.getGroup().equals(group))
                .collect(Collectors.toMap(Student::getLastName, Student::getFirstName,
                        BinaryOperator.minBy(String::compareTo)));
    }
}
