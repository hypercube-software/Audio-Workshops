<template>
  <q-layout view="hHh lpR fFf">
    <DialogMsg />
    <q-header elevated class="bg-primary text-white">
      <q-toolbar>
        <q-toolbar-title>
          <q-icon name="equalizer" size="2em" />
          {{ app.store.appTitle }} v{{ app.store.appVersion }}
        </q-toolbar-title>
      </q-toolbar>
    </q-header>

    <q-drawer v-model="leftDrawerOpen" side="left" overlay elevated>
      <!-- drawer content -->
      <div style="margin: 15px;">
        <q-select v-model="app.store.selectedMidiInDevice" :options="app.store.devices.inputs" option-label="name"
          label="MIDI In device" />
        <q-select v-model="app.store.selectedMidiOutDevice" :options="app.store.devices.outputs" option-label="name"
          label="MIDI Out device" />
        <div style="height: 20px;"></div>

        <q-list>
          <q-btn label="Refresh Params" @click="app.refreshParams()" />
          <q-item clickable v-ripple>
            <q-item-section>System Parameters</q-item-section>
            <q-item-section avatar>
              <q-icon name="settings" />
            </q-item-section>
          </q-item>

          <q-item clickable v-ripple>
            <q-item-section>Information</q-item-section>
            <q-item-section avatar>
              <q-icon name="info" />
            </q-item-section>
          </q-item>
        </q-list>
      </div>
    </q-drawer>

    <q-page-container>
      <router-view />
    </q-page-container>

  </q-layout>
</template>

<script setup>
import app from '../app/SynthEditorApp.js';
import DialogMsg from '../components/DialogMsg.vue';

const leftDrawerOpen = true;
</script>
