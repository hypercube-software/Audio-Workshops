class SynthEditorAPI {
  // WebSocket handle
  #ws = null;
  #app = null;
  setApp(app) {
    this.app = app;
  }
  async getMIDIDevices() {
    const response = await fetch("api/devices");
    const devices = await response.json();
    return devices;
  }
  async getParameters() {
    const response = await fetch("api/parameters");
    const parameters = await response.json();
    return parameters;
  }
  async refreshParameters() {
    const response = await fetch("api/parameters/update");
  }

  async selectMidiInDevice(deviceName) {
    const response = await fetch(`api/input/${deviceName}`);
  }
  async selectMidiOutDevice(deviceName) {
    const response = await fetch(`api/output/${deviceName}`);
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
    if (this.app) {
      this.app.onServerMsg(JSON.parse(evt.data));
    }
  }

  onWebSocketclose() {
    console.log("Websocket connection is closed...");
    this.ws = null;
  }

  send(param) {
    const json = JSON.stringify({
      address: param.address,
      value: param.value,
    });
    console.log("Websocket send..." + json);
    this.ws.send(json);
  }
}

export default new SynthEditorAPI();
