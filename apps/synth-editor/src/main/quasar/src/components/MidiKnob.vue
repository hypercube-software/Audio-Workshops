<template>
  <div class="column items-center knob-container">
    <q-knob v-model="p.value" :min="0" :max="127" :angle="180" size="45px" :thickness="0.2" color="orange"
      center-color="orange-10" />
    <pre class="knob-caption">{{ p.caption }}</pre>
    <pre class="knob-value">{{ p.value }}</pre>
  </div>
</template>

<script setup>
import app from "../app/SynthEditorApp.js"
import { ref, onBeforeMount } from 'vue';

const p = ref({
  caption: "unset",
  value: 0
})

defineOptions({
  name: 'MidiKnob'
})

onBeforeMount(() => {
  // update the ref to point to the model
  p.value = props.param;
});

const props = defineProps({
  param: {
    type: Object,
    default: () => ({
      path: "",
      address: 0,
      size: 1,
      value: 0
    }),
  }
})
</script>

<style lang="scss" scoped>
.knob-caption {
  margin-top: 4px;
  margin-bottom: 0px;
}

.knob-value {
  margin-top: 0px;
  margin-bottom: 0px;
  font-size: x-small;
}

.knob-container {
  background: transparent;
}
</style>
