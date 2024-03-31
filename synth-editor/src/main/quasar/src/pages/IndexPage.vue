<template>
  <q-page class="flex flex-center">
    <q-btn push color="primary" label="Call SpringBoot" @click="onClick" />
  </q-page>
</template>

<script>
import synthEditorAPI from "../api/synth-editor-api.js";

async function onClick(event) {
  const devices = await synthEditorAPI.getMIDIDevices();
  console.log(devices);
}
</script>

<script setup>

defineOptions({
  name: 'IndexPage'
});
</script>
