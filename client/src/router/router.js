import Vue from 'vue';
import VueRouter from 'vue-router'
import UserProfile from '../components/UserProfile.vue'
import VacationReqReview from '../components/VacationReqReview'
import Room from '../views/Rooms'
import Login from '../views/Login'
import Appointments from '../views/Appointments'
import ExaminationTypeReview from '../components/examinationType/ExaminationTypeReview.vue'
import Calendar from '../views/Calendar'
import Clinics from '../views/clinic/Clinics.vue'
import Doctors from '../views/Doctors.vue'
import Register from '../views/Register.vue'
import DoctorPage from '../views/DoctorPage'
import PatientReview from '../components/PatientReview.vue'
import VacationRequest from '../components/VacationRequest'
import ClinicAdminPage from '../views/ClinicAdminPage'
import AppointmentRequests from '../components/AppointmentRequests'
import ScheduleAppointment from '../components/ScheduleAppointment';
import DoctorReview from '../components/doctor/DoctorReview';
import ClinicProfile from '../components/ClinicProfile';
import ChangePassword from '../components/ChangePassword';
import NursePage from '../views/NursePage'
import Unauthorized from "../views/Unauthorized"
import NotFound from "../views/NotFound"


Vue.use(VueRouter);

// TODO: Ovo iskoristiti kada se napravi stranica za administratora klinickog centra
// const isClinicCenterAdmin = (to, from, next) => {
//     if (localStorage.getItem("user_role") === "ROLE_CLINIC_CENTER_ADMIN") {
//       next();
//       return;
//     }
//     next({
//       name: "Unauthorized"
//     })
// };

const isClinicAdmin = (to, from, next) => {
  if (localStorage.getItem("user_role") === "ROLE_CLINIC_ADMIN") {
    next();
    return;
  }
  next({
    name: "Unauthorized"
  })
};

const isDoctor = (to, from, next) => {
  if (localStorage.getItem("user_role") === "ROLE_DOCTOR") {
    if(localStorage.getItem("is_password_changed") == 'true'){
      next();
      return;
    }else{
      if(from.path === '/'){
        next({path: '/change-password'});
        return;
      }else{
        next();
      }
    }
  }
  else{
    next({
      name: "Unauthorized"
    })
  }
};

const isNurse = (to, from, next) => {
  if (localStorage.getItem("user_role") === "ROLE_NURSE") {
    next();
    return;
  }
  next({
    name: "Unauthorized"
  })
};

// TODO: Ovo iskoristiti kada se napravi stranica za administratora klinickog centra
// const isPatient = (to, from, next) => {
//     if (localStorage.getItem("user_role") === "ROLE_CLINIC_CENTER_ADMIN") {
//       next();
//       return;
//     }
//     next({
//       name: "Unauthorized"
//     })
// };

const isLogOut = (to, from, next) => {
    if (!localStorage.getItem("user_role")) {
      next();
      return;
    }
    next({
      name: "Unauthorized"
    })
};

const router = new VueRouter({
    mode : 'hash',
    routes: [
      {
        path: '/',
        name: Login,
        component: Login,
        beforeEnter: isLogOut
      },
      {
        path: '/register',
        name: "Register",
        component: Register,
        beforeEnter: isLogOut
      },
      
      {
        path: '/doctor',
        name: 'DoctorPage',
        component: DoctorPage,
        beforeEnter: isDoctor,
        children: [
            {path: 'patient', name: 'PatientReview', component: PatientReview},
            {path: 'profile', name: 'UserProfile', component: UserProfile},
            {path: ':16/calendar', name: 'Calendar', component : Calendar},
            {path: 'vacationRequest', name: 'VacationRequest', component: VacationRequest},
            {path: 'scheduleApp', name:'ScheduleAppointment', component: ScheduleAppointment}
        ]
      },
      {
        path: '/nurse',
        name: 'NursePage',
        component: NursePage,
        beforeEnter: isNurse,
        children: [
            {path: 'patient', name: 'PatientReview', component: PatientReview},
            {path: 'profile', name: 'UserProfile', component: UserProfile},
            {path: ':16/calendar', name: 'Calendar', component : Calendar},
            {path: 'vacationRequest', name: 'VacationRequest', component: VacationRequest}
        ]
      },
      {
        path: '/clinicAdmin',
        name: 'ClinicAdminPage',
        component: ClinicAdminPage,
        beforeEnter: isClinicAdmin,
        children: [
          {path: 'appointmentRequests', name: 'AppointmentRequests', component: AppointmentRequests},
          {path: 'vacationRequests', name: 'Vacation Requests', component: VacationReqReview },
          {path: 'rooms', name: 'Rooms', component: Room },
          {path: 'ex_type', name: 'ExaminationType', component : ExaminationTypeReview },
          {path: 'appointments', name: 'Appointments', component : Appointments },
          {path: 'doctors', name: 'Doctors', component : DoctorReview},
          {path: 'clinicProfile', name: 'ClinicProfile', component: ClinicProfile},
          {path: 'profile', name: 'UserProfile', component: UserProfile},

        ]
      },
      {
        path: '/clinics',
        name: 'Clinics',
        component : Clinics
      },
      {
        path: '/doctors',
        name: 'Doctors',
        component: Doctors
      },
      {
        path: '/change-password',
        name: 'ChangePassword',
        component: ChangePassword
      },

      {
        path: "/401",
        name: "Unauthorized",
        component: Unauthorized
      },

      {
        path: "/404",
        alias: "*",
        name: "NotFound",
        component: NotFound
      }
    ]
  })

export default router;