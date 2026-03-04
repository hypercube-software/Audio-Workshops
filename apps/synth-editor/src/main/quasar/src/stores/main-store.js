import { defineStore } from "pinia";

const store = defineStore("mainStore", {
  state: () => ({
    dialogVisible: false,
    progress: 0,
    appTitle: "Synth Editor",
    appVersion: "1.0.0",
    devices: {
      inputs: [],
      outputs: [],
    },
    synth: {
      params: {
        level: {
          caption: "Level",
          value: 127,
        },
        attack: {
          caption: "Attack",
          value: 127,
        },
        decay: {
          caption: "Decay",
          value: 127,
        },
        release: {
          caption: "Release",
          value: 127,
        },
      },
    },
    selectedMidiInDevice: null,
    selectedMidiOutDevice: null,
    block: 1,
  }),

  getters: {
    inputDevices(state) {
      return state.inputs;
    },
    outputDevices(state) {
      return state.outputs;
    },
    progressLabel(state) {
      return Math.floor(state.progress * 100) + "%";
    },
  },

  actions: {
    setProgress(value) {
      this.progress = value / 100;
    },
    showDialog(visible) {
      this.dialogVisible = visible;
    },
    setDevices(devices) {
      this.devices = devices;
    },
    setParameters(parameters) {
      this.synth.params = parameters.map((p) => {
        const caption = p.path.substring(p.path.lastIndexOf("/") + 1);
        p.caption = caption;
        return p;
      });
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
