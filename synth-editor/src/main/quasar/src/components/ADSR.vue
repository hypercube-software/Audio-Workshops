<template>
  <div :style="style">
    <canvas id="myChart" ref="chartRef"></canvas>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUpdate } from 'vue';
import { Chart, registerables, scales } from 'chart.js'
Chart.register(...registerables);

var chart = null;

const maxVolume = 100;
const sustainVolume = 70;

const chartRef = ref(null)

const props = defineProps({
  width: {
    type: String,
    default: "200px"
  },
  attack: {
    type: Number,
    default: 10
  },
  decay: {
    type: Number,
    default: 10
  },
  sustain: {
    type: Number,
    default: 127
  },
  release: {
    type: Number,
    default: 60
  }
})


const style = ref({
  width: props.width,
  height: null
});

const buildConfig = () => {
  const x1 = 0;
  const x2 = x1 + props.attack;
  const x3 = x2 + props.decay;
  const x4 = x3 + props.sustain;
  const x5 = x4 + props.release;
  return {
    datasets: [{
      showLine: true,
      fill: true,
      borderColor: '#FF6D00',
      backgroundColor: '#9E4100',
      data: [
        { x: x1, y: 0 },
        { x: x2, y: maxVolume },
        { x: x2, y: maxVolume },
        { x: x3, y: sustainVolume },
        { x: x4, y: sustainVolume },
        { x: x5, y: 0 }
      ],
    }]
  };
}


onBeforeUpdate(() => {
  chart.data = buildConfig();
  chart.update();
  style.value.width = props.width;
});

onMounted(() => {
  if (!chartRef.value) return
  chart = new Chart(chartRef.value, {
    type: 'scatter',
    data: buildConfig(),
    options: {
      animation: false,
      color: "#600",
      responsive: true,
      scales: {
        y: {
          max: maxVolume * 6 / 4,
          border: {
            display: true,
            color: "#666"
          },
          ticks: {
            display: false
          },
          grid: {
            display: false
          }
        },
        x: {
          max: 127 * 4,
          border: {
            display: true,
            color: "#666"
          },
          ticks: {
            display: false
          },
          grid: {
            display: false
          }
        }
      },
      plugins: {
        tooltip: {
          enabled: false,
        },
        legend: {
          display: false
        }
      }
    }
  });
});
</script>
