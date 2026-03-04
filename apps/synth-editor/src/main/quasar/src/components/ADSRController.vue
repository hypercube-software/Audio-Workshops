<template>
  <div class="main column items-center content-center">
    <ADSR width="200px" :attack="adsr.attack.value" :decay="adsr.decay.value" :release="adsr.release.value"></ADSR>
    <div class="buttons row">
      <MIDIKnob :param="adsr.attack"></MIDIKnob>
      <MIDIKnob :param="adsr.decay"></MIDIKnob>
      <MIDIKnob :param="adsr.release"></MIDIKnob>
    </div>
  </div>
</template>

<script setup>
import ADSR from './ADSR.vue'
import MIDIKnob from './MidiKnob.vue'
import { ref, onBeforeMount } from "vue"
import app from "../app/SynthEditorApp.js"

const adsr = ref({
  attack: {
    caption: "Attack",
    value: 0,
  },
  decay: {
    caption: "Decay",
    value: 0,
  },
  release: {
    caption: "Release",
    value: 0,
  },
})

onBeforeMount(() => {
  // update the ref to point to the model
  adsr.value.attack = ref(app.getParam(`block[${app.store.block}]/PatchParams/ToneParams/Attack`));
  adsr.value.decay = ref(app.getParam(`block[${app.store.block}]/PatchParams/ToneParams/Decay`));
  adsr.value.release = ref(app.getParam(`block[${app.store.block}]/PatchParams/ToneParams/Release`));
});

</script>

<style scoped>
.main {
  gap: 2em;
  border-width: medium;
  border-color: grey;
  padding: 20px;
  border-style: solid;
  border-radius: 1em;
}

.buttons {
  gap: 1em;
}

.test {
  width: 40px;
  height: 40px;
  background: red;
}
</style>
