class SynthEditorAPI {
  async getMIDIDevices() {
    const response = await fetch("api/devices");
    const devices = await response.json();
    return devices;
  }
}

export default new SynthEditorAPI();
