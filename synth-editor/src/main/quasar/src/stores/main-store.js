import { defineStore } from "pinia";

export const mainStore = defineStore("mainStore", {
  state: () => ({
    appTitle: "Synth Edit",
    appVersion: "1.0.0",
    counter: 0,
  }),

  getters: {
    doubleCount(state) {
      return state.counter * 2;
    },
  },

  actions: {
    increment() {
      this.counter++;
    },
  },
});
