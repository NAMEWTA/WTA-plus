<template>
  <TopbarFrame :class="'nav' + navType">
    <template #leading>
      <div v-if="navType !== NavTypeEnum.TOP" class="hamburger-shell">
        <hamburger
          id="hamburger-container"
          :is-active="appStore.sidebar.opened"
          class="hamburger-container"
          @toggle-click="toggleSideBar"
        />
      </div>
      <router-link v-else-if="showLogo" to="/" class="navtop-logo-shell">
        <img :src="appLogo" class="navtop-logo-icon" alt="logo" />
      </router-link>
    </template>
    <template #context>
      <breadcrumb v-if="navType == NavTypeEnum.LEFT" id="breadcrumb-container" class="breadcrumb-container" />
      <top-nav v-if="navType == NavTypeEnum.MIX" id="topmenu-container" class="topmenu-container" />

      <template v-if="navType == NavTypeEnum.TOP">
        <top-bar id="topbar-container" class="topbar-container" />
      </template>
    </template>
    <template #actions>
      <template v-if="appStore.device !== 'mobile'">
        <search-menu ref="searchMenuRef" />
        <el-tooltip content="搜索" effect="dark" placement="bottom">
          <div class="right-menu-item hover-effect" @click="openSearchMenu">
            <svg-icon class-name="search-icon" icon-class="search" />
          </div>
        </el-tooltip>
        <!-- 消息 -->
        <el-tooltip :content="$t('navbar.message')" effect="dark" placement="bottom">
          <div>
            <el-popover
              placement="bottom"
              trigger="click"
              transition="el-zoom-in-top"
              :width="300"
              :persistent="false"
              @show="refreshMessageBox"
            >
              <template #reference>
                <el-badge :value="noticeStore.unreadCount.value > 0 ? noticeStore.unreadCount.value : ''" :max="99">
                  <div class="right-menu-item hover-effect message-trigger">
                    <svg-icon icon-class="message" />
                  </div>
                </el-badge>
              </template>
              <template #default>
                <notice></notice>
              </template>
            </el-popover>
          </div>
        </el-tooltip>
        <el-tooltip content="Github" effect="dark" placement="bottom">
          <WTAGit id="wta-git" class="right-menu-item hover-effect" />
        </el-tooltip>

        <el-tooltip :content="$t('navbar.document')" effect="dark" placement="bottom">
          <WTADoc id="wta-doc" class="right-menu-item hover-effect" />
        </el-tooltip>

        <el-tooltip :content="$t('navbar.full')" effect="dark" placement="bottom">
          <screenfull id="screenfull" class="right-menu-item hover-effect" />
        </el-tooltip>

        <el-tooltip :content="$t('navbar.language')" effect="dark" placement="bottom">
          <lang-select id="lang-select" class="right-menu-item hover-effect" />
        </el-tooltip>

        <el-tooltip :content="$t('navbar.layoutSize')" effect="dark" placement="bottom">
          <size-select id="size-select" class="right-menu-item hover-effect" />
        </el-tooltip>
      </template>
      <div class="avatar-container">
        <el-dropdown class="avatar-dropdown" trigger="click" @command="handleCommand">
          <div class="avatar-wrapper">
            <img :src="userStore.avatar" class="user-avatar" />
            <div class="avatar-meta">
              <span class="avatar-name">{{ displayName }}</span>
              <span class="avatar-role">Workspace</span>
            </div>
            <el-icon class="avatar-arrow"><caret-bottom /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <router-link to="/user/profile">
                <el-dropdown-item>{{ $t('navbar.personalCenter') }}</el-dropdown-item>
              </router-link>
              <el-dropdown-item v-if="settingsStore.showSettings" command="setLayout">
                <span>{{ $t('navbar.layoutSetting') }}</span>
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <span>退出当前应用</span>
              </el-dropdown-item>
              <el-dropdown-item v-if="userStore.globalLogoutAvailable" command="globalLogout">
                退出全部应用
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </template>
  </TopbarFrame>
</template>

<script setup lang="ts">
import type { ElMessageBoxOptions } from 'element-plus';
import { CaretBottom } from '@element-plus/icons-vue';
import TopbarFrame from '@namewta/web-kit-ui-element/topbar-frame';
import { createAppSocialRuntime } from '@/application/social';
import appLogo from '@/assets/logo/logo.svg';
import WTADoc from '@/components/WTADoc/index.vue';
import WTAGit from '@/components/WTAGit/index.vue';
import { NavTypeEnum } from '@/enums/NavTypeEnum';
import router from '@/router';
import { useAppStore } from '@/store/modules/app';
import { useNoticeStore } from '@/store/modules/notice';
import { useSettingsStore } from '@/store/modules/settings';
import { useUserStore } from '@/store/modules/user';
import { initMessageBox } from '@/utils/push';
import notice from './notice/index.vue';
import TopBar from './TopBar/index.vue';
import SearchMenu from './TopBar/search.vue';

const appStore = useAppStore();
const userStore = useUserStore();
const settingsStore = useSettingsStore();
const noticeStore = storeToRefs(useNoticeStore());

const navType = computed(() => settingsStore.navType);
const showLogo = computed(() => settingsStore.sidebarLogo);
const displayName = computed(() => userStore.nickname || '管理员');

const refreshMessageBox = () => {
  void initMessageBox(true).catch(error => console.warn('消息盒子刷新失败:', error));
};

// 搜索菜单
const searchMenuRef = ref<InstanceType<typeof SearchMenu>>();

const openSearchMenu = () => {
  searchMenuRef.value?.openSearch();
};

const toggleSideBar = () => {
  appStore.toggleSideBar(false);
};

const logout = async () => {
  await ElMessageBox.confirm('确定注销并退出系统吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  } as ElMessageBoxOptions);
  const redirect = encodeURIComponent(router.currentRoute.value.fullPath || '/');
  try {
    await userStore.logout();
  } catch {
    // The HTTP boundary reports remote failure; local logout still completes.
  } finally {
    await router.replace({ path: '/login', query: { redirect } });
  }
};

const emits = defineEmits(['setLayout']);
const setLayout = () => {
  emits('setLayout');
};
// 定义Command方法对象 通过key直接调用方法
const globalLogout = async () => {
  try {
    await ElMessageBox.confirm('将退出当前应用与统一登录会话，其他已接入应用会同步退出。', '退出全部应用', {
      confirmButtonText: '确认退出',
      cancelButtonText: '取消'
    });
  } catch {
    return;
  }
  try {
    await createAppSocialRuntime().globalLogout();
  } catch {
    ElMessage.error('统一退出未完成，请重试');
  }
};
const commandMap: Record<string, () => void | Promise<void>> = {
  globalLogout,
  setLayout,
  logout
};
const handleCommand = (command: string) => {
  // 判断是否存在该方法
  if (commandMap[command]) {
    void commandMap[command]();
  }
};
</script>
