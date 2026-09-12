<script lang="ts">
import { FileUpload as SharedFileUpload } from '@namewta/web-kit-file-upload';
import { defineComponent, h } from 'vue';
import modal from '@/application/host/feedback';
import { ossUploadClient } from '@/application/services';

const feedback = {
  closeLoading: () => modal.closeLoading(),
  error: (message: string) => modal.msgError(message),
  loading: (message: string) => modal.loading(message)
};

export default defineComponent({
  name: 'FileUpload',
  inheritAttrs: false,
  setup(_, { attrs, slots }) {
    return () => h(SharedFileUpload, { ...attrs, client: ossUploadClient, feedback }, slots);
  }
});
</script>
