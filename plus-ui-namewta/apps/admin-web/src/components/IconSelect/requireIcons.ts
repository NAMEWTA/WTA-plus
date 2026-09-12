const modules = import.meta.glob('./../../assets/icons/svg/*.svg');
const icons = Object.freeze(
  Object.keys(modules)
    .map(path => path.split('assets/icons/svg/')[1].split('.svg')[0])
);

export default icons;
