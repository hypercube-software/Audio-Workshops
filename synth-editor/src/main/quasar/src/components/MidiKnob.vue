<template>
  <div class="column knob-container">
    <q-knob class="self-center" v-model="value" size="45px" :thickness="0.2" color="orange" center-color="orange-10" />
    <pre class="knob-caption">{{ props.caption }}</pre>
    <pre class="knob-value">{{ value }}</pre>
  </div>
</template>

<script setup>
import { ref } from 'vue';
const value = ref(12);

defineOptions({
  name: 'MidiKnob'
})

const props = defineProps({
  caption: {
    type: String,
    default: ''
  },
})
</script>

<style lang="scss" scoped>
.knob-caption {
  align-self: center;
  margin-top: 4px;
  margin-bottom: 0px;
}

.knob-value {
  align-self: center;
  margin-top: 0px;
  margin-bottom: 0px;
  font-size: x-small;
}

.knob-container {
  background: transparent;
}
</style>
