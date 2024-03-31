<template>
  <q-page class="flex flex-center">
    <q-btn push color="primary" label="Call SpringBoot" @click="onClick" />
    <q-btn push color="primary" label="Connect to WebSocket" @click="onConnect" />
  </q-page>
</template>

<script>
import synthEditorAPI from "../api/synth-editor-api.js";

async function onClick(event) {
  const devices = await synthEditorAPI.getMIDIDevices();
  console.log(devices);
}

async function onConnect() {
  synthEditorAPI.connectWebSocket();
}
</script>

<script setup>

defineOptions({
  name: 'IndexPage'
});
</script>
