package com.example.demo.dto;

import com.example.demo.model.MedicalRecord;
import com.example.demo.model.Prescription;
import org.modelmapper.ModelMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MedicalRecordDTO {
    private static ModelMapper modelMapper = new ModelMapper();

    private Integer id;
    private String weight;
    private String height;
    private String leftEye;
    private String rightEye;
    private String bloodType;
    private Map<String,String> reports;
    private Collection<AppointmentDTO> appointments;
    private Collection<PrescriptionDTO> prescriptions;

    public MedicalRecordDTO() {
        reports = new HashMap<String,String>();
        appointments = new ArrayList<>();
        prescriptions = new ArrayList<>();
    }

    public void setFields(MedicalRecord medicalRecord) {
        setPrescriptionsFromMedicalRecord(medicalRecord);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getLeftEye() {
        return leftEye;
    }

    public void setLeftEye(String leftEye) {
        this.leftEye = leftEye;
    }

    public String getRightEye() {
        return rightEye;
    }

    public void setRightEye(String rightEye) {
        this.rightEye = rightEye;
    }

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public Map<String, String> getReports() {
        return reports;
    }

    public void setReports(Map<String, String> reports) {
        this.reports = reports;
    }

    public Collection<AppointmentDTO> getAppointments() {
        return appointments;
    }

    public void setAppointments(Collection<AppointmentDTO> appointments) {
        this.appointments = appointments;
    }

    public Collection<PrescriptionDTO> getPrescriptions() {
        return prescriptions;
    }

    public void setPrescriptions(Collection<PrescriptionDTO> prescriptions) {
        this.prescriptions = prescriptions;
    }

    public void setPrescriptionsFromMedicalRecord(MedicalRecord medicalRecord) {
        Collection<Prescription> prescriptions = medicalRecord.getPrescriptions();
        for (Prescription prescription : prescriptions) {
            PrescriptionDTO prescriptionDTO = modelMapper.map(prescription, PrescriptionDTO.class);
            prescriptionDTO.setMedications(prescription.getContent().keySet());
            prescriptionDTO.setTimes(prescription.getContent().values());
            this.prescriptions.add(prescriptionDTO);
        }
    }
}
