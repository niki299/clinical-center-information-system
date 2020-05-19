import Vue from 'vue';

const state = {
    clinics: [],
};

const getters = {
    allClinics: (state) => state.clinics
};

const actions = {
    async fetchClinics({commit}){
        await Vue.$axios.get('http://localhost:8081/clinics')
        .then(response => {
            for (let i = 0; i < response.data.length; i++) {
                var clinic = response.data[i];
                clinic["filteredDoctors"] = clinic.doctors;
            }
            commit('setClinics', response.data);
        })
        .catch(() => { alert("Nemate pravo pregleda svih klinika") });
    },

    clinicsSetter({commit}, clinics) {
        commit('setClinics', clinics);
    }
};

const mutations = {
    setClinics: (state, clinics) => (state.clinics = clinics)
};

const namespaced = true;

export default {
    namespaced,
    state,
    getters,
    actions,
    mutations
}