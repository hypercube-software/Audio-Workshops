import synthEditorAPI from "../api/SynthEditorAPI.js";
import store from "../stores/main-store.js";
import { storeToRefs } from "pinia";
import { toRef } from "vue";
import { watch } from "vue";

class SynthEditorAPI {
  store = null;

  constructor(store) {}

  async onStart() {
    this.store = store();
    synthEditorAPI.setApp(this);
    synthEditorAPI.connectWebSocket();
    this.store.setDevices(await synthEditorAPI.getMIDIDevices());

    this.store.setParameters(await synthEditorAPI.getParameters());
    this.store.synth.params.forEach((p) =>
      watch(p, () => {
        synthEditorAPI.send(p);
      })
    );

    this.store.devices.outputs.forEach((device) => {
      console.log("OUTPUT DEVICE:" + device.name);
    });
    this.store.devices.inputs.forEach((device) => {
      console.log("INPUT DEVICE:" + device.name);
    });
    this.store.setMidiInDevice(this.store.devices.inputs[0]);
    this.store.setMidiOutDevice(this.store.devices.outputs[0]);
    const { selectedMidiInDevice, selectedMidiOutDevice } = storeToRefs(
      this.store
    );

    watch(selectedMidiInDevice, () => {
      synthEditorAPI.selectMidiInDevice(this.store.selectedMidiInDevice.name);
    });
    watch(selectedMidiOutDevice, () => {
      synthEditorAPI.selectMidiOutDevice(this.store.selectedMidiOutDevice.name);
    });
  }

  onSelectMidiInDevice(device) {
    console.log(device);
  }

  getParam(path) {
    if (this.store.synth.params) {
      const result = this.store.synth.params.filter((p) => p.path === path);
      if (result.length == 1) return result[0];
      return null;
    } else {
      return null;
    }
  }

  refreshParams() {
    console.log("Refresh params");
    this.store.setProgress(0);
    this.store.showDialog(true);
    synthEditorAPI.refreshParameters().then(async () => {
      console.log("Refresh params done");
      this.store.setParameters(await synthEditorAPI.getParameters());
      this.store.showDialog(false);
    });
  }

  onServerMsg(evt) {
    console.log("Message is received:" + evt);
    if (evt.type === "PROGRESS") {
      this.store.setProgress(Number(evt.msg));
    }
  }
}

export default new SynthEditorAPI(store);
