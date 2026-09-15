<template>
  <div class="app">
    <header class="app-header">
      <h1>TokenHunter Dashboard</h1>
    </header>
    <main class="app-main">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.app-header {
  padding: 16px 24px;
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
  position: sticky;
  top: 0;
  z-index: 10;
}
.app-header h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #111827;
}
.app-main {
  flex: 1;
  background: #f9fafb;
}
</style>
