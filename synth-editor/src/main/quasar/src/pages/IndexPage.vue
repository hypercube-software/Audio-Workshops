<template>
  <q-page class="flex flex-center">
    <ADSRController />
    <div class="container">
      <MIDIKnob :param="reverb"></MIDIKnob>
      <MIDIKnob :param="chorus"></MIDIKnob>
      <MIDIKnob :param="cutoff"></MIDIKnob>
      <MIDIKnob :param="reso"></MIDIKnob>
    </div>
  </q-page>
</template>

<script setup>
import ADSRController from 'components/ADSRController.vue'
import MIDIKnob from 'components/MidiKnob.vue'
import app from "../app/SynthEditorApp.js"
import { ref, onBeforeMount } from 'vue'

const reverb = ref(null)
const chorus = ref(null)
const cutoff = ref(null)
const reso = ref(null)

onBeforeMount(() => {
  // update the ref to point to the model
  reverb.value = app.getParam(`block[${app.store.block}]/PatchParams/Reverb Level`);
  chorus.value = app.getParam(`block[${app.store.block}]/PatchParams/Chorus Level`);
  cutoff.value = app.getParam(`block[${app.store.block}]/PatchParams/ToneParams/TVF Cutoff`);
  reso.value = app.getParam(`block[${app.store.block}]/PatchParams/ToneParams/TVF Resonance`);
});

defineOptions({
  name: 'IndexPage'
});
</script>

<style scoped>
.container {
  margin: 10px;
  border-width: medium;
  border-color: grey;
  padding: 20px;
  border-style: solid;
  border-radius: 1em;
}
</style>
