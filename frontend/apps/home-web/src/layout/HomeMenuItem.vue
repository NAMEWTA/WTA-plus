<template>
  <div v-if="!item.hidden">
    <HomeMenuItem v-if="singleChild" :item="singleChild" :base-path="homeMenuPath(basePath, singleChild.path)" />
    <el-sub-menu v-else-if="children.length" :index="basePath">
      <template #title>
        <SvgIcon :icon-class="String(item.meta?.icon || '')" />
        <span class="menu-title">{{ item.meta?.title }}</span>
      </template>
      <HomeMenuItem
        v-for="child in children"
        :key="child.path"
        :item="child"
        :base-path="homeMenuPath(basePath, child.path)"
      />
    </el-sub-menu>
    <el-menu-item v-else :index="basePath" class="submenu-title-noDropdown">
      <SvgIcon :icon-class="String(item.meta?.icon || '')" />
      <template #title>
        <span class="menu-title">{{ item.meta?.title || item.name }}</span>
      </template>
    </el-menu-item>
  </div>
</template>
<script setup lang="ts">
import type { RouteRecordRaw } from 'vue-router';
import SvgIcon from '@namewta/web-kit-ui-element/icon';
import { computed } from 'vue';
import { homeMenuPath, singleVisibleMenuChild } from './menuPresentation';
const props = defineProps<{ item: RouteRecordRaw; basePath: string }>();
const children = computed(() => (props.item.children ?? []).filter(item => !item.hidden));
const singleChild = computed(() => singleVisibleMenuChild(props.item));
</script>
