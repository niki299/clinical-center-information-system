package com.example.demo.service;

import com.example.demo.Repository.*;
import com.example.demo.dto.AppointmentDTO;
import com.example.demo.exceptions.NotFoundException;
import com.example.demo.model.*;
import com.example.demo.model.Calendar;
import com.example.demo.useful_beans.AppointmentToFinish;
import com.example.demo.useful_beans.AppointmentToReservePatient;
import com.example.demo.useful_beans.MedicineForPrescription;
import com.example.demo.validation.AppointmentValidation;
import com.example.demo.validation.DoctorValidation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private DoctorRepository doctorRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private ClinicAdminRepository clinicAdminRepository;
    @Autowired
    private ClinicRepository clinicRepository;
    @Autowired
    private ExaminationTypeRepository examinationTypeRepository;
    @Autowired
    private CalendarRepository calendarRepository;
    @Autowired
    private PatientService patientService;
    @Autowired
    private ClinicAdminService clinicAdminService;
    @Autowired
    private PerscriptionRepository perscriptionRepository;

    private DoctorValidation doctorValidation = new DoctorValidation();

    public List<Appointment> getAllAppointments(){
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<Integer> clinicId = clinicAdminRepository.findClinicIdByAdminId(user.getId());
        if(clinicId.isPresent())
            return appointmentRepository.findByClinicId(clinicId.get());

        return null;
    }

    public List<Appointment> findByPatientIdFinished(Integer patientId) {
        return appointmentRepository.findByPatientIdAndFinished(patientId, true);
    }

    public List<Appointment> getPredefinedAppointments(Integer clinicId) {
        Optional<Clinic> optionalClinic = clinicRepository.findById(clinicId);
        if (optionalClinic.isPresent())
            return appointmentRepository.findPredefinedAppointments(optionalClinic.get());
        else
            return new ArrayList<>();
    }

    public Appointment getAppointment(Integer id){
        Optional<Appointment> find_app = appointmentRepository.findById(id);
        return find_app.orElse(null);
    }

    public int savePatientToPredefinedAppointments(Patient patient, Appointment predefinedAppointment) {
        try {
            predefinedAppointment.setCounter(predefinedAppointment.getCounter() + 1);
//                Thread.sleep(5000);           // for testing optimistic blocking
            predefinedAppointment.setPatient(patient);

            predefinedAppointment = appointmentRepository.save(predefinedAppointment);
            if (predefinedAppointment != null)
                return 0;   // success
            else
                return 1;   // not saved to database
        } catch (Exception e) {
            e.printStackTrace();
            return -1;      // OptimisticLockException
        }
    }

    public Appointment saveAppointment(AppointmentDTO appointmentDTO) throws ParseException, NotFoundException {
        ClinicAdmin user = (ClinicAdmin) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date date = formatter.parse(appointmentDTO.getDate());
        //apointmentDTO sada ima doctorDTO i tu treba izmena
        Doctor getDoctor = doctorRepository.findByEmail(appointmentDTO.doctorEmail);

        Optional<Clinic> getClinic = clinicRepository.findById(user.getClinic().getId());
        Room getRoom = roomRepository.findByIdAndActivity(Integer.parseInt(appointmentDTO.getRoomId()),true);
        ExaminationType getType = examinationTypeRepository.findById(Integer.parseInt(appointmentDTO.getExaminationType())).get();

        if(!doctorValidation.validateDoctorBusy(date, getType.getDuration(),getDoctor))
            throw new NotFoundException("Doktor nije dostupan za trazeno vreme.");
        if(!validateRoom(date, getType.getDuration(), getRoom))
            throw new NotFoundException("Soba nije dostupna za trazeno vreme.");

        Appointment appointment_to_add;
        if(getClinic.isPresent()) {
            //Sta ako klinika nema cenovnik (vratimo bad request)
            Optional<PriceListItem> listItem = getClinic.get().getPriceList().getItems().stream()
                    .filter(item -> item.getExaminationType().getId().equals(getType.getId()))
                    .findAny();
            if(!listItem.isPresent())
                throw new NotFoundException("Price list item for this type doesnt exists. ");
            float price = listItem.get().getPrice();

            if(appointmentDTO.getDiscount() == null)
                appointmentDTO.setDiscount(0.0f);
            appointment_to_add = new Appointment(date, price, appointmentDTO.getDiscount(), getDoctor, getRoom, getType, getClinic.get());
            getRoom.addAppointment(appointment_to_add);
            getDoctor.addAppointment(appointment_to_add);
            appointmentRepository.save(appointment_to_add);
            return appointment_to_add;
        }
        return null;
    }

    public boolean valRoom(Date sd, Date ed, Room room){
        Long start = sd.getTime();
        Long end = ed.getTime();
        List<Date> startDates = room.getCalendar().getEventStartDates();
        List<Date> endDates = room.getCalendar().getEventEndDates();
        for(int i = 0; i<startDates.size(); i++){
            Long getTimeStart = startDates.get(i).getTime();
            Long getTimeEnd = endDates.get(i).getTime();
            if((getTimeStart <= start && getTimeEnd > start) ||
                    (getTimeStart <= end && getTimeEnd >= end) ||
                    (getTimeStart >= start && getTimeStart <= end) )
            {
                return false;
            }
        }return true;
    }

    public boolean validateRoom(Date startDate, float duration, Room room){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm");
        int d = (int) (3600*1000*duration);
        Date endDate = new Date(startDate.getTime()+d);
        if(room.getCalendar().getEventStartDates() == null){
            return true;
        }

        return valRoom(startDate,endDate, room);
    }

    public CodeBook getCodebookFromAppointmentClinic(Integer appointment_id) {
        //TODO make custom query
        Optional<Appointment> appointment = appointmentRepository.findByIdAndFetchClinicEagerly(appointment_id);
        if(appointment.isPresent())
            return appointment.get().getClinic().getCodeBook();
        return null;
    }

    public boolean handleAppointmentFinish(AppointmentToFinish appointmentToFinish) {
        Optional<Appointment> appointmentOptional = appointmentRepository.findByIdAndFetchClinicEagerly(appointmentToFinish.appointmentId);
        if(appointmentOptional.isPresent()){
            Appointment appointment = appointmentOptional.get();
            appointment.setReport(appointmentToFinish.report);
            Prescription prescription = new Prescription(appointmentToFinish.prescriptionToAdd);
            MedicalRecord mr = appointment.getPatient().getMedicalRecord();
            mr.addAppointment(appointment);
            mr.addPrescription(prescription);

            List<Calendar> calendars = calendarRepository.findAllByAppointmentIdsContaining(appointmentToFinish.appointmentId);
            for(Calendar calendar : calendars){
                calendar.removeEventByAppointmentId(appointmentToFinish.appointmentId);
            }

            appointment.getClinic().addPrescription(prescription);
            appointment.setFinished(true);
            prescription.setClinic(appointment.getClinic());


            perscriptionRepository.save(prescription);
            appointmentRepository.save(appointment);
            return true;
        }
        return false;

    }

    public String getPatinetEmail(Integer appointment_id) {
        return appointmentRepository.findPatientEmailFromAppointment(appointment_id);
    }

    public Appointment getAppointmentForPatient(String patientEmail) {
        Doctor doctor = (Doctor) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<Appointment> appointmentOptional =  appointmentRepository.findFirstByDoctorEmailAndPatientEmailAndFinishedFalseOrderByTimeAsc(doctor.getEmail(), patientEmail);
        if(appointmentOptional.isPresent()) return appointmentOptional.get();
        return null;
    }
}
