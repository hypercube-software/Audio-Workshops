<template>
  <router-view />
</template>

<script>
import synthEditorAPI from "./app/SynthEditorApp";

export default {
  mounted() {
    synthEditorAPI.onStart();
  }
}
</script>

<script setup>
defineOptions({
  name: 'App'
});
</script>
