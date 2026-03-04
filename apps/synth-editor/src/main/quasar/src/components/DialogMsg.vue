<template>
  <q-dialog v-model="app.store.dialogVisible" persistent transition-show="scale" transition-hide="scale">
    <q-card class="bg-teal text-white" style="width: 300px">
      <q-card-section>
        <div class="text-h6">Updating...</div>
      </q-card-section>

      <q-card-section class="q-pt-none">
        <q-linear-progress size="25px" :value="app.store.progress" instant-feedback color="accent">
          <div class="absolute-full flex flex-center">
            <q-badge color="white" text-color="accent" :label="app.store.progressLabel" />
          </div>
        </q-linear-progress>
      </q-card-section>

      <q-card-actions align="right" class="bg-white text-teal">
        <q-btn flat label="OK" v-close-popup></q-btn>
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref } from 'vue';
import app from "../app/SynthEditorApp.js"
</script>
