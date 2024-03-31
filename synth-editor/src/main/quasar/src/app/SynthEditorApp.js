import synthEditorAPI from "../api/SynthEditorAPI.js";
import store from "../stores/main-store.js";
import { storeToRefs } from "pinia";
import { watch } from "vue";

class SynthEditorAPI {
  store = null;

  constructor(store) {}

  async onStart() {
    this.store = store();
    synthEditorAPI.connectWebSocket();
    this.store.setDevices(await synthEditorAPI.getMIDIDevices());
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
}

export default new SynthEditorAPI(store);
