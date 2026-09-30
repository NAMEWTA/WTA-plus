export const documentTypes = [
  ['居民身份证', 'CN_RESIDENT_ID'],
  ['香港居民身份证', 'HK_RESIDENT_ID'],
  ['澳门居民身份证', 'MO_RESIDENT_ID'],
  ['台湾居民身份证', 'TW_RESIDENT_ID'],
  ['港澳居民居住证', 'HK_MACAO_RESIDENCE_PERMIT'],
  ['台湾居民居住证', 'TW_RESIDENCE_PERMIT'],
  ['港澳居民来往内地通行证', 'MAINLAND_TRAVEL_PERMIT_HK_MACAO'],
  ['台湾居民来往大陆通行证', 'MAINLAND_TRAVEL_PERMIT_TW'],
  ['中国护照', 'CN_PASSPORT'],
  ['香港护照', 'HK_PASSPORT'],
  ['澳门护照', 'MO_PASSPORT'],
  ['台湾旅行证件', 'TW_TRAVEL_DOCUMENT']
].map(([label, value]) => ({ label, value }));

export const genderOptions = [
  { label: '男', value: 'MALE' },
  { label: '女', value: 'FEMALE' },
  { label: '未知', value: 'UNKNOWN' }
];
