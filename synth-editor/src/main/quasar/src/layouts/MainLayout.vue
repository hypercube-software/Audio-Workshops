<template>
  <q-layout view="hHh lpR fFf">

    <q-header elevated class="bg-primary text-white">
      <q-toolbar>
        <q-toolbar-title>
          <q-icon name="equalizer" size="2em" />
          {{ store.appTitle }} - {{ store.appVersion }}
        </q-toolbar-title>
      </q-toolbar>
    </q-header>

    <q-drawer v-model="leftDrawerOpen" side="left" overlay elevated>
      <!-- drawer content -->
      <div class="q-pa-md" style="max-width: 350px">
        <q-list bordered>
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

<script>
import { ref } from 'vue'
import { mainStore } from 'stores/main-store';
import synthEditorAPI from "../api/synth-editor-api.js";

export default {
  setup() {
    const store = mainStore()

    return {
      store,
      leftDrawerOpen: true
    }
  },
  mounted() {
    //synthEditorAPI.connectWebSocket();
  }
}
</script>
