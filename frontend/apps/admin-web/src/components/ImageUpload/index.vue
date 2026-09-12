<script lang="ts">
import { ImageUpload as SharedImageUpload } from '@namewta/web-kit-file-upload';
import { compressAccurately } from 'image-conversion';
import { defineComponent, h } from 'vue';
import modal from '@/application/host/feedback';
import { ossUploadClient } from '@/application/services';

const feedback = {
  closeLoading: () => modal.closeLoading(),
  error: (message: string) => modal.msgError(message),
  loading: (message: string) => modal.loading(message)
};

export default defineComponent({
  name: 'ImageUpload',
  inheritAttrs: false,
  setup(_, { attrs, slots }) {
    return () =>
      h(
        SharedImageUpload,
        {
          ...attrs,
          client: ossUploadClient,
          compress: async (file: File, targetSizeKb: number) => {
            const blob = await compressAccurately(file, targetSizeKb);
            return new File([blob], file.name, { lastModified: file.lastModified, type: blob.type || file.type });
          },
          feedback
        },
        slots
      );
  }
});
</script>
