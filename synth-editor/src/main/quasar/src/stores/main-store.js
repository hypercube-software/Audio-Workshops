import { defineStore } from "pinia";

const store = defineStore("mainStore", {
  state: () => ({
    appTitle: "Synth Editor",
    appVersion: "1.0.0",
    devices: {
      inputs: [],
      outputs: [],
    },
    selectedMidiInDevice: null,
    selectedMidiOutDevice: null,
  }),

  getters: {
    inputDevices(state) {
      return state.inputs;
    },
    outputDevices(state) {
      return state.outputs;
    },
  },

  actions: {
    setDevices(devices) {
      this.devices = devices;
    },
    setMidiInDevice(device) {
      this.selectedMidiInDevice = device;
    },
    setMidiOutDevice(device) {
      this.selectedMidiOutDevice = device;
    },
  },
});

export default store;
