import Vue from 'vue';

const getDefaultState = () => {
    return {
        registrationRequests: []
    }
} ;

const state = getDefaultState();

const getters = {
    
    getregistrationRequests : (state) => () => {
        return state.registrationRequests;
    },

}

const actions = {
    async fetchRegRequests({commit}){
        const response = await Vue.$axios.get("http://localhost:8081/clinicCenterAdmin/getRegistrationRequests");
        commit('setregistrationRequests', response.data);
    },

    async handleDenyingRequest({commit}, requestToDeny){
        try{
            let message = requestToDeny.message;
            await Vue.$axios.post('http://localhost:8081/clinicCenterAdmin/deleteRequest/' + requestToDeny.requestId, message);
            commit('deletedRequest', requestToDeny.reqiestId);
            alert("Successfuly deleted request");
        }catch(error){
            alert(error.response);
        }
    },

    async handleAcceptingRequest({commit, dispatch}, id){
        try{
            await Vue.$axios.post("http://localhost:8081/clinicCenterAdmin/handleAcceptingRequest/" + id);
            commit('deletedRequest', id);
            dispatch('snackbar/showSuccess', "Successfuly regitered user", {root : true});
        }
        catch(error){
            // dispatch('snackbar/showError', error.response, {root : true});
            alert("Request doesn't exist");
        }
    },

    resetRegistrationRequests({commit}) {
        commit("resetState");
    }
}

const mutations = {
    setregistrationRequests: (state, requests) => {
        state.registrationRequests = requests;
    },

    deletedRequest(state,id) {
        const index = state.registrationRequests.findIndex(type => type.id === id);
        state.registrationRequests.splice(index,1);
    },

    resetState (state) {
        Object.assign(state, getDefaultState())
    }
}
const namespaced = true;

export default {
    namespaced,
    state,
    getters,
    actions,
    mutations
}