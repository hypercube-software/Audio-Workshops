class SynthEditorAPI {
  // WebSocket handle
  #ws = null;

  async getMIDIDevices() {
    const response = await fetch("api/devices");
    const devices = await response.json();
    return devices;
  }
  async selectMidiInDevice(deviceName) {
    const response = await fetch(`api/input/${deviceName}`);
  }
  async selectMidiOutDevice(deviceName) {
    const response = await fetch(`api/ouput/${deviceName}`);
  }

  async connectWebSocket() {
    const url = `ws://${window.location.host}/ws`;
    this.ws = new WebSocket(url);
    this.ws.onopen = this.onWebSocketOpen.bind(this);
    this.ws.onmessage = this.onWebSocketMessage.bind(this);
    this.ws.onclose = this.onWebSocketclose.bind(this);
  }

  onWebSocketOpen() {
    console.log("Websocket connected");
  }

  onWebSocketMessage(evt) {
    console.log("Message is received:" + evt.data);
  }

  onWebSocketclose() {
    console.log("Websocket connection is closed...");
    this.ws = null;
  }
}

export default new SynthEditorAPI();
