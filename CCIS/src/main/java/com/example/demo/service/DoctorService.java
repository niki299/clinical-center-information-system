package com.example.demo.service;

import com.example.demo.Repository.ClinicRepository;
import com.example.demo.Repository.DoctorRepository;
import com.example.demo.Repository.MedicalStaffRequestRepository;
import com.example.demo.Repository.PatientRepository;
import com.example.demo.Repository.RatingRepository;
import com.example.demo.Repository.*;
import com.example.demo.dto.AppointmentDTO;
import com.example.demo.dto.MedicalRecordDTO;
import com.example.demo.exceptions.ForbiddenException;
import com.example.demo.exceptions.NotFoundException;
import com.example.demo.model.AppointmentRequest;
import com.example.demo.model.Doctor;
import com.example.demo.model.Patient;
import com.example.demo.model.MedicalStaffRequest;
import com.example.demo.model.Rating;
import com.example.demo.dto.DoctorDTO;
import com.example.demo.model.*;
import com.example.demo.validation.DoctorValidation;
import org.omg.CosNaming.NamingContextPackage.NotFound;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class DoctorService {

    @Autowired
    private UserService userService;
    @Autowired
    private DoctorRepository doctorRepository;
    @Autowired
    private EmailService emailService;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private ClinicRepository clinicRepository;
    @Autowired
    private MedicalStaffRequestRepository medicalStaffRequestRepository;
    @Autowired
    private ClinicAdminRepository clinicAdminRepository;
    @Autowired
    private BusinessHoursRepository businessHoursRepository;
    @Autowired
    private ExaminationTypeRepository examinationTypeRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private RatingRepository ratingRepository;
    @Autowired
    private MedicalRecordRepository medicalRecordRepository;
    @Autowired
    private MedicalStaffRepository medicalStaffRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private CalendarRepository calendarRepository;
    @Autowired
    private AuthorityService authorityService;
    @Autowired
    private UserRepository userRepository;


    private DoctorValidation doctorValidation = new DoctorValidation();
    public Doctor findById(Integer id){
        return doctorRepository.findById(id).orElse(null);
    }

    public List<Doctor> findAllDoctors(){
        //treba izmena za aktivnost
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<ClinicAdmin> clinicAdmin =  clinicAdminRepository.findById(user.getId());
        return doctorRepository.findAllByClinicIdAndActivity(
                clinicAdmin.get().getClinic().getId(),true);
    }

    public Patient findPatientProfile(String email){
        Patient patient = patientRepository.findByEmail(email);
        return patient;
    }

    public boolean canStaffViewRecord(String patientEmail){
        //DOCTOR ACTIVITY
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<Appointment> appointments = appointmentRepository.findAllByDoctorIdAndPatientEmailAndFinished(
                user.getId(), patientEmail, true);
        if(appointments.size() == 0)
            return false;
        return true;
    }

    public List<Doctor> findDoctorsFromClinic(Integer clinicId) {
        //DOCTOR ACTIVITY
        Optional<Clinic> optionalClinic = clinicRepository.findById(clinicId);
        Clinic clinic = optionalClinic.get();
        List<Doctor> doctorsFromClinic = clinicRepository.findDoctorsFromClinic(clinic);
        doctorsFromClinic.removeIf(doctor -> !doctor.getActivity());
        return doctorsFromClinic;
    }


    public Doctor findByEmail(String email){
        //DOCTOR ACTIVITY
        return doctorRepository.findByEmailAndActivity(email, true);
    }

    public boolean gradeDoctor(Doctor doctor, Integer patientId, float newGrade) {
        Rating doctorRating = doctor.getRating();
        doctorRating.setGrade(patientId, newGrade);
        doctorRating = ratingRepository.save(doctorRating);
        return doctorRating != null;
    }
    public void deleteDoctor(Integer id) throws ForbiddenException {
        Optional<Doctor> find_doc = doctorRepository.findById(id);
        Date date = new Date();
        List<Appointment> appointments = appointmentRepository.findAllByDoctorIdAndTimeAfter(id, date);
        //ne pokupi doktora koji ima appointments null
        if(find_doc.isPresent()){
            Doctor doctor = find_doc.get();

            if(appointments.size() == 0){
                doctor.setActivity(false);
                doctorRepository.save(doctor);
                return;
            }

            throw new ForbiddenException("Doktor ima zakazane preglede.");
        }
        throw new ForbiddenException("Doktor ima zakazane preglede.");
    }

    public Doctor saveDoctor(DoctorDTO doctorDTO) throws NotFoundException, ForbiddenException {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        ClinicAdmin admin = clinicAdminRepository.findByEmailAndFetchClinicEagerly(user.getEmail());

        this.doesEmailAndNumberExists(doctorDTO);
        List<Authority> auth = authorityService.findByName("ROLE_DOCTOR");
        Doctor newDoctor = new Doctor();
        newDoctor.setAuthorities(auth);

        //businessHours u novu funkciju
        BusinessHours businessHours = new BusinessHours();
        LocalTime startTime = LocalTime.parse(doctorDTO.getBusinessHours().getStarted());
        businessHours.setStarted(Time.valueOf(startTime));
        LocalTime endTime = LocalTime.parse(doctorDTO.getBusinessHours().getEnded());
        businessHours.setEnded(Time.valueOf(endTime));
        if(doctorDTO.getExTypeId() == null)
            throw new NotFoundException("Examintaion type not found");

        Optional<ExaminationType> examinationType = examinationTypeRepository.findById(Integer.parseInt(doctorDTO.getExTypeId()));

        setDoctorFields(newDoctor, doctorDTO);

        businessHoursRepository.save(businessHours); //videcemo da pretrazimo postojece pa dodelimo
        newDoctor.setBusinessHours(businessHours);
        newDoctor.setClinic(admin.getClinic());
        if(examinationType.isPresent())
            newDoctor.setExaminationType(examinationType.get());
        else{
            throw new NotFoundException("Examintaion type not found");
        }
        return userRepository.save(newDoctor);
    }
    private void doesEmailAndNumberExists(DoctorDTO doctorDTO) throws NotFoundException {
        List<User> existUsers = userService.findByEmailOrSocialSecurityNumber(
                doctorDTO.getEmail(),
                doctorDTO.getSocialSecurityNumber());
        if (existUsers.size() > 2)
            throw new NotFoundException("Unknown error. This should not happen.");

        else if (existUsers.size() == 2)
            throw new NotFoundException("Both email and social security number are already taken.");

        else if (existUsers.size() == 1) {
            if (existUsers.get(0).getEmail().equals(doctorDTO.getEmail()))
                throw new NotFoundException("Choosen email is already taken.");

            if (existUsers.get(0).getSocialSecurityNumber().equals(doctorDTO.getSocialSecurityNumber()))
                throw new NotFoundException("Choosen social security number is already taken.");
        }
    }

    private void setDoctorFields(Doctor newDoctor, DoctorDTO doctorDTO) {
        newDoctor.setEmail(doctorDTO.getEmail());
        newDoctor.setPassword(passwordEncoder.encode(doctorDTO.getPassword()));
        newDoctor.setFirstName(doctorDTO.getFirstName());
        newDoctor.setLastName(doctorDTO.getLastName());
        newDoctor.setPhoneNumber(doctorDTO.getPhoneNumber());
        newDoctor.setSocialSecurityNumber(doctorDTO.getSocialSecurityNumber());
        newDoctor.setCity(doctorDTO.getCity());
        newDoctor.setAddress(doctorDTO.getAddress());
        newDoctor.setCountry(doctorDTO.getCountry());
        Rating rating = new Rating();
        rating.setAverageGrade(0.0f);
        newDoctor.setPasswordChanged(false);
        newDoctor.setRating(rating);
        Calendar calendar = calendarRepository.save(new Calendar());
        newDoctor.setCalendar(calendar);
        newDoctor.setActivity(true);
    }

    public boolean sendRequest(MedicalStaffRequest request){

        MedicalStaff user = (MedicalStaff) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        MedicalStaff get_doctor_clinic = medicalStaffRepository.findByEmailAndFetchClinicEagerly(user.getEmail());
//        get_doctor_clinic.getCalendar().getDates();

        request.setMedicalStaff_email(user.getEmail());
        request.setMedicalStaffName(user.getFirstName());
        request.setMedicalStaffLastName(user.getLastName());
        get_doctor_clinic.getClinic().getMedicalStaffRequests().add(request);
        clinicRepository.save(get_doctor_clinic.getClinic());

        emailService.alertAdminForVacation(user,request);

        return true;
    }

    public boolean schedule(AppointmentDTO appointmentDTO) throws ParseException {
        try {
            Doctor user = (Doctor) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Patient patient = patientRepository.findByEmail(appointmentDTO.getPatient());
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm");

            Date startDate = formatter.parse(appointmentDTO.getDate());
//          if(!doctorValidation.validateDoctorBusy(startDate,))

            if (patient == null)
                return false;

            AppointmentRequest request = new AppointmentRequest();
            user.setCounter(user.getCounter() + 1);
//            Thread.sleep(5000);           // for testing optimistic blocking
            doctorRepository.save(user);
            request.setDoctor(user);
            request.setPatient(patient);
            request.setTime(formatter.parse(appointmentDTO.getDate()));
            request.setType(AppointmentRequest.AppointmentReqType.DOCTOR);

            Doctor get_doctor_clinic = doctorRepository.findByEmailAndFetchClinicEagerly(user.getEmail());
            get_doctor_clinic.getClinic().getAppointmentRequests().add(request);

            clinicRepository.save(get_doctor_clinic.getClinic());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateMedicalRecord(MedicalRecordDTO recordToUpdate) {
        Optional<MedicalRecord> recordOptional = medicalRecordRepository.findById(recordToUpdate.getId());
        if(recordOptional.isPresent()){
            MedicalRecord record = recordOptional.get();
            record.setReports(recordToUpdate.getReports());
            record.setBloodType(recordToUpdate.getBloodType());
            record.setHeight(recordToUpdate.getHeight());
            record.setLeftEye(recordToUpdate.getLeftEye());
            record.setRightEye(recordToUpdate.getRightEye());
            record.setWeight(recordToUpdate.getWeight());
            medicalRecordRepository.save(record);
            return true;
        }
        return false;
    }

    public List<Doctor> gedDoctorsByExType(Integer ex_type_id) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<Integer> clinicId = clinicAdminRepository.findClinicIdByAdminId(user.getId());
        if(clinicId.isPresent()){
            return doctorRepository.findAllByClinicIdAndExaminationTypeIdAndActivityTrue(clinicId.get(), ex_type_id);
        }
        return null;
    }
}
