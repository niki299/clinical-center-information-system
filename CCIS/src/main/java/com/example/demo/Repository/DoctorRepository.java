package com.example.demo.Repository;

import com.example.demo.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface DoctorRepository extends JpaRepository<Doctor, Integer> {

    List<Doctor> findByExaminationTypeName(String name);
}
