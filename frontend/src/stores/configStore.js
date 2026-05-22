import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as api from '../services/api'

export const useConfigStore = defineStore('config', () => {
  const providers = ref([])
  const activeProviderId = ref(null)
  const chatMode = ref('DIRECT')

  async function loadProviders() {
    providers.value = await api.listProviders()
    const active = providers.value.find(p => p.active)
    if (active) activeProviderId.value = active.id
  }

  async function addProvider(data) {
    await api.addProvider(data)
    await loadProviders()
  }

  async function updateProvider(id, data) {
    await api.updateProvider(id, data)
    await loadProviders()
  }

  async function deleteProvider(id) {
    await api.deleteProvider(id)
    await loadProviders()
  }

  async function activateProvider(id) {
    await api.activateProvider(id)
    activeProviderId.value = id
    await loadProviders()
  }

  function setChatMode(mode) {
    chatMode.value = mode
  }

  return {
    providers, activeProviderId, chatMode,
    loadProviders, addProvider, updateProvider,
    deleteProvider, activateProvider, setChatMode
  }
})
