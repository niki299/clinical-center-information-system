<template>
    <div>     
        <v-navigation-drawer width="260" :clipped="clipped" v-model="drawer" enable-resize-watcher app class="indigo lighten-5">
            <v-divider class="mb-5" />
            <v-list>
                <div/>
                <v-list-item v-for="item in items"
                        :to="item.link" 
                        :key="item.title"
                        link
                        >

                    <v-list-item-icon>
                        <v-icon>{{ item.icon }}</v-icon>
                    </v-list-item-icon>
                    <v-list-item-content>
                        <v-list-item-title class="font">{{ item.title }}</v-list-item-title>
                    </v-list-item-content>
                </v-list-item> 
            </v-list>
        </v-navigation-drawer>      
         
         <CustomToolbar/>
        <v-content style="padding: 6px 0px 0px 6px">
            <v-container fluid>
                <router-view></router-view>
            </v-container>
        </v-content>
    </div>
</template>

<script>
import CustomToolbar from "../components/CustomToolbar"

export default {
    components: {
        CustomToolbar
    },

    data(){
        return {
            items: [
                { title: 'Patient', link: '/nurse/patient', icon: 'mdi-clipboard-outline' },
                { title: 'Work calendar', link: '/nurse/calendar', icon: 'mdi-calendar' },
                { title: 'Create request for vacation', link: '/nurse/vacationRequest',icon:'mdi-airplane' },
                { title: 'Profile', link: '/nurse/profile', icon: 'mdi-account' },
                { title: 'Validate Perscription', link: '/nurse/validatePerscriptions', icon: 'mdi-ballot-outline' },

            ],
            drawer: true
        }   
    }
}
</script>

<style>
.font{
    font-size: 13px
}
</style>