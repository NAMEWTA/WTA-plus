<template>
  <div class="navbar ui-topbar">
    <div class="navbar-left">
      <slot name="leading" />
      <div class="nav-context"><slot name="context" /></div>
    </div>
    <div class="right-menu"><slot name="actions" /></div>
  </div>
</template>
